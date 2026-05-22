package org.cloud.service;

import org.cloud.domain.entity.AssetItem;
import org.cloud.domain.entity.SimulationConfig;
import org.cloud.dto.AssetAllocation;
import org.cloud.dto.SimulationRequest;
import org.cloud.mapper.AssetItemMapper;
import org.cloud.mapper.SimulationConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final SimulationConfigMapper configMapper;
    private final AssetItemMapper assetItemMapper;

    @Transactional
    public Long saveConfig(SimulationRequest request) {   
        
        SimulationConfig config = new SimulationConfig();
        config.setInitialAmount(request.getInitialAmount());
        config.setMonthlyDeposit(request.getMonthlyDeposit());
        config.setDurationYears(request.getDurationYears());
        config.setKrInflationRate(request.getKrInflationRate());
        config.setTaxRate(request.getTaxRate());

        configMapper.insertConfig(config);

        Long configId = config.getConfigId().longValue();

        for (AssetAllocation asset : request.getAssets()) {

            AssetItem item = new AssetItem();
            item.setConfigId(configId);
            item.setTicker(asset.getTicker());
            

            assetItemMapper.insertAssetItem(item);
        }

        return configId;
    }
}