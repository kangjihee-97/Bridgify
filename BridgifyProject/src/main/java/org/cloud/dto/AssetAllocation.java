package org.cloud.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetAllocation {

    private String ticker;
    private BigDecimal ratio;

    // 과거 매수 정보 — 입력하지 않으면 null로 두고, 그 경우는 "오늘부터 투자 시작"으로 간주한다.
    private LocalDate purchaseDate;     // 매수일자
    private BigDecimal purchasePrice;   // 매수 평단가 (해당 종목의 원래 통화 기준, 보통 USD)
    private BigDecimal purchaseRate;    // 매수 시점 환율 (KRW/USD)
}