package org.cloud.service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
public class MarketDataService {

    private final RestTemplate restTemplate;

    @Value("${api.alphavantage.key}")
    private String stockApiKey;

    private final Map<String, BigDecimal> stockCache = new ConcurrentHashMap<>();

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
}