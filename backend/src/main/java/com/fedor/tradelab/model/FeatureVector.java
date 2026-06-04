package com.fedor.tradelab.model;

public record FeatureVector(
        double rsi,
        double priceVsEma,
        double emaSlope
) {}
