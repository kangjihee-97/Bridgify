package org.cloud.mapper;

import java.math.BigDecimal;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PriceHistoryMapper {

    // 특정 종목·연도의 주가 조회 — 없으면 null
    BigDecimal findPrice(@Param("ticker") String ticker, @Param("year") int year);

    // 특정 종목의 주가 데이터 개수 (캐시 존재 여부 확인용)
    int countByTicker(@Param("ticker") String ticker);

    // 주가 이력 저장 (같은 종목·연도가 있으면 갱신) — API 캐싱용
    void upsert(@Param("ticker") String ticker,
                @Param("year") int year,
                @Param("price") BigDecimal price);
}