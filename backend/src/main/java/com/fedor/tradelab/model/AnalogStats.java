package com.fedor.tradelab.model;

public record AnalogStats(
        int count,
        double upPct,
        double meanChangePct,
        double medianChangePct,
        double p25ChangePct,
        double p75ChangePct
) {}
