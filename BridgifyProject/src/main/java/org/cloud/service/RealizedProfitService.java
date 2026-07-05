package org.cloud.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloud.domain.calculator.DividendCalculator;
import org.cloud.domain.calculator.DividendResult;
import org.cloud.domain.calculator.TaxCalculator;
import org.cloud.dto.AssetAllocation;
import org.cloud.dto.RealizedAssetResult;
import org.cloud.dto.RealizedProfitResponse;
import org.cloud.dto.SimulationRequest;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * 실현손익 정산 서비스.
 *
 * "과거에 이 종목을 이 가격/날짜에 샀는데, 지금 팔면 세금·배당 다 반영해서
 *  손에 쥐는 순수익이 얼마인가?"를 계산한다.
 *
 * 조립하는 부품:
 *   - 보유 주식수 : assets(비중·매수가·매수환율)로 역산
 *   - 시세차익   : 현재가(MarketDataService) − 매수가
 *   - 배당       : 매수연도~올해까지 실제 배당(DividendCalculator, 배당세 15.4% 포함)
 *   - 양도세     : TaxCalculator (250만 공제 후 세율)
 *
 * ※ v1 단순화: 배당 재투자(DRIP)와 연도별 환율은 아직 반영하지 않는다.
 *   (DRIP는 '연도별 과거 주가'가 필요 → price_history 테이블 추가 시 켤 수 있음)
 */
@Service
@RequiredArgsConstructor
public class RealizedProfitService {

    private final MarketDataService marketDataService;
    private final ExchangeRateService exchangeRateService;
    private final DividendCalculator dividendCalculator;
    private final TaxCalculator taxCalculator;

    public RealizedProfitResponse calculate(SimulationRequest request) {

        List<RealizedAssetResult> assetResults = new ArrayList<>();

        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalCurrentValue = BigDecimal.ZERO;
        BigDecimal totalCapitalGain = BigDecimal.ZERO;
        BigDecimal totalDividend = BigDecimal.ZERO;

        BigDecimal initialAmount = (request.getInitialAmount() != null)
                ? request.getInitialAmount() : BigDecimal.ZERO;

        // 현재(오늘) 환율 — 현재 평가액/배당 원화 환산에 사용
        BigDecimal currentRate = exchangeRateService.fetchUsdToKrwRate();

        if (request.getAssets() != null) {
            for (AssetAllocation asset : request.getAssets()) {

                // 과거 매수 정보가 없으면 실현손익 계산 불가 → 건너뜀
                if (asset.getPurchaseDate() == null
                        || asset.getPurchasePrice() == null
                        || asset.getPurchasePrice().compareTo(BigDecimal.ZERO) <= 0
                        || asset.getRatio() == null) {
                    continue;
                }

                // 1) 매수 시점 환율 (입력이 없으면 매수일자 기준으로 조회)
                BigDecimal purchaseRate = (asset.getPurchaseRate() != null)
                        ? asset.getPurchaseRate()
                        : exchangeRateService.fetchUsdToKrwRate(asset.getPurchaseDate());

                // 2) 이 종목에 투입한 원화 (초기금액 × 비중)
                BigDecimal allocatedKrw = initialAmount
                        .multiply(asset.getRatio())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                // 3) 보유 주식수 = 투입원화 ÷ (매수가 × 매수환율)
                BigDecimal purchasePriceKrw = asset.getPurchasePrice().multiply(purchaseRate);
                if (purchasePriceKrw.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                BigDecimal shares = allocatedKrw.divide(purchasePriceKrw, 6, RoundingMode.HALF_UP);

                BigDecimal costBasisKrw = allocatedKrw.setScale(0, RoundingMode.HALF_UP);

                // 4) 현재 평가액 (시세차익)
                BigDecimal currentPriceUsd = marketDataService.fetchStockPrice(asset.getTicker());
                BigDecimal currentValueKrw;
                if (currentPriceUsd != null && currentPriceUsd.compareTo(BigDecimal.ZERO) > 0) {
                    currentValueKrw = shares.multiply(currentPriceUsd).multiply(currentRate)
                            .setScale(0, RoundingMode.HALF_UP);
                } else {
                    // 현재가 조회 실패 시 시세차익 0 처리 (원금 = 현재평가)
                    currentValueKrw = costBasisKrw;
                }
                BigDecimal capitalGainKrw = currentValueKrw.subtract(costBasisKrw);

                // 5) 매수연도 ~ 올해까지 실제 배당 합산 (배당세 15.4%는 계산기 내부에서 처리)
                BigDecimal dividendKrw = BigDecimal.ZERO;
                int startYear = asset.getPurchaseDate().getYear();
                int endYear = LocalDate.now().getYear();

                Map<String, BigDecimal> holdings = new HashMap<>();
                holdings.put(asset.getTicker(), shares);

                for (int y = startYear; y <= endYear; y++) {
                    DividendResult r = dividendCalculator.calculateYearlyDividend(
                            holdings, y, currentRate, null, false); // v1: 재투자(DRIP) off
                    dividendKrw = dividendKrw.add(r.getAfterTaxDividendKrw());
                    holdings = r.getUpdatedHoldings();
                }

                assetResults.add(RealizedAssetResult.builder()
                        .ticker(asset.getTicker())
                        .shares(shares)
                        .costBasisKrw(costBasisKrw)
                        .currentValueKrw(currentValueKrw)
                        .capitalGainKrw(capitalGainKrw)
                        .dividendKrw(dividendKrw)
                        .build());

                totalCost = totalCost.add(costBasisKrw);
                totalCurrentValue = totalCurrentValue.add(currentValueKrw);
                totalCapitalGain = totalCapitalGain.add(capitalGainKrw);
                totalDividend = totalDividend.add(dividendKrw);
            }
        }

        // 6) 양도소득세 — 포트폴리오 전체 시세차익에 250만 공제 후 세율 적용
        BigDecimal taxRateFraction = toFraction(request.getTaxRate());
        BigDecimal capitalGainsTax = taxCalculator.calculateTax(totalCapitalGain, taxRateFraction);

        // 7) 순 실현손익 = 시세차익 + 세후배당 − 양도세
        BigDecimal netRealizedProfit = totalCapitalGain
                .add(totalDividend)
                .subtract(capitalGainsTax);

        return RealizedProfitResponse.builder()
                .assets(assetResults)
                .totalCostKrw(totalCost)
                .totalCurrentValueKrw(totalCurrentValue)
                .totalCapitalGainKrw(totalCapitalGain)
                .totalDividendKrw(totalDividend)
                .capitalGainsTaxKrw(capitalGainsTax)
                .netRealizedProfitKrw(netRealizedProfit)
                .build();
    }

    // 세율이 22(%)로 들어오면 0.22로, 이미 0.22면 그대로 (TaxCalculator는 0~1 소수를 기대)
    private BigDecimal toFraction(BigDecimal taxRate) {
        if (taxRate == null) {
            return BigDecimal.ZERO;
        }
        return (taxRate.compareTo(BigDecimal.ONE) > 0)
                ? taxRate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
                : taxRate;
    }
}