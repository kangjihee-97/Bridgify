package org.cloud.domain.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

import org.cloud.mapper.DividendHistoryMapper;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DividendCalculator {

    // 배당소득세 15.4% (소득세 14% + 지방소득세 1.4%)
    private static final BigDecimal DIVIDEND_TAX_RATE = BigDecimal.valueOf(0.154);

    private final DividendHistoryMapper dividendHistoryMapper;

    /**
     * 한 해 동안 보유 종목에서 발생한 세후 배당(원화) 합계를 계산한다.
     * reinvest=true면 세후 배당(USD)으로 같은 종목을 그 해 주가에 재매수(DRIP)해 보유 주식수를 늘린다.
     *
     * @param holdings   종목별 보유 주식수 (연도 시작 시점) : ticker -> 주식수
     * @param year       대상 연도
     * @param usdKrwRate 해당 연도 USD -> KRW 환율
     * @param prices     종목별 해당 연도 주가(USD) : ticker -> 주가 (재투자 계산용, 없으면 재투자 생략)
     * @param reinvest   배당 재투자(DRIP) 여부
     * @return 세후 배당(원화) 합계 + 재투자 반영된 보유 주식수
     */
    public DividendResult calculateYearlyDividend(
            Map<String, BigDecimal> holdings,
            int year,
            BigDecimal usdKrwRate,
            Map<String, BigDecimal> prices,
            boolean reinvest
    ) {
        BigDecimal totalAfterTaxKrw = BigDecimal.ZERO;
        Map<String, BigDecimal> updatedHoldings = new HashMap<>(holdings);

        for (Map.Entry<String, BigDecimal> entry : holdings.entrySet()) {
            String ticker = entry.getKey();
            BigDecimal shares = entry.getValue();

            if (shares == null || shares.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // 그 해 주당배당금(DPS) 조회 — 배당이 없는 종목/연도면 건너뜀
            BigDecimal dps = dividendHistoryMapper.findDps(ticker, year);
            if (dps == null || dps.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // 세전 배당(USD) = 보유주식수 × 주당배당금
            BigDecimal grossUsd = shares.multiply(dps);

            // 배당소득세 15.4% 차감 → 세후 배당(USD)
            BigDecimal afterTaxUsd = grossUsd.multiply(BigDecimal.ONE.subtract(DIVIDEND_TAX_RATE));

            // 원화 환산 (그 해 환율)
            BigDecimal afterTaxKrw = afterTaxUsd
                    .multiply(usdKrwRate)
                    .setScale(0, RoundingMode.HALF_UP);

            totalAfterTaxKrw = totalAfterTaxKrw.add(afterTaxKrw);

            // 배당 재투자(DRIP): 세후 배당(USD)으로 그 해 주가에 추가 매수
            if (reinvest && prices != null) {
                BigDecimal price = prices.get(ticker);
                if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal addedShares = afterTaxUsd.divide(price, 6, RoundingMode.HALF_UP);
                    updatedHoldings.merge(ticker, addedShares, BigDecimal::add);
                }
            }
        }

        return new DividendResult(totalAfterTaxKrw, updatedHoldings);
    }
}
