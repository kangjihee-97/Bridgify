package org.cloud.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationResponse {
	
    private Long configId;
    private BigDecimal nominalBalanceKrw; 
    private BigDecimal realBalanceKrw;
    private BigDecimal totalProfit;
    private BigDecimal tax;
    private BigDecimal totalTax;
    private BigDecimal returnRate;
    private List<YearlyResultResponse> yearlyResults; 
    private String assetComparisonText;
    private BigDecimal totalPrincipal;

}