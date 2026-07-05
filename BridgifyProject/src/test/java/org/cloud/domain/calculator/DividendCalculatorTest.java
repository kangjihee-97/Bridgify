package org.cloud.domain.calculator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.cloud.mapper.DividendHistoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DividendCalculatorTest {

    private DividendHistoryMapper dividendHistoryMapper;
    private DividendCalculator dividendCalculator;

    @BeforeEach
    void setUp() {
        // 매퍼는 DB 대신 가짜(mock)로 대체 → 계산 로직만 순수하게 검증
        dividendHistoryMapper = Mockito.mock(DividendHistoryMapper.class);
        dividendCalculator = new DividendCalculator(dividendHistoryMapper);
    }

    @Test
    @DisplayName("재투자 없음 - 세후 배당(원화)이 정확히 계산된다")
    void calculateDividend_noReinvest() {
        // given: AAPL 100주, 2020년 DPS 0.795, 환율 1200원
        when(dividendHistoryMapper.findDps("AAPL", 2020))
                .thenReturn(new BigDecimal("0.7950"));

        Map<String, BigDecimal> holdings = new HashMap<>();
        holdings.put("AAPL", new BigDecimal("100"));

        // when
        DividendResult result = dividendCalculator.calculateYearlyDividend(
                holdings, 2020, new BigDecimal("1200"), null, false);

        // then: 100 * 0.795 * (1 - 0.154) * 1200 = 80,708원
        assertThat(result.getAfterTaxDividendKrw()).isEqualByComparingTo("80708");
        // 재투자 안 했으니 보유 주식수는 그대로
        assertThat(result.getUpdatedHoldings().get("AAPL")).isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("재투자(DRIP) - 세후 배당으로 추가 매수해 보유 주식수가 늘어난다")
    void calculateDividend_withReinvest() {
        when(dividendHistoryMapper.findDps("AAPL", 2020))
                .thenReturn(new BigDecimal("0.7950"));

        Map<String, BigDecimal> holdings = new HashMap<>();
        holdings.put("AAPL", new BigDecimal("100"));

        Map<String, BigDecimal> prices = new HashMap<>();
        prices.put("AAPL", new BigDecimal("130"));  // 2020년 주가(USD)

        DividendResult result = dividendCalculator.calculateYearlyDividend(
                holdings, 2020, new BigDecimal("1200"), prices, true);

        // 세후배당(USD) = 100 * 0.795 * 0.846 = 67.257
        // 추가주식 = 67.257 / 130 = 0.517362 (소수 6자리 반올림)
        // 보유주식 = 100 + 0.517362 = 100.517362
        assertThat(result.getUpdatedHoldings().get("AAPL"))
                .isEqualByComparingTo("100.517362");
        assertThat(result.getAfterTaxDividendKrw()).isEqualByComparingTo("80708");
    }

    @Test
    @DisplayName("배당 이력이 없는 종목/연도는 배당 0으로 건너뛴다")
    void calculateDividend_noDps() {
        // TSLA는 배당 데이터가 없다고 가정 → null 반환
        when(dividendHistoryMapper.findDps("TSLA", 2020)).thenReturn(null);

        Map<String, BigDecimal> holdings = new HashMap<>();
        holdings.put("TSLA", new BigDecimal("50"));

        DividendResult result = dividendCalculator.calculateYearlyDividend(
                holdings, 2020, new BigDecimal("1200"), null, false);

        assertThat(result.getAfterTaxDividendKrw()).isEqualByComparingTo("0");
        assertThat(result.getUpdatedHoldings().get("TSLA")).isEqualByComparingTo("50");
    }
}