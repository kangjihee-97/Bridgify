package org.cloud.mapper;

import org.cloud.domain.entity.SimulationConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SimulationConfigMapper {
    void insertConfig(SimulationConfig config);  
    SimulationConfig findById(Integer configId);
}