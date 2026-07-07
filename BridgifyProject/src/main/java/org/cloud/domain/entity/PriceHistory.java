package org.cloud.domain.entity;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class PriceHistory {
    private Integer priceHistId;
    private String ticker;
    private Integer year;
    private BigDecimal price;   // 그 해 주가 (미국주식이면 USD)
}