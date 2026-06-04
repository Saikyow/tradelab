package com.fedor.tradelab.controller;

import com.fedor.tradelab.model.AnalogQuery;
import com.fedor.tradelab.model.AnalogResponse;
import com.fedor.tradelab.service.AnalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analogs")
public class AnalogController {

    private final AnalogService analogService;

    public AnalogController(AnalogService analogService) {
        this.analogService = analogService;
    }

    @GetMapping
    public AnalogResponse analogs(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam(defaultValue = "1000") int limit,
            @RequestParam(defaultValue = "14") int rsiPeriod,
            @RequestParam(defaultValue = "14") int emaPeriod,
            @RequestParam(defaultValue = "20") int k,
            @RequestParam(defaultValue = "12") int horizon
    ) {
        return analogService.findAnalogs(
                new AnalogQuery(symbol, interval, limit, rsiPeriod, emaPeriod, k, horizon));
    }
}
