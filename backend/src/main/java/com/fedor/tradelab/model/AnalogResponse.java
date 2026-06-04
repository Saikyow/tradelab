package com.fedor.tradelab.model;

import java.util.List;

public record AnalogResponse(
        int currentIndex,
        long currentOpenTime,
        FeatureVector currentVector,
        List<Analog> analogs,
        AnalogStats stats,
        String disclaimer
) {}
