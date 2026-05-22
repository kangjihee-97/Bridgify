package org.cloud.domain.entity;

import lombok.Data;

@Data
public class AssetItem {
    private Long assetId;            // PK
    private Long configId;           // FK (SimulationConfig)
    private String ticker;           // 종목 티커
}