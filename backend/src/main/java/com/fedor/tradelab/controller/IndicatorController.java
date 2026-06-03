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
      List<Double> closes = loadCloses(symbol, interval, limit);
      return indicatorService.sma(closes, period);
    }

    @GetMapping("/api/indicators/sma-engine")
    public List<Double> smaEngine(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "14") int period
    ){
        List<Double> closes = loadCloses(symbol, interval, limit);
        return engineClient.computeSma(closes, period);

    }

    public List<Double> loadCloses(String symbol, String interval, int period){
        List<Candle> candles = binanceService.getCandles(symbol, interval, period);
        return candles.stream().map(Candle::close).toList();
    }

}
