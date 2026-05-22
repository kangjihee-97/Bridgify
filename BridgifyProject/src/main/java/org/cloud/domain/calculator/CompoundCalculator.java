package org.cloud.domain.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.cloud.dto.SimulationRequest;
import org.cloud.dto.YearlyResultResponse;
import org.springframework.stereotype.Component;

@Component
public class CompoundCalculator {

    private final InflationCalculator inflationCalculator;

    public CompoundCalculator(InflationCalculator inflationCalculator) {
        this.inflationCalculator = inflationCalculator;
    }

    public List<YearlyResultResponse> runSimulation(
            SimulationRequest request,
            BigDecimal expectedReturn
    ) {

        List<YearlyResultResponse> results = new ArrayList<>();

        BigDecimal currentNominal = request.getInitialAmount();
        BigDecimal totalContribution = request.getInitialAmount();

        for (int year = 1; year <= request.getDurationYears(); year++) {

            // 연간 납입금
            BigDecimal annualDeposit = request.getMonthlyDeposit()
                    .multiply(BigDecimal.valueOf(12));

            // 원금 증가
            currentNominal = currentNominal.add(annualDeposit);

            // 총 투자 원금 증가
            totalContribution = totalContribution.add(annualDeposit);

            // 연 수익 계산
            BigDecimal profit = currentNominal.multiply(expectedReturn);

            // 세금 제외 (최종 단계에서만 과세)
            currentNominal = currentNominal.add(profit);

            // 누적 총수익
            BigDecimal totalProfit = currentNominal.subtract(totalContribution);

            // 물가 반영 실질 가치
            BigDecimal realValue = inflationCalculator.toRealValue(
                    currentNominal,
                    request.getKrInflationRate(),
                    year
            );

            results.add(
                    YearlyResultResponse.builder()
                            .year(year)
                            .nominalBalanceKrw(currentNominal)
                            .realBalanceKrw(realValue)
                            .annualProfit(profit)
                            .totalProfit(totalProfit)
                            .cumulativeReturnRate(
                                    calculateReturnRate(
                                            currentNominal,
                                            totalContribution
                                    )
                            )
                            .build()
            );
        }

        return results;
    }

    private BigDecimal calculateReturnRate(
            BigDecimal current,
            BigDecimal contribution
    ) {

        if (contribution.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return current.subtract(contribution)
                .divide(contribution, 4, RoundingMode.HALF_UP);
    }
}