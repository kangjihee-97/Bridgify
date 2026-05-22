package org.cloud.domain.entity;

import java.math.BigDecimal;
import java.sql.Timestamp;
import lombok.Data;

@Data
public class SimulationConfig {
    private Long configId;           // PK
    private BigDecimal initialAmount;   // 초기 투자금
    private BigDecimal monthlyDeposit;  // 월 적립금
    private Integer durationYears;      // 기간
    private BigDecimal krInflationRate; // 한국 물가 상승률
    private BigDecimal taxRate;         // 세율
    private Timestamp createdAt;        // 생성일
}