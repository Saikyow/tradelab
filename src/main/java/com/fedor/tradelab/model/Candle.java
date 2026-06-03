package com.fedor.tradelab.model;

public record Candle(
        long openTime,
        double open,
        double high,
        double low,
        double close,
        double volume
) {}
