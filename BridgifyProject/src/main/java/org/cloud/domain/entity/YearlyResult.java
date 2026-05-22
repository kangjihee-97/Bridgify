package org.cloud.domain.entity;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class YearlyResult {
    private Long resultId;           // PK
    private Long configId;            // FK (SimulationConfig)
    private Integer year;               // 연차
    private BigDecimal realBalanceKrw; // 실질 잔고
    private BigDecimal annualProfit;   //  올해의 수익
    private BigDecimal totalProfit;     //  누적 총 수익
    private BigDecimal returnRate;      // 수익률
}