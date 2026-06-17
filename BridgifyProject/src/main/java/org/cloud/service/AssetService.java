package org.cloud.service;

import java.util.ArrayList;
import java.util.List;

import org.cloud.domain.entity.AssetItem;
import org.cloud.domain.entity.AssetTransaction;
import org.cloud.domain.entity.SimulationConfig;
import org.cloud.dto.AssetAllocation;
import org.cloud.dto.SimulationRequest;
import org.cloud.mapper.AssetItemMapper;
import org.cloud.mapper.AssetTransactionMapper;
import org.cloud.mapper.SimulationConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final SimulationConfigMapper configMapper;
    private final AssetItemMapper assetItemMapper;
    private final AssetTransactionMapper assetTransactionMapper;

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

        List<AssetTransaction> transactions = new ArrayList<>();

        for (AssetAllocation asset : request.getAssets()) {

            AssetItem item = new AssetItem();
            item.setConfigId(configId);
            item.setTicker(asset.getTicker());

            assetItemMapper.insertAssetItem(item);

            // 과거 매수 정보가 입력된 종목만 거래 내역으로 저장한다.
            // (매수일자/평단가/환율 중 하나라도 없으면 "오늘부터 신규 투자"로 간주해 거래 내역을 만들지 않는다.)
            if (asset.getPurchaseDate() != null
                    && asset.getPurchasePrice() != null
                    && asset.getPurchaseRate() != null) {

                AssetTransaction tx = new AssetTransaction();
                tx.setAssetId(item.getAssetId());
                tx.setTxDate(java.sql.Date.valueOf(asset.getPurchaseDate()));
                tx.setQuantity(java.math.BigDecimal.ONE); // 평단가 기준 시뮬레이션이라 수량은 1단위로 고정
                tx.setPrice(asset.getPurchasePrice());
                tx.setExchangeRate(asset.getPurchaseRate());

                transactions.add(tx);
            }
        }

        if (!transactions.isEmpty()) {
            assetTransactionMapper.insertTransactions(transactions);
        }

        return configId;
    }
}