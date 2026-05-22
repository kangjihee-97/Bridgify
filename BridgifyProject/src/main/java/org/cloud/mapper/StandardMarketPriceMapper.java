package org.cloud.mapper;

import java.util.List;
import org.cloud.domain.entity.StandardMarketPrice;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StandardMarketPriceMapper {
    
    List<StandardMarketPrice> findAll();
    StandardMarketPrice findByItemName(String itemName);
}
