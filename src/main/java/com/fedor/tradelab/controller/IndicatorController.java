package com.fedor.tradelab.controller;

import com.fedor.tradelab.model.Candle;
import com.fedor.tradelab.service.BinanceService;
import com.fedor.tradelab.service.IndicatorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class IndicatorController {

    private final BinanceService binanceService;
    private final IndicatorService indicatorService;

    public IndicatorController(BinanceService binanceService, IndicatorService indicatorService) {
        this.binanceService = binanceService;
        this.indicatorService = indicatorService;
    }

    @GetMapping("/api/indicators/sma")
    public List<Double> sma(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "14") int period
    ) {
      List<Candle> candles = binanceService.getCandles(symbol, interval, limit);
      List<Double> closes = candles.stream()
              .map(Candle::close)
              .toList();
      return indicatorService.sma(closes, period);
    }
}
