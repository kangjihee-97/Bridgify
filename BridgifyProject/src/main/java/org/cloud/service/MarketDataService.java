package org.cloud.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
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

    // 시세 조회를 못했을 때 대신 사용하는 기본 연수익률(8%)
    private static final BigDecimal FALLBACK_ANNUAL_RETURN = new BigDecimal("0.08");

    // 캐시 유효 기간 12시간
    private static final long CACHE_TTL_MILLIS = 1000L * 60 * 60 * 12;

    private final RestTemplate restTemplate;

    @Value("${api.finnhub.key}")
    private String finnhubApiKey;

    private final Map<String, BigDecimal> stockCache = new ConcurrentHashMap<>();
    private final Map<String, CachedReturn> returnCache = new ConcurrentHashMap<>();

    private record CachedReturn(BigDecimal value, long cachedAt) {
        boolean isExpired() {
            return Instant.now().toEpochMilli() - cachedAt > CACHE_TTL_MILLIS;
        }
    }

    /**
     * Finnhub API로 현재 주가를 조회한다.
     * /quote 엔드포인트의 "c" 필드(current price)를 사용한다.
     */
    public BigDecimal fetchStockPrice(String ticker) {

        BigDecimal cached = stockCache.get(ticker);
        if (cached != null) {
            return cached;
        }

        String url = "https://finnhub.io/api/v1/quote"
                + "?symbol=" + ticker
                + "&token=" + finnhubApiKey;

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null || response.get("c") == null) {
                log.warn("Finnhub 현재가 조회 실패 (ticker={})", ticker);
                return BigDecimal.ZERO;
            }

            BigDecimal price = new BigDecimal(response.get("c").toString());

            if (price.compareTo(BigDecimal.ZERO) > 0) {
                stockCache.put(ticker, price);
            }

            return price;

        } catch (Exception e) {
            log.warn("Finnhub 현재가 조회 예외 (ticker={})", ticker, e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Finnhub /stock/metric API로 52주 수익률을 가져와 연환산 수익률로 사용한다.
     * 무료 티어에서 사용 가능한 엔드포인트이다.
     */
    public BigDecimal fetchAnnualizedReturn(String ticker) {

        if (ticker == null || ticker.isBlank()) {
            return FALLBACK_ANNUAL_RETURN;
        }

        CachedReturn cached = returnCache.get(ticker);
        if (cached != null && !cached.isExpired()) {
            return cached.value();
        }

        String url = "https://finnhub.io/api/v1/stock/metric"
                + "?symbol=" + ticker
                + "&metric=all"
                + "&token=" + finnhubApiKey;

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null || response.get("metric") == null) {
                return useFallback(ticker, "metric 데이터 없음");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> metric = (Map<String, Object>) response.get("metric");

            // 52주 수익률 (% 단위로 내려오므로 /100 해서 소수로 변환)
            Object weekReturn = metric.get("52WeekPriceReturnDaily");
            if (weekReturn == null) {
                return useFallback(ticker, "52주 수익률 데이터 없음");
            }

            BigDecimal annualReturn = new BigDecimal(weekReturn.toString())
                    .divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);

            returnCache.put(ticker, new CachedReturn(annualReturn, Instant.now().toEpochMilli()));
            log.info("Finnhub 52주 수익률 조회 완료 (ticker={}, return={})", ticker, annualReturn);
            return annualReturn;

        } catch (Exception e) {
            log.warn("Finnhub 연수익률 조회 실패 (ticker={})", ticker, e);
            return useFallback(ticker, "예외 발생: " + e.getMessage());
        }
    }

    private BigDecimal useFallback(String ticker, String reason) {
        log.info("ticker={} 에 기본 수익률({}) 적용. 이유: {}", ticker, FALLBACK_ANNUAL_RETURN, reason);
        return FALLBACK_ANNUAL_RETURN;
    }
}
