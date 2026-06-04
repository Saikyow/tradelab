package com.fedor.tradelab.model;

public record Analog(
        int index,
        long openTime,
        double distance,
        double changePct
) {}
