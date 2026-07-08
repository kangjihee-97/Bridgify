package org.cloud.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.cloud.mapper.DividendHistoryMapper;
import org.cloud.mapper.PriceHistoryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;

/**
 * 배당·과거주가 데이터 캐싱 서비스.
 *
 * 동작 방식 (온디맨드 조회 + DB 캐싱):
 *   1) DB(dividend_history)에 해당 종목 데이터가 있으면 → 아무것도 안 함 (캐시 히트, API 0회)
 *   2) 없으면 → Alpha Vantage TIME_SERIES_MONTHLY_ADJUSTED 1회 호출 (주가+배당 한 번에)
 *   3) 월별 응답을 연 단위로 합산(배당) / 연말 종가(주가)로 정리해 DB에 저장
 *   4) 이후 같은 종목 요청은 전부 DB에서 처리 → 무료 API 제한(하루 25회)을 회피
 *
 * 배당·과거주가는 한 번 확정되면 바뀌지 않는 데이터라 영구 캐싱에 적합하다.
 */
@Service
@RequiredArgsConstructor
public class DividendDataService {

    private static final Logger log = LoggerFactory.getLogger(DividendDataService.class);

    private final RestTemplate restTemplate;
    private final DividendHistoryMapper dividendHistoryMapper;
    private final PriceHistoryMapper priceHistoryMapper;

    @Value("${api.alphavantage.key}")
    private String alphaVantageApiKey;

    // 이번 실행 중 이미 확인한 종목 (같은 요청 안에서 중복 체크 방지)
    private final Set<String> checkedTickers = ConcurrentHashMap.newKeySet();

    /**
     * 해당 종목의 배당·주가 데이터가 DB에 준비되도록 보장한다.
     * 실현손익 계산 전에 호출하면, 없는 종목은 자동으로 API에서 받아와 채워진다.
     */
    public void ensureDividendData(String ticker) {

        // 0) 이번 실행에서 이미 확인한 종목이면 스킵
        if (checkedTickers.contains(ticker)) {
            return;
        }

        // 1) DB에 이미 있으면 캐시 히트 — API 호출 없음
        if (!dividendHistoryMapper.findByTicker(ticker).isEmpty()) {
            checkedTickers.add(ticker);
            log.info("배당 캐시 히트 (DB): {}", ticker);
            return;
        }

        // 2) 없으면 Alpha Vantage 호출 (월별 주가+배당 한 번에)
        log.info("배당 캐시 미스 → Alpha Vantage 조회: {}", ticker);
        String url = "https://www.alphavantage.co/query"
                + "?function=TIME_SERIES_MONTHLY_ADJUSTED"
                + "&symbol=" + ticker
                + "&apikey=" + alphaVantageApiKey;

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null) {
                log.warn("Alpha Vantage 응답 없음: {}", ticker);
                return;
            }

            // 호출 한도 초과 시 "Note"/"Information" 키로 응답이 옴
            if (response.containsKey("Note") || response.containsKey("Information")) {
                log.warn("Alpha Vantage 호출 제한 도달 (하루 25회): {}", response);
                return; // checkedTickers에 안 넣음 → 내일 다시 시도 가능
            }

            @SuppressWarnings("unchecked")
            Map<String, Map<String, String>> series =
                    (Map<String, Map<String, String>>) response.get("Monthly Adjusted Time Series");

            if (series == null || series.isEmpty()) {
                log.warn("월별 시계열 데이터 없음 (티커 오타?): {}", ticker);
                return;
            }

            // 3) 월별 → 연 단위로 정리
            //    - 배당: 그 해 배당들을 전부 합산 → 연간 DPS
            //    - 주가: 그 해에서 가장 늦은 달의 종가 → 연 대표 주가
            Map<Integer, BigDecimal> yearlyDividend = new HashMap<>();
            Map<Integer, String> latestDateOfYear = new HashMap<>();
            Map<Integer, BigDecimal> yearlyPrice = new HashMap<>();

            for (Map.Entry<String, Map<String, String>> entry : series.entrySet()) {
                String date = entry.getKey();                    // "2026-05-29"
                int year = Integer.parseInt(date.substring(0, 4));
                Map<String, String> values = entry.getValue();

                // 배당 합산
                BigDecimal dividend = new BigDecimal(values.getOrDefault("7. dividend amount", "0"));
                yearlyDividend.merge(year, dividend, BigDecimal::add);

                // 그 해의 가장 늦은 날짜의 종가를 대표 주가로
                String prevLatest = latestDateOfYear.get(year);
                if (prevLatest == null || date.compareTo(prevLatest) > 0) {
                    latestDateOfYear.put(year, date);
                    yearlyPrice.put(year, new BigDecimal(values.getOrDefault("4. close", "0")));
                }
            }

            // 4) DB 저장 (배당이 0인 해는 저장 생략 — 무배당 종목/연도)
            int savedDiv = 0;
            int savedPrice = 0;
            for (Integer year : yearlyPrice.keySet()) {
                BigDecimal dps = yearlyDividend.getOrDefault(year, BigDecimal.ZERO);
                if (dps.compareTo(BigDecimal.ZERO) > 0) {
                    dividendHistoryMapper.upsert(ticker, year, dps);
                    savedDiv++;
                }
                BigDecimal price = yearlyPrice.get(year);
                if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                    priceHistoryMapper.upsert(ticker, year, price);
                    savedPrice++;
                }
            }

            checkedTickers.add(ticker);
            log.info("Alpha Vantage 캐싱 완료: {} (배당 {}건, 주가 {}건 저장)", ticker, savedDiv, savedPrice);

        } catch (Exception e) {
            log.warn("Alpha Vantage 조회 실패: {} - {}", ticker, e.getMessage());
            // 실패해도 예외를 던지지 않음 → 배당 없이(0으로) 실현손익 계산은 계속 진행
        }
    }
}