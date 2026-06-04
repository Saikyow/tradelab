package com.fedor.tradelab.model;

public record AnalogStats(
        int count,
        double upPct,
        double meanChangePct,
        double medianChangePct
) {}
