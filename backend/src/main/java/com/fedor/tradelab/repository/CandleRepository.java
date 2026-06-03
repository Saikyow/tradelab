package com.fedor.tradelab.repository;

import com.fedor.tradelab.model.CandleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CandleRepository extends JpaRepository<CandleEntity, Long> {

    List<CandleEntity> findBySymbolAndIntervalOrderByOpenTimeAsc(
            String symbol,
            String interval);

    long countBySymbolAndInterval(String symbol, String interval);
}
