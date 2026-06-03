package com.fedor.tradelab.service;

import com.fedor.tradelab.model.Candle;
import com.fedor.tradelab.model.CandleEntity;
import com.fedor.tradelab.repository.CandleRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Service
public class BinanceService {

    private final RestClient restClient = RestClient.create("https://api.binance.com");
    private final CandleRepository candleRepository;

    public BinanceService(CandleRepository candleRepository) {
        this.candleRepository = candleRepository;
    }

    public List<Candle> getCandles(String symbol, String interval, int limit){

        long cached = candleRepository.countBySymbolAndInterval(symbol, interval);

        if (cached < limit){
            List<CandleEntity> fresh = fetchFromBinance(symbol, interval, limit);
            saveNew(fresh);
        }

        List<CandleEntity> entities =
                candleRepository.findBySymbolAndIntervalOrderByOpenTimeAsc(symbol, interval);

        List<Candle> result = new ArrayList<>();
        for (CandleEntity e : entities){
            result.add(new Candle(
                    e.getOpenTime(), e.getOpen(), e.getHigh(),
                    e.getLow(), e.getClose(), e.getVolume()
            ));
        }
        return result;

    }

    public List<CandleEntity> fetchFromBinance(String symbol, String interval, int limit) {
        // Binance отдает массив массивов List<List<Object>>
        List<List<Object>> raw = restClient.get()
                .uri("/api/v3/klines?symbol={s}&interval={i}&limit={l}", symbol, interval, limit)
                .retrieve()
                .body(List.class);

        List<CandleEntity> candles = new ArrayList<>();
        if (raw == null) return candles;

        for (List<Object> k: raw){
            candles.add(new CandleEntity(
                    symbol,
                    interval,
                    ((Number) k.get(0)).longValue(),
                    Double.parseDouble((String) k.get(1)),
                    Double.parseDouble((String) k.get(2)),
                    Double.parseDouble((String) k.get(3)),
                    Double.parseDouble((String) k.get(4)),
                    Double.parseDouble((String) k.get(5))
            ));
        }
        return candles;

    }

    public void saveNew(List<CandleEntity> candles){
        for (CandleEntity c : candles){

            boolean exists = candleRepository
                    .findBySymbolAndIntervalOrderByOpenTimeAsc(c.getSymbol(), c.getInterval())
                    .stream()
                    .anyMatch(e -> e.getOpenTime() == c.getOpenTime());

            if (!exists){
                candleRepository.save(c);
            }
        }

    }

}

