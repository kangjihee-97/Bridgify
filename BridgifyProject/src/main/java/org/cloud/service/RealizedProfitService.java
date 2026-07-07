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
import org.cloud.mapper.PriceHistoryMapper;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * 실현손익 정산 서비스 (배당 재투자 DRIP 반영).
 *
 * 재투자(DRIP): 매년 받은 배당으로 그 해 주가에 주식을 더 사서 보유 주식수를 늘린다.
 * → 배당이 "현금"이 아니라 "주식"이 되므로, 최종 주식수로 현재 평가액을 계산하고
 *   순수익 = 현재 평가액 − 원금 − 양도세 로 계산한다. (배당은 주식에 녹아있어 따로 더하지 않음)
 */
@Service
@RequiredArgsConstructor
public class RealizedProfitService {

    private final MarketDataService marketDataService;
    private final ExchangeRateService exchangeRateService;
    private final DividendCalculator dividendCalculator;
    private final TaxCalculator taxCalculator;
    private final PriceHistoryMapper priceHistoryMapper;

    public RealizedProfitResponse calculate(SimulationRequest request) {

        List<RealizedAssetResult> assetResults = new ArrayList<>();

        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalCurrentValue = BigDecimal.ZERO;
        BigDecimal totalCapitalGain = BigDecimal.ZERO;
        BigDecimal totalDividend = BigDecimal.ZERO;       // 재투자된 세후 배당 (표시용)
        BigDecimal totalDividendTax = BigDecimal.ZERO;     // 배당소득세 누적

        BigDecimal initialAmount = (request.getInitialAmount() != null)
                ? request.getInitialAmount() : BigDecimal.ZERO;

        BigDecimal currentRate = exchangeRateService.fetchUsdToKrwRate();

        // 배당 재투자(DRIP) 여부 — 값이 없으면 기본 ON(true)
        boolean reinvest = (request.getReinvest() == null) || request.getReinvest();

        if (request.getAssets() != null) {
            for (AssetAllocation asset : request.getAssets()) {

                if (asset.getPurchaseDate() == null
                        || asset.getPurchasePrice() == null
                        || asset.getPurchasePrice().compareTo(BigDecimal.ZERO) <= 0
                        || asset.getRatio() == null) {
                    continue;
                }

                // 1) 매수 시점 환율
                BigDecimal purchaseRate = (asset.getPurchaseRate() != null)
                        ? asset.getPurchaseRate()
                        : exchangeRateService.fetchUsdToKrwRate(asset.getPurchaseDate());

                // 2) 이 종목에 투입한 원화
                BigDecimal allocatedKrw = initialAmount
                        .multiply(asset.getRatio())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                // 3) 최초 보유 주식수 = 투입원화 ÷ (매수가 × 매수환율)
                BigDecimal purchasePriceKrw = asset.getPurchasePrice().multiply(purchaseRate);
                if (purchasePriceKrw.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                BigDecimal shares = allocatedKrw.divide(purchasePriceKrw, 6, RoundingMode.HALF_UP);

                BigDecimal costBasisKrw = allocatedKrw.setScale(0, RoundingMode.HALF_UP);

                // 4) 매수연도 ~ 올해까지: 배당 계산 + 재투자(DRIP)로 주식수 늘리기
                BigDecimal dividendKrw = BigDecimal.ZERO;
                int startYear = asset.getPurchaseDate().getYear();
                int endYear = LocalDate.now().getYear();

                Map<String, BigDecimal> holdings = new HashMap<>();
                holdings.put(asset.getTicker(), shares);

                for (int y = startYear; y <= endYear; y++) {
                    // 그 해 주가 조회 (재투자 시 몇 주 더 살지 계산에 사용)
                    Map<String, BigDecimal> prices = new HashMap<>();
                    BigDecimal yearPrice = priceHistoryMapper.findPrice(asset.getTicker(), y);
                    if (yearPrice != null) {
                        prices.put(asset.getTicker(), yearPrice);
                    }

                    DividendResult r = dividendCalculator.calculateYearlyDividend(
                            holdings, y, currentRate, prices, reinvest); // 재투자(DRIP) 여부는 요청값

                    dividendKrw = dividendKrw.add(r.getAfterTaxDividendKrw());
                    totalDividendTax = totalDividendTax.add(r.getDividendTaxKrw());
                    holdings = r.getUpdatedHoldings(); // 재투자로 주식수 증가
                }

                // 재투자까지 반영된 최종 보유 주식수
                BigDecimal finalShares = holdings.get(asset.getTicker());

                // 5) 현재 평가액 = 최종 주식수 × 현재가 (재투자분 포함)
                BigDecimal currentPriceUsd = marketDataService.fetchStockPrice(asset.getTicker());
                BigDecimal currentValueKrw;
                if (currentPriceUsd != null && currentPriceUsd.compareTo(BigDecimal.ZERO) > 0) {
                    currentValueKrw = finalShares.multiply(currentPriceUsd).multiply(currentRate)
                            .setScale(0, RoundingMode.HALF_UP);
                } else {
                    currentValueKrw = costBasisKrw;
                }

                // 6) 시세차익 = 현재 평가액 − 투입 원금 (재투자 배당 성장분 포함)
                BigDecimal capitalGainKrw = currentValueKrw.subtract(costBasisKrw);

                assetResults.add(RealizedAssetResult.builder()
                        .ticker(asset.getTicker())
                        .shares(finalShares)
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

        // 7) 양도소득세 — 전체 시세차익에 250만 공제 후 세율
        BigDecimal taxRateFraction = toFraction(request.getTaxRate());
        BigDecimal capitalGainsTax = taxCalculator.calculateTax(totalCapitalGain, taxRateFraction);

        // 8) 순 실현손익 = 시세차익(재투자 포함) − 양도세
        //    (배당은 이미 주식으로 재투자돼 시세차익에 포함되므로 따로 더하지 않음)
        BigDecimal netRealizedProfit = totalCapitalGain.subtract(capitalGainsTax);

        return RealizedProfitResponse.builder()
                .assets(assetResults)
                .totalCostKrw(totalCost)
                .totalCurrentValueKrw(totalCurrentValue)
                .totalCapitalGainKrw(totalCapitalGain)
                .totalDividendKrw(totalDividend)
                .capitalGainsTaxKrw(capitalGainsTax)
                .dividendTaxKrw(totalDividendTax)
                .netRealizedProfitKrw(netRealizedProfit)
                .build();
    }

    private BigDecimal toFraction(BigDecimal taxRate) {
        if (taxRate == null) {
            return BigDecimal.ZERO;
        }
        return (taxRate.compareTo(BigDecimal.ONE) > 0)
                ? taxRate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
                : taxRate;
    }
}