package com.fedor.tradelab.service;

import com.fedor.tradelab.model.Candle;
import com.fedor.tradelab.model.CandleEntity;
import com.fedor.tradelab.repository.CandleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CandleService {

    private static final Logger log = LoggerFactory.getLogger(CandleService.class);
    private static final long REQUEST_PAUSE_MS = 250;
    private static final int REFRESH_FETCH_SIZE = 5;

    private static final Map<String, Long> INTERVAL_MS = Map.of(
            "1m", 60_000L,
            "3m", 180_000L,
            "5m", 300_000L,
            "15m", 900_000L,
            "30m", 1_800_000L,
            "1h", 3_600_000L,
            "4h", 14_400_000L,
            "1d", 86_400_000L
    );

    private final BinanceApiClient binanceApiClient;
    private final CandleRepository candleRepository;

    public CandleService(BinanceApiClient binanceApiClient, CandleRepository candleRepository) {
        this.binanceApiClient = binanceApiClient;
        this.candleRepository = candleRepository;
    }

    public List<Candle> getCandles(String symbol, String interval, int limit) {
        long cached = candleRepository.countBySymbolAndInterval(symbol, interval);

        if (cached < limit) {
            int toFetch = Math.min(limit, BinanceApiClient.MAX_LIMIT);
            List<CandleEntity> fresh = binanceApiClient.fetchKlines(symbol, interval, toFetch);
            persistNew(symbol, interval, fresh);
        } else {
            refreshLatestIfStale(symbol, interval);
        }

        List<CandleEntity> recent = candleRepository.findBySymbolAndIntervalOrderByOpenTimeDesc(
                symbol, interval, PageRequest.of(0, limit));
        Collections.reverse(recent);

        List<Candle> result = new ArrayList<>(recent.size());
        for (CandleEntity e : recent) {
            result.add(new Candle(
                    e.getOpenTime(), e.getOpen(), e.getHigh(),
                    e.getLow(), e.getClose(), e.getVolume()));
        }
        return result;
    }

    public long loadHistory(String symbol, String interval, int count) {
        Set<Long> existing = new HashSet<>(
                candleRepository.findOpenTimesBySymbolAndInterval(symbol, interval));
        log.info("Start loading {}/{}: target {} candles, already in DB {}",
                symbol, interval, count, existing.size());

        int newlyLoaded = 0;
        int requests = 0;
        Long endTime = null;

        while (newlyLoaded < count) {
            List<CandleEntity> batch = binanceApiClient.fetchKlines(
                    symbol, interval, BinanceApiClient.MAX_LIMIT, endTime);
            requests++;

            if (batch.isEmpty()) {
                log.info("Empty response from Binance — stopping");
                break;
            }

            long oldestOpenTime = Long.MAX_VALUE;
            List<CandleEntity> toSave = new ArrayList<>(batch.size());
            for (CandleEntity c : batch) {
                if (c.getOpenTime() < oldestOpenTime) {
                    oldestOpenTime = c.getOpenTime();
                }
                if (existing.add(c.getOpenTime())) {
                    toSave.add(c);
                }
            }

            if (!toSave.isEmpty()) {
                candleRepository.saveAll(toSave);
                newlyLoaded += toSave.size();
            }

            log.info("Request #{}: received {}, new {}, total new {}/{}",
                    requests, batch.size(), toSave.size(), newlyLoaded, count);

            if (batch.size() < BinanceApiClient.MAX_LIMIT) {
                log.info("Got {} < {} — exchange history exhausted", batch.size(), BinanceApiClient.MAX_LIMIT);
                break;
            }

            endTime = oldestOpenTime - 1;

            if (!sleepBetweenRequests()) {
                break;
            }
        }

        long total = candleRepository.countBySymbolAndInterval(symbol, interval);
        log.info("Load complete: {} requests, {} new candles, total in DB {}",
                requests, newlyLoaded, total);
        return total;
    }

    private void refreshLatestIfStale(String symbol, String interval) {
        Long intervalMs = INTERVAL_MS.get(interval);
        if (intervalMs == null) {
            return;
        }
        Long latest = candleRepository.findMaxOpenTime(symbol, interval);
        if (latest == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (latest + intervalMs > now) {
            return;
        }
        List<CandleEntity> fresh = binanceApiClient.fetchKlines(symbol, interval, REFRESH_FETCH_SIZE);
        persistNew(symbol, interval, fresh);
    }

    private void persistNew(String symbol, String interval, List<CandleEntity> candidates) {
        if (candidates.isEmpty()) {
            return;
        }
        Set<Long> existing = new HashSet<>(
                candleRepository.findOpenTimesBySymbolAndInterval(symbol, interval));
        List<CandleEntity> toSave = new ArrayList<>(candidates.size());
        for (CandleEntity c : candidates) {
            if (existing.add(c.getOpenTime())) {
                toSave.add(c);
            }
        }
        if (!toSave.isEmpty()) {
            candleRepository.saveAll(toSave);
        }
    }

    private boolean sleepBetweenRequests() {
        try {
            Thread.sleep(REQUEST_PAUSE_MS);
            return true;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.warn("Load interrupted");
            return false;
        }
    }
}
