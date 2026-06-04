package com.fedor.tradelab.service;

import com.fedor.tradelab.model.Analog;
import com.fedor.tradelab.model.AnalogResponse;
import com.fedor.tradelab.model.AnalogStats;
import com.fedor.tradelab.model.Candle;
import com.fedor.tradelab.model.FeatureVector;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class AnalogService {

    private static final int SLOPE_LOOKBACK = 5;
    private static final String DISCLAIMER =
            "Это историческая статистика по похожим состояниям рынка, не предсказание. " +
            "Совпадение в прошлом не гарантирует поведение в будущем.";

    private final BinanceService binanceService;
    private final EngineClient engineClient;

    public AnalogService(BinanceService binanceService, EngineClient engineClient) {
        this.binanceService = binanceService;
        this.engineClient = engineClient;
    }

    public AnalogResponse findAnalogs(String symbol,
                                      String interval,
                                      int limit,
                                      int rsiPeriod,
                                      int emaPeriod,
                                      int k,
                                      int horizon) {
        List<Candle> candles = binanceService.getCandles(symbol, interval, limit);
        int n = candles.size();
        if (n == 0) {
            throw new IllegalStateException("No candles returned for " + symbol + "/" + interval);
        }

        List<Double> closes = candles.stream().map(Candle::close).toList();

        List<Double> rsi = engineClient.computeRsi(closes, rsiPeriod);
        List<Double> ema = engineClient.computeEma(closes, emaPeriod);

        FeatureVector[] vectors = new FeatureVector[n];
        for (int i = 0; i < n; i++) {
            Double r = rsi.get(i);
            Double e = ema.get(i);
            Double ePrev = (i >= SLOPE_LOOKBACK) ? ema.get(i - SLOPE_LOOKBACK) : null;
            if (r == null || e == null || ePrev == null || ePrev == 0.0) {
                continue;
            }
            double f0 = r / 100.0;
            double f1 = (closes.get(i) - e) / e;
            double f2 = (e - ePrev) / ePrev;
            vectors[i] = new FeatureVector(f0, f1, f2);
        }

        int currentIdx = -1;
        for (int i = n - 1; i >= 0; i--) {
            if (vectors[i] != null) {
                currentIdx = i;
                break;
            }
        }
        if (currentIdx < 0) {
            throw new IllegalStateException(
                    "Не хватает данных для построения текущего вектора (увеличь limit или уменьши периоды)");
        }
        FeatureVector current = vectors[currentIdx];

        int maxCandidateIdx = n - 1 - horizon;
        List<Candidate> candidates = new ArrayList<>();
        for (int i = 0; i <= maxCandidateIdx; i++) {
            if (vectors[i] == null || i == currentIdx) {
                continue;
            }
            candidates.add(new Candidate(i, distance(current, vectors[i])));
        }

        candidates.sort(Comparator.comparingDouble(Candidate::distance));
        int take = Math.min(k, candidates.size());

        List<Analog> analogs = new ArrayList<>(take);
        List<Double> changePcts = new ArrayList<>(take);
        for (int j = 0; j < take; j++) {
            Candidate c = candidates.get(j);
            double closeNow = closes.get(c.index());
            double closeLater = closes.get(c.index() + horizon);
            double changePct = (closeLater - closeNow) / closeNow * 100.0;
            analogs.add(new Analog(c.index(), candles.get(c.index()).openTime(), c.distance(), changePct));
            changePcts.add(changePct);
        }

        AnalogStats stats = aggregate(changePcts);

        return new AnalogResponse(
                currentIdx,
                candles.get(currentIdx).openTime(),
                current,
                analogs,
                stats,
                DISCLAIMER
        );
    }

    private double distance(FeatureVector a, FeatureVector b) {
        double dx = a.rsi() - b.rsi();
        double dy = a.priceVsEma() - b.priceVsEma();
        double dz = a.emaSlope() - b.emaSlope();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private AnalogStats aggregate(List<Double> changes) {
        int count = changes.size();
        if (count == 0) {
            return new AnalogStats(0, 0.0, 0.0, 0.0);
        }
        long upCount = changes.stream().filter(c -> c > 0).count();
        double upPct = upCount * 100.0 / count;
        double mean = changes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        List<Double> sorted = changes.stream().sorted().toList();
        double median = (count % 2 == 1)
                ? sorted.get(count / 2)
                : (sorted.get(count / 2 - 1) + sorted.get(count / 2)) / 2.0;

        return new AnalogStats(count, upPct, mean, median);
    }

    private record Candidate(int index, double distance) {}
}
