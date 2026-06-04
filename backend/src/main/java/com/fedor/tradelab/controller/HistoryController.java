package com.fedor.tradelab.controller;

import com.fedor.tradelab.model.HistoryLoadResponse;
import com.fedor.tradelab.service.CandleService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private final CandleService candleService;

    public HistoryController(CandleService candleService) {
        this.candleService = candleService;
    }

    @PostMapping("/load")
    public HistoryLoadResponse load(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam int count
    ) {
        long total = candleService.loadHistory(symbol, interval, count);
        return new HistoryLoadResponse(symbol, interval, count, total);
    }
}
