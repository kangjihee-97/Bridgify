package org.cloud.mapper;

import java.util.List;
import org.cloud.domain.entity.YearlyResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface YearlyResultMapper {

    void insertResults(@Param("list") List<YearlyResult> results);

    List<YearlyResult> findByConfigId(Long configId);
}