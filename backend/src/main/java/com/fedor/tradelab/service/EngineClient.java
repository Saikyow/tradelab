package com.fedor.tradelab.service;

import indicator.IndicatorServiceGrpc;
import indicator.Indicator.SmaRequest;
import indicator.Indicator.SmaResponse;
import indicator.Indicator.EmaRequest;
import indicator.Indicator.EmaResponse;
import indicator.Indicator.RsiRequest;
import indicator.Indicator.RsiResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EngineClient {

    private ManagedChannel channel;
    private IndicatorServiceGrpc.IndicatorServiceBlockingStub stub;

    @PostConstruct
    public void init() {
        channel = ManagedChannelBuilder
                .forAddress("localhost", 50051)
                .usePlaintext()
                .build();
        stub = IndicatorServiceGrpc.newBlockingStub(channel);
    }

    public List<Double> computeSma(List<Double> values, int period) {
        SmaRequest request = SmaRequest.newBuilder()
                .addAllValues(values)
                .setPeriod(period)
                .build();

        SmaResponse response = stub.computeSma(request);

        List<Double> result = new ArrayList<>();
        for (double v : response.getResultList()) {
            result.add(Double.isNaN(v) ? null : v);
        }
        return result;
    }

    public List<Double> computeEma(List<Double> values, int period) {
        EmaRequest request = EmaRequest.newBuilder()
                .addAllValues(values)
                .setPeriod(period)
                .build();

        EmaResponse response = stub.computeEma(request);

        List<Double> result = new ArrayList<>();
        for (double v : response.getResultList()) {
            result.add(Double.isNaN(v) ? null : v);
        }
        return result;
    }

    public List<Double> computeRsi(List<Double> values, int period) {
        RsiRequest request = RsiRequest.newBuilder()
                .addAllValues(values)
                .setPeriod(period)
                .build();

        RsiResponse response = stub.computeRsi(request);

        List<Double> result = new ArrayList<>();
        for (double v : response.getResultList()) {
            result.add(Double.isNaN(v) ? null : v);
        }
        return result;
    }

    @PreDestroy
    public void shutdown() {
        if (channel != null) {
            channel.shutdown();
        }
    }
}