package com.fedor.tradelab.model;

public record AnalogQuery(
        String symbol,
        String interval,
        int limit,
        int rsiPeriod,
        int emaPeriod,
        int k,
        int horizon
) {}
