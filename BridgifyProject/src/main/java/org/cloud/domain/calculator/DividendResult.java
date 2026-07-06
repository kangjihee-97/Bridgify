package org.cloud.domain.calculator;

import java.math.BigDecimal;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DividendResult {

    // 해당 연도 세후 배당금 합계 (원화)
    private BigDecimal afterTaxDividendKrw;

    // 해당 연도 배당소득세 합계 (원화)
    private BigDecimal dividendTaxKrw;

    // 재투자(DRIP) 반영 후 보유 주식수 (ticker -> 주식수)
    private Map<String, BigDecimal> updatedHoldings;
}