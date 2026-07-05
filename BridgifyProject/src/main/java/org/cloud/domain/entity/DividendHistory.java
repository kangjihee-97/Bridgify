package org.cloud.domain.entity;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class DividendHistory {
    private Integer dividendId;
    private String ticker;
    private Integer year;
    private BigDecimal dps;   // 주당배당금 (미국주식이면 USD)
}
