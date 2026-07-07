package org.cloud.mapper;

import java.math.BigDecimal;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PriceHistoryMapper {

    // 특정 종목·연도의 주가 조회 — 없으면 null
    BigDecimal findPrice(@Param("ticker") String ticker, @Param("year") int year);
}