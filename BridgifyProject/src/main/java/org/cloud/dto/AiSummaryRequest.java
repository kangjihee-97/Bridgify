package org.cloud.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** AI 해설 요청 — 프론트는 숫자 데이터만 보낸다 (프롬프트는 서버가 조립) */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSummaryRequest {

    private Integer durationYears;
    private BigDecimal totalPrincipal;
    private BigDecimal nominalBalanceKrw;
    private BigDecimal realBalanceKrw;
    private BigDecimal totalProfit;
    private BigDecimal returnRate;
    private BigDecimal tax;
    private List<String> tickers;
}