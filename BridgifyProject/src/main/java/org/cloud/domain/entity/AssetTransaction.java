package org.cloud.domain.entity;

import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

@Data
public class AssetTransaction {
    private Long txId;               // PK
    private Long assetId;            // FK (AssetItem)
    private Date txDate;                // 거래일
    private BigDecimal quantity;        // 수량
    private BigDecimal price;           // 주가
    private BigDecimal exchangeRate;    // 환율
}