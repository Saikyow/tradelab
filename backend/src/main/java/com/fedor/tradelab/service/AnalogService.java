package com.fedor.tradelab.service;

import com.fedor.tradelab.model.Analog;
import com.fedor.tradelab.model.AnalogQuery;
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
    private static final int VOLUME_LOOKBACK = 20;
    private static final String DISCLAIMER =
            "Historical statistics over similar market states, not a prediction. " +
            "Past behaviour does not guarantee future outcomes.";

    private final CandleService candleService;
    private final IndicatorService indicatorService;

    public AnalogService(CandleService candleService, IndicatorService indicatorService) {
        this.candleService = candleService;
        this.indicatorService = indicatorService;
    }

    public AnalogResponse findAnalogs(AnalogQuery query) {
        List<Candle> candles = candleService.getCandles(query.symbol(), query.interval(), query.limit());
        int n = candles.size();
        if (n == 0) {
            throw new IllegalStateException(
                    "No candles available for " + query.symbol() + "/" + query.interval());
        }

        List<Double> closes = candles.stream().map(Candle::close).toList();
        List<Double> highs = candles.stream().map(Candle::high).toList();
        List<Double> lows = candles.stream().map(Candle::low).toList();
        List<Double> volumes = candles.stream().map(Candle::volume).toList();

        List<Double> rsi = indicatorService.rsi(closes, query.rsiPeriod());
        List<Double> ema = indicatorService.ema(closes, query.emaPeriod());
        List<Double> atr = indicatorService.atr(highs, lows, closes, query.rsiPeriod());
        List<Double> volMa = indicatorService.sma(volumes, VOLUME_LOOKBACK);

        FeatureVector[] vectors = buildVectors(candles, closes, rsi, ema, atr, volMa, n);
        int currentIdx = findCurrentIndex(vectors);
        FeatureVector current = vectors[currentIdx];

        List<EligibleCandidate> eligible = collectEligible(vectors, n, query.horizon(), currentIdx);
        List<Match> matches = findTopK(eligible, current, query.k());
        AnalogsAndChanges built = buildAnalogsAndChanges(matches, candles, closes, query.horizon());

        return new AnalogResponse(
                currentIdx,
                candles.get(currentIdx).openTime(),
                current,
                built.analogs(),
                aggregate(built.changePcts()),
                DISCLAIMER
        );
    }

    private FeatureVector[] buildVectors(List<Candle> candles,
                                         List<Double> closes,
                                         List<Double> rsi,
                                         List<Double> ema,
                                         List<Double> atr,
                                         List<Double> volMa,
                                         int n) {
        FeatureVector[] vectors = new FeatureVector[n];
        for (int i = 0; i < n; i++) {
            Double r = rsi.get(i);
            Double e = ema.get(i);
            Double ePrev = (i >= SLOPE_LOOKBACK) ? ema.get(i - SLOPE_LOOKBACK) : null;
            Double a = atr.get(i);
            Double vMa = volMa.get(i);
            double close = closes.get(i);
            double volume = candles.get(i).volume();

            if (r == null || e == null || ePrev == null || a == null || vMa == null) continue;
            if (ePrev == 0.0 || close == 0.0 || vMa == 0.0) continue;

            double f0 = r / 100.0;
            double f1 = (close - e) / e;
            double f2 = (e - ePrev) / ePrev;
            double f3 = a / close;
            double f4 = volume / vMa - 1.0;
            vectors[i] = new FeatureVector(f0, f1, f2, f3, f4);
        }
        return vectors;
    }

    private int findCurrentIndex(FeatureVector[] vectors) {
        for (int i = vectors.length - 1; i >= 0; i--) {
            if (vectors[i] != null) {
                return i;
            }
        }
        throw new IllegalStateException(
                "Not enough data to build the current feature vector (increase limit or shrink periods)");
    }

    private List<EligibleCandidate> collectEligible(FeatureVector[] vectors, int n, int horizon, int currentIdx) {
        int maxCandidateIdx = n - 1 - horizon;
        List<EligibleCandidate> eligible = new ArrayList<>();
        for (int i = 0; i <= maxCandidateIdx; i++) {
            if (vectors[i] == null || i == currentIdx) {
                continue;
            }
            eligible.add(new EligibleCandidate(i, vectors[i]));
        }
        return eligible;
    }

    private List<Match> findTopK(List<EligibleCandidate> eligible, FeatureVector current, int k) {
        List<Match> all = new ArrayList<>(eligible.size());
        for (EligibleCandidate e : eligible) {
            all.add(new Match(e.originalIndex(), distance(current, e.vector())));
        }
        all.sort(Comparator.comparingDouble(Match::distance));
        return List.copyOf(all.subList(0, Math.min(k, all.size())));
    }

    private AnalogsAndChanges buildAnalogsAndChanges(List<Match> matches,
                                                    List<Candle> candles,
                                                    List<Double> closes,
                                                    int horizon) {
        List<Analog> analogs = new ArrayList<>(matches.size());
        List<Double> changePcts = new ArrayList<>(matches.size());
        for (Match m : matches) {
            double closeNow = closes.get(m.index());
            double closeLater = closes.get(m.index() + horizon);
            double changePct = (closeLater - closeNow) / closeNow * 100.0;
            analogs.add(new Analog(m.index(), candles.get(m.index()).openTime(), m.distance(), changePct));
            changePcts.add(changePct);
        }
        return new AnalogsAndChanges(analogs, changePcts);
    }

    private double distance(FeatureVector a, FeatureVector b) {
        double dr = a.rsi() - b.rsi();
        double dp = a.priceVsEma() - b.priceVsEma();
        double ds = a.emaSlope() - b.emaSlope();
        double dv = a.volatility() - b.volatility();
        double dvol = a.volumeAnomaly() - b.volumeAnomaly();
        return Math.sqrt(dr * dr + dp * dp + ds * ds + dv * dv + dvol * dvol);
    }

    private AnalogStats aggregate(List<Double> changes) {
        int count = changes.size();
        if (count == 0) {
            return new AnalogStats(0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }
        long upCount = changes.stream().filter(c -> c > 0).count();
        double upPct = upCount * 100.0 / count;
        double mean = changes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        List<Double> sorted = changes.stream().sorted().toList();
        double median = percentile(sorted, 0.5);
        double p25 = percentile(sorted, 0.25);
        double p75 = percentile(sorted, 0.75);

        return new AnalogStats(count, upPct, mean, median, p25, p75);
    }

    private double percentile(List<Double> sorted, double p) {
        if (sorted.isEmpty()) return 0.0;
        if (sorted.size() == 1) return sorted.get(0);
        double idx = p * (sorted.size() - 1);
        int low = (int) Math.floor(idx);
        int high = (int) Math.ceil(idx);
        if (low == high) return sorted.get(low);
        double frac = idx - low;
        return sorted.get(low) * (1.0 - frac) + sorted.get(high) * frac;
    }

    private record EligibleCandidate(int originalIndex, FeatureVector vector) {}
    private record Match(int index, double distance) {}
    private record AnalogsAndChanges(List<Analog> analogs, List<Double> changePcts) {}
}
