package org.cloud.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.cloud.domain.calculator.CompoundCalculator;
import org.cloud.dto.AssetAllocation;
import org.cloud.dto.SimulationRequest;
import org.cloud.dto.YearlyResultResponse;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CalculationService {

    private final CompoundCalculator compoundCalculator;
    private final MarketDataService marketDataService;

    // 사용자가 기대수익률을 직접 입력하지 않고, 자산 배분도 비어 있을 때만 쓰는 최종 기본값
    private static final BigDecimal DEFAULT_RETURN = new BigDecimal("0.08");

    public List<YearlyResultResponse> calculate(SimulationRequest request) {
        BigDecimal expectedReturn = resolveExpectedReturn(request);
        return compoundCalculator.runSimulation(request, expectedReturn);
    }

    private BigDecimal resolveExpectedReturn(SimulationRequest request) {
        // 1. 사용자가 기대수익률을 직접 입력한 경우 그 값을 최우선으로 사용
        if (request.getExpectedReturn() != null) {
            return toRate(request.getExpectedReturn());
        }

        // 2. 자산 배분이 있는 경우, 종목별 수익률을 비율대로 가중평균
        if (request.getAssets() != null && !request.getAssets().isEmpty()) {
            BigDecimal totalRatio = BigDecimal.ZERO;
            BigDecimal weightedSum = BigDecimal.ZERO;

            for (AssetAllocation asset : request.getAssets()) {
                BigDecimal ratio = asset.getRatio();
                if (ratio == null) continue;

                if (ratio.compareTo(BigDecimal.ZERO) < 0 ||
                    ratio.compareTo(BigDecimal.valueOf(100)) > 0) {
                    continue;
                }

                BigDecimal tickerReturn = resolveTickerReturn(asset);

                BigDecimal weightedReturn = tickerReturn
                        .multiply(ratio)
                        .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);

                weightedSum = weightedSum.add(weightedReturn);
                totalRatio = totalRatio.add(ratio);
            }

            if (totalRatio.compareTo(BigDecimal.ZERO) > 0) {
                return weightedSum;
            }
        }

        // 3. 입력도, 자산 배분도 없는 경우 최종 기본값
        return DEFAULT_RETURN;
    }

    // DB의 return_rate 컬럼이 DECIMAL(7,4)라서 최대 ±999.9999(99999.99%)까지만 저장 가능.
    // 그보다 훨씬 안전한 범위로 상한/하한을 둬서, 매수일자가 오늘에 가까울 때 생기는
    // 비현실적인 연환산 수치(수백~수천%)가 DB 저장 단계에서 터지지 않게 막는다.
    private static final BigDecimal MAX_REASONABLE_RETURN = new BigDecimal("5.0");   // +500%
    private static final BigDecimal MIN_REASONABLE_RETURN = new BigDecimal("-0.9");  // -90%

    // 매수일자가 이보다 가까우면 연환산(제곱) 자체가 통계적으로 의미가 없다고 보고
    // 매수 정보 분기를 타지 않고 시장 평균 CAGR로 대체한다.
    private static final long MIN_HOLDING_DAYS_FOR_ANNUALIZATION = 30;

    /**
     * 종목 하나의 연환산 수익률을 결정한다.
     *
     * 과거 매수 정보(매수일자, 매수가)가 입력된 경우에는
     * "내가 실제로 그 가격에 샀다면 지금까지 연평균 몇 %씩 벌었는가"를 직접 계산한다.
     * 매수 정보가 없으면 기존처럼 시장의 최근 5년 CAGR(MarketDataService)을 대신 사용한다.
     *
     * 환율은 이 계산(연환산 수익률)에는 영향을 주지 않는다 — 매수가/현재가 모두
     * 원래 통화(USD) 기준으로 비교하기 때문. 환율은 AssetService에서 거래 내역을
     * 원화로 환산해 저장할 때만 사용되며, 거기서는 입력이 없으면 한국수출입은행
     * API로 자동 조회된다.
     */
    private BigDecimal resolveTickerReturn(AssetAllocation asset) {

        if (asset.getPurchaseDate() != null && asset.getPurchasePrice() != null) {

            long days = ChronoUnit.DAYS.between(asset.getPurchaseDate(), LocalDate.now());

            // 매수일자가 너무 최근(한 달 미만)이면 연환산이 의미가 없으므로
            // 시장 평균 CAGR로 대체한다 — 매수일이 오늘이어도 에러 없이 동작하게.
            if (days < MIN_HOLDING_DAYS_FOR_ANNUALIZATION) {
                return marketDataService.fetchAnnualizedReturn(asset.getTicker());
            }

            BigDecimal currentPrice = marketDataService.fetchStockPrice(asset.getTicker());

            if (currentPrice != null && currentPrice.compareTo(BigDecimal.ZERO) > 0
                    && asset.getPurchasePrice().compareTo(BigDecimal.ZERO) > 0) {

                double years = days / 365.0;

                double ratio = currentPrice
                        .divide(asset.getPurchasePrice(), 10, RoundingMode.HALF_UP)
                        .doubleValue();

                double cagr = Math.pow(ratio, 1.0 / years) - 1.0;

                BigDecimal result = BigDecimal.valueOf(cagr).setScale(6, RoundingMode.HALF_UP);

                // 비현실적인 값은 합리적인 범위로 강제 보정 (DB 컬럼 오버플로우 방지)
                if (result.compareTo(MAX_REASONABLE_RETURN) > 0) {
                    return MAX_REASONABLE_RETURN;
                }
                if (result.compareTo(MIN_REASONABLE_RETURN) < 0) {
                    return MIN_REASONABLE_RETURN;
                }

                return result;
            }
        }

        // 매수 정보가 없거나 현재가 조회에 실패하면, 시장의 최근 5년 CAGR로 대체
        return marketDataService.fetchAnnualizedReturn(asset.getTicker());
    }

    private BigDecimal toRate(BigDecimal value) {
        return value.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
    }
}