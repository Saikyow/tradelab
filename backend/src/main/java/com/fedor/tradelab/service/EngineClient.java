package com.fedor.tradelab.service;

import indicator.IndicatorServiceGrpc;
import indicator.Indicator.SmaRequest;
import indicator.Indicator.SmaResponse;
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

    @PreDestroy
    public void shutdown() {
        if (channel != null) {
            channel.shutdown();
        }
    }
}