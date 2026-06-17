package org.cloud.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;

/**
 * 한국수출입은행 Open API(현재환율) 연동 서비스.
 *
 * searchdate 파라미터로 과거 특정 날짜의 환율을 조회할 수 있어, 매수일자 기준
 * 실제 환율을 가져올 수 있다. 다만 비영업일(주말/공휴일)이나 영업일 11시 이전에
 * 그날 데이터를 요청하면 결과가 비어 있을 수 있으므로, 그 경우 가장 가까운
 * 이전 영업일로 최대 5일까지 거슬러 올라가며 재시도한다. 그래도 실패하면
 * 안전한 기본값(1350원)으로 대체한다.
 */
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateService.class);

    private static final BigDecimal FALLBACK_RATE = new BigDecimal("1350.00");
    private static final long CACHE_TTL_MILLIS = 1000L * 60 * 60 * 12; // 12시간
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int MAX_LOOKBACK_DAYS = 5; // 주말+공휴일이 겹쳐도 안전하게 거슬러 올라갈 수 있는 한도

    private final RestTemplate restTemplate;

    @Value("${api.koreaexim.key}")
    private String koreaeximApiKey;

    private final Map<String, CachedRate> rateCache = new ConcurrentHashMap<>();

    private record CachedRate(BigDecimal value, long cachedAt) {
        boolean isExpired() {
            return Instant.now().toEpochMilli() - cachedAt > CACHE_TTL_MILLIS;
        }
    }

    /**
     * 특정 날짜(매수일자) 기준 USD -> KRW 환율을 조회한다.
     * 해당 날짜에 데이터가 없으면(비영업일 등) 이전 영업일로 거슬러 올라가며 재시도한다.
     */
    public BigDecimal fetchUsdToKrwRate(LocalDate date) {

        LocalDate target = (date != null) ? date : LocalDate.now();
        String cacheKey = target.format(DATE_FORMAT);

        CachedRate cached = rateCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.value();
        }

        for (int i = 0; i <= MAX_LOOKBACK_DAYS; i++) {
            LocalDate tryDate = target.minusDays(i);
            BigDecimal rate = callApi(tryDate);

            if (rate != null) {
                rateCache.put(cacheKey, new CachedRate(rate, Instant.now().toEpochMilli()));
                return rate;
            }
        }

        log.info("매수일자={} 환율 조회 실패(영업일/한도 문제로 추정), 기본값({}) 적용", target, FALLBACK_RATE);
        return FALLBACK_RATE;
    }

    /** 매수일자를 모르는 경우(오늘 투자 시작) 오늘 환율을 조회한다. */
    public BigDecimal fetchUsdToKrwRate() {
        return fetchUsdToKrwRate(LocalDate.now());
    }

    private BigDecimal callApi(LocalDate date) {
        String searchDate = date.format(DATE_FORMAT);

        String url = "https://oapi.koreaexim.go.kr/site/program/financial/exchangeJSON"
                + "?authkey=" + koreaeximApiKey
                + "&searchdate=" + searchDate
                + "&data=AP01";

        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> response = restTemplate.getForObject(url, List.class);

            if (response == null || response.isEmpty()) {
                // 비영업일이거나 11시 이전 요청 시 빈 배열이 반환된다.
                return null;
            }

            for (Map<String, Object> entry : response) {
                Object resultCode = entry.get("result");
                Object curUnit = entry.get("cur_unit");

                if (curUnit == null || !"USD".equals(curUnit.toString())) {
                    continue;
                }

                // result: 1=성공, 2=DATA코드 오류, 3=인증코드 오류, 4=일일제한횟수 마감
                if (resultCode == null || !"1".equals(resultCode.toString())) {
                    log.warn("한국수출입은행 환율 API 응답 오류 (result={}, date={})", resultCode, searchDate);
                    return null;
                }

                Object dealBasR = entry.get("deal_bas_r");
                if (dealBasR == null) {
                    return null;
                }

                // 매매기준율에 쉼표가 포함될 수 있어 제거 후 파싱한다 (예: "1,350.50")
                String cleaned = dealBasR.toString().replace(",", "");
                return new BigDecimal(cleaned).setScale(2, RoundingMode.HALF_UP);
            }

            return null;

        } catch (Exception e) {
            log.warn("한국수출입은행 환율 API 호출 실패 (date={})", searchDate, e);
            return null;
        }
    }
}