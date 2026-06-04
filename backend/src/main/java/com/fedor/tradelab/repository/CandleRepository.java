package com.fedor.tradelab.repository;

import com.fedor.tradelab.model.CandleEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CandleRepository extends JpaRepository<CandleEntity, Long> {

    List<CandleEntity> findBySymbolAndIntervalOrderByOpenTimeDesc(
            String symbol,
            String interval,
            Pageable pageable);

    long countBySymbolAndInterval(String symbol, String interval);

    @Query("SELECT c.openTime FROM CandleEntity c WHERE c.symbol = :symbol AND c.interval = :interval")
    List<Long> findOpenTimesBySymbolAndInterval(@Param("symbol") String symbol,
                                                @Param("interval") String interval);

    @Query("SELECT MAX(c.openTime) FROM CandleEntity c WHERE c.symbol = :symbol AND c.interval = :interval")
    Long findMaxOpenTime(@Param("symbol") String symbol,
                         @Param("interval") String interval);
}
