package com.fedor.tradelab.controller;

import com.fedor.tradelab.model.Candle;
import com.fedor.tradelab.service.BinanceService;
import com.fedor.tradelab.service.EngineClient;
import com.fedor.tradelab.service.IndicatorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class IndicatorController {

    private final BinanceService binanceService;
    private final IndicatorService indicatorService;
    private final EngineClient engineClient;

    public IndicatorController(BinanceService binanceService,
                               IndicatorService indicatorService,
                               EngineClient engineClient) {
        this.binanceService = binanceService;
        this.indicatorService = indicatorService;
        this.engineClient = engineClient;
    }

    @GetMapping("/api/indicators/sma")
    public List<Double> sma(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "14") int period
    ) {
//      List<Double> closes = loadCloses(symbol, interval, limit);
//
//      return indicatorService.sma(closes, period);
        List<Double> closes = loadCloses(symbol, interval, limit);

        long start = System.nanoTime();
        List<Double> result = indicatorService.sma(closes, period);
        long end = System.nanoTime();

        System.out.println("Java SMA time ms: " + (end - start) / 1_000_000.0);

        return result;
    }

    @GetMapping("/api/indicators/sma-engine")
    public List<Double> smaEngine(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "14") int period
    ){
//        List<Double> closes = loadCloses(symbol, interval, limit);
//        return engineClient.computeSma(closes, period);

        List<Double> closes = loadCloses(symbol, interval, limit);

        long start = System.nanoTime();
        List<Double> result = engineClient.computeSma(closes, period);
        long end = System.nanoTime();

        System.out.println("C++ engine SMA time ms: " + (end - start) / 1_000_000.0);

        return result;

    }

    @GetMapping("/api/indicators/ema-engine")
    public List<Double> emaEngine(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "14") int period
    ){
        List<Double> closes = loadCloses(symbol, interval, limit);

        long start = System.nanoTime();
        List<Double> result = engineClient.computeEma(closes, period);
        long end = System.nanoTime();

        System.out.println("C++ engine EMA time ms: " + (end - start) / 1_000_000.0);

        return result;
    }

    @GetMapping("/api/indicators/rsi-engine")
    public List<Double> rsiEngine(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "14") int period
    ){
        List<Double> closes = loadCloses(symbol, interval, limit);

        long start = System.nanoTime();
        List<Double> result = engineClient.computeRsi(closes, period);
        long end = System.nanoTime();

        System.out.println("C++ engine RSI time ms: " + (end - start) / 1_000_000.0);

        return result;
    }

    public List<Double> loadCloses(String symbol, String interval, int period){
        List<Candle> candles = binanceService.getCandles(symbol, interval, period);
        return candles.stream().map(Candle::close).toList();
    }

}
