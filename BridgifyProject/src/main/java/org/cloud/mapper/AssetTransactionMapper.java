package org.cloud.mapper;

import java.util.List;
import org.cloud.domain.entity.AssetTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AssetTransactionMapper {
    void insertTransaction(AssetTransaction transaction);
    void insertTransactions(@Param("list") List<AssetTransaction> transactions); 
    List<AssetTransaction> findByAssetId(Integer assetId);
}