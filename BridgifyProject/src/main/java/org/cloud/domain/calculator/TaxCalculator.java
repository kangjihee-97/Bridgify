package org.cloud.domain.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

@Component
public class TaxCalculator {

    private static final BigDecimal DEFAULT_EXEMPTION = BigDecimal.valueOf(2_500_000);

    public BigDecimal calculateTax(BigDecimal totalProfit, BigDecimal taxRate) {

        if (totalProfit == null || taxRate == null) {
            return BigDecimal.ZERO;
        }

        if (taxRate.compareTo(BigDecimal.ZERO) < 0
                || taxRate.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal taxableAmount = totalProfit.subtract(DEFAULT_EXEMPTION);

        if (taxableAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return taxableAmount
                .multiply(taxRate)
                .setScale(0, RoundingMode.HALF_UP);
    }
}