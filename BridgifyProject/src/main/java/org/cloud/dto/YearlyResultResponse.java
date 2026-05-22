package org.cloud.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YearlyResultResponse {

    private Integer year;
    private BigDecimal nominalBalanceKrw;
    private BigDecimal realBalanceKrw;
    private BigDecimal annualProfit;
    private BigDecimal totalProfit;
    private BigDecimal cumulativeReturnRate;
    private String assetComparison;
}