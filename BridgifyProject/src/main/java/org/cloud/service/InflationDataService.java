package org.cloud.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;

/**
 * 물가상승률 조회 서비스.
 *
 * 사용자가 직접 입력하던 물가상승률을 실제 소비자물가지수(CPI) API에서 자동 계산한다.
 *   - 미국 물가: FRED (series CPIAUCSL)
 *   - 한국 물가: 한국은행 ECOS (통계표 901Y009)
 *   - 계산식: (최신 CPI − 12개월 전 CPI) ÷ 12개월 전 CPI × 100
 *   - CPI는 자주 바뀌지 않으므로 12시간 캐싱한다.
 */
@Service
@RequiredArgsConstructor
public class InflationDataService {

    private static final Logger log = LoggerFactory.getLogger(InflationDataService.class);

    private final RestTemplate restTemplate;

    @Value("${api.fred.key}")
    private String fredApiKey;

    @Value("${api.ecos.key}")
    private String ecosApiKey;

    private static final String US_CPI_SERIES = "CPIAUCSL";  // 미국 소비자물가지수
    private static final String KR_CPI_STAT = "901Y009";      // 한국 소비자물가지수 통계표
    private static final BigDecimal DEFAULT_RATE = BigDecimal.valueOf(3.0); // 조회 실패 시 기본값

    private static final long TTL_MS = 12 * 60 * 60 * 1000L;

    // 간단 인메모리 캐시 (미국/한국 각각)
    private BigDecimal cachedUsRate;
    private long cachedUsAt = 0;
    private BigDecimal cachedKrRate;
    private long cachedKrAt = 0;

    /* ======================= 미국 (FRED) ======================= */

    public BigDecimal getUsInflationRate() {
        long now = System.currentTimeMillis();
        if (cachedUsRate != null && (now - cachedUsAt) < TTL_MS) {
            return cachedUsRate;
        }
        try {
            String url = "https://api.stlouisfed.org/fred/series/observations"
                    + "?series_id=" + US_CPI_SERIES
                    + "&api_key=" + fredApiKey
                    + "&file_type=json&sort_order=desc&limit=15";

            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            if (resp == null) return usFallback();

            @SuppressWarnings("unchecked")
            List<Map<String, String>> obs = (List<Map<String, String>>) resp.get("observations");
            if (obs == null || obs.isEmpty()) return usFallback();

            // (연월, 값) 최신순 — FRED는 date "2026-05-01", value "333.979" (없는 달은 ".")
            List<String[]> valid = new ArrayList<>();
            for (Map<String, String> o : obs) {
                String v = o.get("value");
                String d = o.get("date");
                if (v != null && !".".equals(v) && d != null) {
                    valid.add(new String[]{d.substring(0, 7).replace("-", ""), v}); // "202605"
                }
            }
            BigDecimal rate = calcRate(valid);
            if (rate == null) return usFallback();

            cachedUsRate = rate;
            cachedUsAt = now;
            log.info("미국 물가상승률(FRED) 조회: {}%", rate);
            return rate;
        } catch (Exception e) {
            log.warn("FRED 물가 조회 실패: {}", e.getMessage());
            return usFallback();
        }
    }

    /* ======================= 한국 (ECOS) ======================= */

    public BigDecimal getKrInflationRate() {
        long now = System.currentTimeMillis();
        if (cachedKrRate != null && (now - cachedKrAt) < TTL_MS) {
            return cachedKrRate;
        }
        try {
            // 최근 13개월 범위 조회 (yyyyMM)
            String end = yyyymmNow();
            String start = yyyymmMinusMonths(end, 13);

            String url = "https://ecos.bok.or.kr/api/StatisticSearch/"
                    + ecosApiKey + "/json/kr/1/13/"
                    + KR_CPI_STAT + "/M/" + start + "/" + end + "/0";

            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            if (resp == null) return krFallback();

            @SuppressWarnings("unchecked")
            Map<String, Object> search = (Map<String, Object>) resp.get("StatisticSearch");
            if (search == null) return krFallback(); // 코드/키 오류 시 다른 구조로 옴

            @SuppressWarnings("unchecked")
            List<Map<String, String>> rows = (List<Map<String, String>>) search.get("row");
            if (rows == null || rows.isEmpty()) return krFallback();

            // ECOS는 오름차순으로 옴 → 최신순으로 뒤집기. TIME "202605", DATA_VALUE "119.92"
            List<String[]> valid = new ArrayList<>();
            for (int i = rows.size() - 1; i >= 0; i--) {
                Map<String, String> r = rows.get(i);
                String time = r.get("TIME");
                String value = r.get("DATA_VALUE");
                if (time != null && value != null && !value.isBlank()) {
                    valid.add(new String[]{time, value});
                }
            }
            BigDecimal rate = calcRate(valid);
            if (rate == null) return krFallback();

            cachedKrRate = rate;
            cachedKrAt = now;
            log.info("한국 물가상승률(ECOS) 조회: {}%", rate);
            return rate;
        } catch (Exception e) {
            log.warn("ECOS 물가 조회 실패: {}", e.getMessage());
            return krFallback();
        }
    }

    /* ======================= 공통 계산 ======================= */

    // valid: (yyyyMM, value) 최신순 리스트 → 물가상승률(%) 계산
    private BigDecimal calcRate(List<String[]> valid) {
        if (valid == null || valid.size() < 2) return null;

        String latestYm = valid.get(0)[0];
        BigDecimal latest = new BigDecimal(valid.get(0)[1]);

        String targetYm = minusOneYear(latestYm); // 12개월 전 같은 달
        BigDecimal yearAgo = null;
        for (String[] p : valid) {
            if (p[0].equals(targetYm)) {
                yearAgo = new BigDecimal(p[1]);
                break;
            }
        }
        if (yearAgo == null) {
            yearAgo = new BigDecimal(valid.get(valid.size() - 1)[1]); // 없으면 가장 오래된 값
        }
        if (yearAgo.compareTo(BigDecimal.ZERO) <= 0) return null;

        return latest.subtract(yearAgo)
                .divide(yearAgo, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    // "202605" → "202505"
    private String minusOneYear(String yyyymm) {
        int year = Integer.parseInt(yyyymm.substring(0, 4));
        String month = yyyymm.substring(4, 6);
        return (year - 1) + month;
    }

    private String yyyymmNow() {
        java.time.LocalDate now = java.time.LocalDate.now().minusMonths(1); // 최신 확정치는 보통 전월
        return String.format("%d%02d", now.getYear(), now.getMonthValue());
    }

    private String yyyymmMinusMonths(String yyyymm, int months) {
        int year = Integer.parseInt(yyyymm.substring(0, 4));
        int month = Integer.parseInt(yyyymm.substring(4, 6));
        java.time.LocalDate d = java.time.LocalDate.of(year, month, 1).minusMonths(months);
        return String.format("%d%02d", d.getYear(), d.getMonthValue());
    }

    private BigDecimal usFallback() {
        return cachedUsRate != null ? cachedUsRate : DEFAULT_RATE;
    }

    private BigDecimal krFallback() {
        return cachedKrRate != null ? cachedKrRate : DEFAULT_RATE;
    }
}