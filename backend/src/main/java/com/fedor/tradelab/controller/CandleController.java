package com.fedor.tradelab.controller;

import com.fedor.tradelab.service.BinanceService;
import com.fedor.tradelab.model.Candle;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CandleController {

    private final BinanceService binanceService;

    public CandleController(BinanceService binanceService) {
        this.binanceService = binanceService;
    }

    @GetMapping("/api/candles")
    public List<Candle> candles(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return binanceService.getCandles(symbol, interval, limit);
    }
}
