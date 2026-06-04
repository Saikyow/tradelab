package com.fedor.tradelab.model;

public record HistoryLoadResponse(
        String symbol,
        String interval,
        int requested,
        long totalInDb
) {}
