package com.fedor.tradelab.controller;

import com.fedor.tradelab.model.Candle;
import com.fedor.tradelab.service.CandleService;
import com.fedor.tradelab.service.IndicatorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/indicators")
public class IndicatorController {

    private final CandleService candleService;
    private final IndicatorService indicatorService;

    public IndicatorController(CandleService candleService, IndicatorService indicatorService) {
        this.candleService = candleService;
        this.indicatorService = indicatorService;
    }

    @GetMapping("/sma")
    public List<Double> sma(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "14") int period
    ) {
        return indicatorService.sma(loadCloses(symbol, interval, limit), period);
    }

    @GetMapping("/ema")
    public List<Double> ema(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "14") int period
    ) {
        return indicatorService.ema(loadCloses(symbol, interval, limit), period);
    }

    @GetMapping("/rsi")
    public List<Double> rsi(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "14") int period
    ) {
        return indicatorService.rsi(loadCloses(symbol, interval, limit), period);
    }

    private List<Double> loadCloses(String symbol, String interval, int limit) {
        List<Candle> candles = candleService.getCandles(symbol, interval, limit);
        return candles.stream().map(Candle::close).toList();
    }
}
