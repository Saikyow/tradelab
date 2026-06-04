package com.fedor.tradelab.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class IndicatorService {

    public List<Double> sma(List<Double> values, int period) {
        validatePeriod(period);
        List<Double> result = new ArrayList<>(values.size());
        double windowSum = 0.0;

        for (int i = 0; i < values.size(); i++) {
            windowSum += values.get(i);
            if (i >= period) {
                windowSum -= values.get(i - period);
            }
            result.add(i >= period - 1 ? windowSum / period : null);
        }
        return result;
    }

    public List<Double> ema(List<Double> values, int period) {
        validatePeriod(period);
        int n = values.size();
        List<Double> result = new ArrayList<>(n);
        double k = 2.0 / (period + 1);
        double seedSum = 0.0;
        double emaPrev = 0.0;

        for (int i = 0; i < n; i++) {
            double v = values.get(i);
            if (i < period - 1) {
                seedSum += v;
                result.add(null);
            } else if (i == period - 1) {
                seedSum += v;
                emaPrev = seedSum / period;
                result.add(emaPrev);
            } else {
                emaPrev = v * k + emaPrev * (1.0 - k);
                result.add(emaPrev);
            }
        }
        return result;
    }

    public List<Double> rsi(List<Double> values, int period) {
        validatePeriod(period);
        int n = values.size();
        List<Double> result = new ArrayList<>(n);
        double avgGain = 0.0;
        double avgLoss = 0.0;

        for (int i = 0; i < n; i++) {
            if (i == 0) {
                result.add(null);
                continue;
            }
            double delta = values.get(i) - values.get(i - 1);
            double gain = delta > 0.0 ? delta : 0.0;
            double loss = delta < 0.0 ? -delta : 0.0;

            if (i < period) {
                avgGain += gain;
                avgLoss += loss;
                result.add(null);
            } else if (i == period) {
                avgGain = (avgGain + gain) / period;
                avgLoss = (avgLoss + loss) / period;
                result.add(computeRsi(avgGain, avgLoss));
            } else {
                avgGain = (avgGain * (period - 1) + gain) / period;
                avgLoss = (avgLoss * (period - 1) + loss) / period;
                result.add(computeRsi(avgGain, avgLoss));
            }
        }
        return result;
    }

    public List<Double> atr(List<Double> highs, List<Double> lows, List<Double> closes, int period) {
        validatePeriod(period);
        int n = closes.size();
        List<Double> result = new ArrayList<>(n);
        double seedSum = 0.0;
        double atrPrev = 0.0;

        for (int i = 0; i < n; i++) {
            if (i == 0) {
                result.add(null);
                continue;
            }
            double prevClose = closes.get(i - 1);
            double h = highs.get(i);
            double l = lows.get(i);
            double tr = Math.max(h - l, Math.max(Math.abs(h - prevClose), Math.abs(l - prevClose)));

            if (i < period) {
                seedSum += tr;
                result.add(null);
            } else if (i == period) {
                seedSum += tr;
                atrPrev = seedSum / period;
                result.add(atrPrev);
            } else {
                atrPrev = (atrPrev * (period - 1) + tr) / period;
                result.add(atrPrev);
            }
        }
        return result;
    }

    private double computeRsi(double avgGain, double avgLoss) {
        if (avgLoss == 0.0) {
            return 100.0;
        }
        double rs = avgGain / avgLoss;
        return 100.0 - 100.0 / (1.0 + rs);
    }

    private void validatePeriod(int period) {
        if (period <= 0) {
            throw new IllegalArgumentException("period must be greater than 0");
        }
    }
}
