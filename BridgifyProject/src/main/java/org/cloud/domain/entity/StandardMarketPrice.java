package org.cloud.domain.entity;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class StandardMarketPrice {
    private Integer priceId;
    private String itemName;
    private BigDecimal price;
    private String unit;
    private String country;
    private Integer year;
}