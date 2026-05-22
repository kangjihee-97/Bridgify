package org.cloud.domain.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class InflationCalculator {

	public BigDecimal toRealValue(BigDecimal futureValue, BigDecimal rate, int years) {
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) return futureValue;

        // 1. 3.0(%)을 0.03으로 변환
        BigDecimal decimalRate = rate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        
        // 2. (1 + 0.03)^n 계산
        BigDecimal divisor = BigDecimal.ONE.add(decimalRate).pow(years);
        
        // 3. 실질 가치 계산
        return futureValue.divide(divisor, 2, RoundingMode.HALF_UP);
    }
}