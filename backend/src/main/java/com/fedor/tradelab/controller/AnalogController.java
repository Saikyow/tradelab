package com.fedor.tradelab.controller;

import com.fedor.tradelab.model.AnalogResponse;
import com.fedor.tradelab.service.AnalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AnalogController {

    private final AnalogService analogService;

    public AnalogController(AnalogService analogService) {
        this.analogService = analogService;
    }

    @GetMapping("/api/analogs")
    public AnalogResponse analogs(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam(defaultValue = "1000") int limit,
            @RequestParam(defaultValue = "14") int rsiPeriod,
            @RequestParam(defaultValue = "14") int emaPeriod,
            @RequestParam(defaultValue = "20") int k,
            @RequestParam(defaultValue = "12") int horizon
    ) {
        return analogService.findAnalogs(symbol, interval, limit, rsiPeriod, emaPeriod, k, horizon);
    }
}
