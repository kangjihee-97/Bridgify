package org.cloud.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.cloud.domain.calculator.TaxCalculator;
import org.cloud.domain.entity.YearlyResult;
import org.cloud.dto.SimulationRequest;
import org.cloud.dto.SimulationResponse;
import org.cloud.dto.YearlyResultResponse;
import org.cloud.mapper.YearlyResultMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SimulationService {

    private final AssetService assetService;
    private final CalculationService calculationService;
    private final TaxCalculator taxCalculator;
    private final YearlyResultMapper yearlyResultMapper;

    public SimulationResponse runSimulation(SimulationRequest request) {

        // 1. 시뮬레이션 설정 저장
        Long configId = assetService.saveConfig(request);

        // 2. 연도별 계산 수행
        List<YearlyResultResponse> results =
                calculationService.calculate(request);

        if (results == null || results.isEmpty()) {
            throw new IllegalStateException(
                    "Simulation result is empty"
            );
        }

        // 3. 마지막 연도 결과
        YearlyResultResponse lastYear =
                results.get(results.size() - 1);

        BigDecimal finalBalance =
                lastYear.getNominalBalanceKrw();

        // 4. 총 투자 원금 계산
        BigDecimal totalPrincipal =
                request.getInitialAmount().add(
                        request.getMonthlyDeposit()
                                .multiply(BigDecimal.valueOf(12))
                                .multiply(
                                        BigDecimal.valueOf(
                                                request.getDurationYears()
                                        )
                                )
                );

        // 5. 누적 순수익
        BigDecimal pureProfit =
                finalBalance.subtract(totalPrincipal);

        // 6. 최종 세금 계산
        BigDecimal tax =
                pureProfit.compareTo(BigDecimal.ZERO) > 0
                        ? taxCalculator.calculateTax(
                                pureProfit,
                                request.getTaxRate()
                        )
                        : BigDecimal.ZERO;

        // 7. DB 저장용 엔티티 변환
        List<YearlyResult> entities =
                results.stream()
                        .map(dto -> {
                            YearlyResult entity =
                                    new YearlyResult();

                            entity.setConfigId(configId);
                            entity.setYear(dto.getYear());

                            entity.setRealBalanceKrw(
                                    dto.getRealBalanceKrw()
                            );

                            entity.setAnnualProfit(
                                    dto.getAnnualProfit()
                            );

                            entity.setTotalProfit(
                                    dto.getTotalProfit()
                            );

                            entity.setReturnRate(
                                    dto.getCumulativeReturnRate()
                            );

                            return entity;
                        })
                        .collect(Collectors.toList());

        // 8. Batch Insert 저장
        yearlyResultMapper.insertResults(entities);

        // 9. 최종 응답 반환
        return SimulationResponse.builder()
                .configId(configId)

                .nominalBalanceKrw(finalBalance)

                .realBalanceKrw(
                        lastYear.getRealBalanceKrw()
                )

                .totalPrincipal(totalPrincipal)

                .totalProfit(pureProfit)

                .tax(tax)

                // 0.15 -> 15%
                .returnRate(
                        lastYear.getCumulativeReturnRate()
                                .multiply(
                                        BigDecimal.valueOf(100)
                                )
                )

                .yearlyResults(results)

                .build();
    }
}