package org.cloud.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.cloud.domain.calculator.CompoundCalculator;
import org.cloud.dto.SimulationRequest;
import org.cloud.dto.YearlyResultResponse;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CalculationService {

    private final CompoundCalculator compoundCalculator;
    private static final BigDecimal DEFAULT_RETURN = new BigDecimal("0.08");

    public List<YearlyResultResponse> calculate(SimulationRequest request) {
        BigDecimal expectedReturn = resolveExpectedReturn(request);
        return compoundCalculator.runSimulation(request, expectedReturn);
    }

    private BigDecimal resolveExpectedReturn(SimulationRequest request) {
        if (request.getExpectedReturn() != null) {
            return toRate(request.getExpectedReturn());
        }

        if (request.getAssets() != null && !request.getAssets().isEmpty()) {
            BigDecimal total = BigDecimal.ZERO;

            for (var asset : request.getAssets()) {
                BigDecimal ratio = asset.getRatio();
                if (ratio == null) continue;

                if (ratio.compareTo(BigDecimal.ZERO) < 0 ||
                    ratio.compareTo(BigDecimal.valueOf(100)) > 0) {
                    continue;
                }

                BigDecimal weightedReturn = DEFAULT_RETURN
                        .multiply(ratio)
                        .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);

                total = total.add(weightedReturn);
            }

            if (total.compareTo(BigDecimal.ZERO) > 0) {
                return total;
            }
        }

        return DEFAULT_RETURN;
    }

    private BigDecimal toRate(BigDecimal value) {
        return value.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
    }
}
