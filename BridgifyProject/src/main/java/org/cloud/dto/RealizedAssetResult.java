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
public class RealizedAssetResult {
    private String ticker;
    private BigDecimal shares;            // 보유 주식수
    private BigDecimal costBasisKrw;      // 투입 원금 (원화)
    private BigDecimal currentValueKrw;   // 현재 평가액 (원화)
    private BigDecimal capitalGainKrw;    // 시세차익 (현재평가 − 원금)
    private BigDecimal dividendKrw;       // 보유기간 세후 배당 합계 (원화)
}