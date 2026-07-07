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
public class SimulationRequest {	
  
    private BigDecimal initialAmount;   
    private BigDecimal monthlyDeposit;
    private Integer durationYears;
    private BigDecimal krInflationRate;    
    private BigDecimal taxRate;
    private BigDecimal expectedReturn;
    private List<AssetAllocation> assets;
    private Boolean reinvest;

}