package org.cloud.mapper;

import java.util.List;
import org.cloud.domain.entity.AssetItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AssetItemMapper {
    void insertAssetItem(AssetItem assetItem);
    List<AssetItem> findByConfigId(Integer configId);
}