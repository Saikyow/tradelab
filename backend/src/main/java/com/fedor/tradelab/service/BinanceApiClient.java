package com.fedor.tradelab.service;

import com.fedor.tradelab.model.CandleEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Component
public class BinanceApiClient {

    public static final int MAX_LIMIT = 1000;

    private final RestClient restClient;

    public BinanceApiClient(@Value("${binance.api.url}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    public List<CandleEntity> fetchKlines(String symbol, String interval, int limit) {
        return fetchKlines(symbol, interval, limit, null);
    }

    public List<CandleEntity> fetchKlines(String symbol, String interval, int limit, Long endTime) {
        @SuppressWarnings("unchecked")
        List<List<Object>> raw = restClient.get()
                .uri(uriBuilder -> {
                    var b = uriBuilder
                            .path("/api/v3/klines")
                            .queryParam("symbol", symbol)
                            .queryParam("interval", interval)
                            .queryParam("limit", limit);
                    if (endTime != null) {
                        b.queryParam("endTime", endTime);
                    }
                    return b.build();
                })
                .retrieve()
                .body(List.class);

        if (raw == null) {
            return List.of();
        }

        List<CandleEntity> candles = new ArrayList<>(raw.size());
        for (List<Object> k : raw) {
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
}
