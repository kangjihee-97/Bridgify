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
public class RealizedProfitResponse {
    private List<RealizedAssetResult> assets;   // 종목별 상세

    private BigDecimal totalCostKrw;            // 총 투입 원금
    private BigDecimal totalCurrentValueKrw;    // 총 현재 평가액
    private BigDecimal totalCapitalGainKrw;     // 총 시세차익
    private BigDecimal totalDividendKrw;        // 총 세후 배당
    private BigDecimal capitalGainsTaxKrw;      // 양도소득세 (250만 공제 후)
    private BigDecimal dividendTaxKrw;          // 배당소득세 (15.4%)
    private BigDecimal netRealizedProfitKrw;    // 순 실현손익 = 시세차익 + 배당 − 양도세
}