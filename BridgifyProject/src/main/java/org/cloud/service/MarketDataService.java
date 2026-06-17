package org.cloud.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MarketDataService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataService.class);

    // 시세 조회를 못했을 때 대신 사용하는 기본 연수익률(8%). 실패 시에도 시뮬레이션이 끊기지 않도록 하는 안전장치.
    private static final BigDecimal FALLBACK_ANNUAL_RETURN = new BigDecimal("0.08");

    // 캐시 유효 기간 (메모리 캐시 + 외부 API 호출 제한 대응)
    private static final long CACHE_TTL_MILLIS = 1000L * 60 * 60 * 12; // 12시간

    private final RestTemplate restTemplate;

    @Value("${api.alphavantage.key}")
    private String stockApiKey;

    private final Map<String, BigDecimal> stockCache = new ConcurrentHashMap<>();
    private final Map<String, CachedReturn> returnCache = new ConcurrentHashMap<>();

    private record CachedReturn(BigDecimal value, long cachedAt) {
        boolean isExpired() {
            return Instant.now().toEpochMilli() - cachedAt > CACHE_TTL_MILLIS;
        }
    }

    public BigDecimal fetchStockPrice(String ticker) {

        BigDecimal cached = stockCache.get(ticker);
        if (cached != null) {
            return cached;
        }

        String url = "https://www.alphavantage.co/query"
                + "?function=GLOBAL_QUOTE"
                + "&symbol=" + ticker
                + "&apikey=" + stockApiKey;

        try {
        	@SuppressWarnings("unchecked")
            Map<String, Object> response =
                    restTemplate.getForObject(url, Map.class);

            if (response == null || response.get("Global Quote") == null) {
                return BigDecimal.ZERO;
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> quote =        
                    (Map<String, Object>) response.get("Global Quote");

            Object priceObj = quote.get("05. price");

            if (priceObj == null) {
                return BigDecimal.ZERO;
            }

            BigDecimal price = new BigDecimal(priceObj.toString());

            if (price.compareTo(BigDecimal.ZERO) > 0) {
                stockCache.put(ticker, price);
            }

            return price;

        } catch (Exception e) {
            e.printStackTrace();
            return BigDecimal.ZERO;
        }
    }

    /**
     * 종목의 실제 과거 시세를 기반으로 연평균 복합 성장률(CAGR)을 계산한다.
     * Alpha Vantage의 TIME_SERIES_MONTHLY_ADJUSTED를 사용해 5년치 데이터를 한 번에 받아오므로,
     * 일별 시세를 매번 조회하는 것보다 API 호출 횟수를 크게 아낄 수 있다.
     *
     * 외부 API 실패, 호출 한도 초과, ticker 인식 불가 등 어떤 이유로든 값을 구하지 못하면
     * FALLBACK_ANNUAL_RETURN(8%)을 반환해 시뮬레이션 자체는 항상 동작하도록 한다.
     */
    public BigDecimal fetchAnnualizedReturn(String ticker) {

        if (ticker == null || ticker.isBlank()) {
            return FALLBACK_ANNUAL_RETURN;
        }

        CachedReturn cached = returnCache.get(ticker);
        if (cached != null && !cached.isExpired()) {
            return cached.value();
        }

        String url = "https://www.alphavantage.co/query"
                + "?function=TIME_SERIES_MONTHLY_ADJUSTED"
                + "&symbol=" + ticker
                + "&apikey=" + stockApiKey;

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null) {
                return useFallback(ticker, "응답 없음");
            }

            // API 한도 초과/잘못된 ticker 등은 정상 키가 아닌 메시지로 내려옴
            if (response.containsKey("Note") || response.containsKey("Information")
                    || response.containsKey("Error Message")) {
                log.warn("Alpha Vantage 응답에 시세 데이터 없음 (ticker={}): {}", ticker, response);
                return useFallback(ticker, "API 한도 초과 또는 잘못된 ticker");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> monthlySeries =
                    (Map<String, Object>) response.get("Monthly Adjusted Time Series");

            if (monthlySeries == null || monthlySeries.isEmpty()) {
                return useFallback(ticker, "월별 시세 데이터 없음");
            }

            // 날짜 문자열 키를 최신순으로 정렬 (TreeMap 역순)
            TreeMap<String, Object> sorted = new TreeMap<>(monthlySeries);

            String latestKey = sorted.lastKey();

            // 5년(60개월) 전 데이터, 데이터가 그보다 짧으면 가장 오래된 데이터로 대체
            LocalDate fiveYearsAgo = LocalDate.parse(latestKey, DateTimeFormatter.ISO_DATE).minusYears(5);
            String baseKey = sorted.keySet().stream()
                    .filter(d -> !LocalDate.parse(d, DateTimeFormatter.ISO_DATE).isAfter(fiveYearsAgo))
                    .max(String::compareTo)
                    .orElse(sorted.firstKey());

            BigDecimal latestPrice = extractAdjustedClose(sorted.get(latestKey));
            BigDecimal basePrice = extractAdjustedClose(sorted.get(baseKey));

            if (latestPrice == null || basePrice == null
                    || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
                return useFallback(ticker, "종가 파싱 실패");
            }

            double years = java.time.temporal.ChronoUnit.MONTHS.between(
                    LocalDate.parse(baseKey, DateTimeFormatter.ISO_DATE),
                    LocalDate.parse(latestKey, DateTimeFormatter.ISO_DATE)
            ) / 12.0;
            if (years < 1.0) {
                years = 1.0; // 데이터 기간이 1년 미만이면 1년으로 보정
            }

            // CAGR = (최신가 / 기준가)^(1/연수) - 1
            double ratio = latestPrice.divide(basePrice, 10, RoundingMode.HALF_UP).doubleValue();
            double cagr = Math.pow(ratio, 1.0 / years) - 1.0;

            BigDecimal result = BigDecimal.valueOf(cagr).setScale(6, RoundingMode.HALF_UP);

            returnCache.put(ticker, new CachedReturn(result, Instant.now().toEpochMilli()));
            return result;

        } catch (Exception e) {
            log.warn("Alpha Vantage 연수익률 조회 실패 (ticker={})", ticker, e);
            return useFallback(ticker, "예외 발생: " + e.getMessage());
        }
    }

    private BigDecimal useFallback(String ticker, String reason) {
        log.info("ticker={} 에 기본 수익률({}) 적용. 이유: {}", ticker, FALLBACK_ANNUAL_RETURN, reason);
        return FALLBACK_ANNUAL_RETURN;
    }

    @SuppressWarnings("unchecked")
    private BigDecimal extractAdjustedClose(Object monthEntry) {
        if (!(monthEntry instanceof Map)) {
            return null;
        }
        Map<String, Object> fields = (Map<String, Object>) monthEntry;
        Object adjustedClose = fields.get("5. adjusted close");
        if (adjustedClose == null) {
            return null;
        }
        try {
            return new BigDecimal(adjustedClose.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}