package org.cloud.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.cloud.domain.entity.DividendHistory;

@Mapper
public interface DividendHistoryMapper {

    // 특정 종목·연도의 주당배당금(DPS) 조회 — 없으면 null
    BigDecimal findDps(@Param("ticker") String ticker, @Param("year") int year);

    // 특정 종목의 전체 배당 이력 조회
    List<DividendHistory> findByTicker(@Param("ticker") String ticker);

    // 배당 이력 저장 (같은 종목·연도가 있으면 갱신) — API 캐싱용
    void upsert(@Param("ticker") String ticker,
                @Param("year") int year,
                @Param("dps") BigDecimal dps);
}