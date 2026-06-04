# TradeLab

Historical-analog projection for crypto prices. Given the current market state
(RSI, distance from EMA, EMA slope, volatility, volume anomaly), the system
finds the **K nearest historical states** in the cached price history and
summarizes what happened next. The output is a confidence band — not a
prediction.

> Educational / portfolio project. Not financial advice and not a trading
> signal.

## Stack

- **Backend** — Java 21, Spring Boot 3.5, PostgreSQL, JPA. Pulls candles from
  Binance, caches them in the DB, computes indicators (SMA/EMA/RSI/ATR), runs
  k-NN analog search and percentile aggregation.
- **Frontend** — Next.js 15 + TypeScript + TailwindCSS. Dark trading theme.
  Streams live BTC price over Binance WebSocket (`@aggTrade`) and polls the
  backend every 5 seconds for the updated projection.
- **Engine (legacy)** — `engine/` contains an early C++ gRPC implementation of
  the same indicators and k-NN. It was used in an earlier iteration to learn
  C++/gRPC and was benchmarked against Java. Backend no longer depends on it.

## Architecture

```
                              ┌──────────────┐
                              │   Binance    │
                              │  REST + WS   │
                              └──────┬───────┘
                                     │
              ┌──────────────────────┼─────────────┐
              │                      │             │
              ▼                      ▼             ▼
   ┌────────────────────┐  ┌──────────────────┐  ┌─────────────┐
   │ Spring Boot backend│  │  Next.js front   │  │ PostgreSQL  │
   │  Indicators (Java) │  │  WebSocket live  │  │  candles    │
   │  k-NN analog k=20  │◄─┤  5s polling      │  │  cache      │
   │  REST API          │  │  Dark UI         │  │             │
   └─────────┬──────────┘  └──────────────────┘  └─────────────┘
             │
             ▼
   ┌────────────────────┐
   │  /api/candles      │
   │  /api/indicators/* │
   │  /api/analogs      │
   │  /api/history/load │
   └────────────────────┘
```

## Repo layout

```
tradelab/
├── backend/          Spring Boot service (Java 21, Maven)
├── frontend/         Next.js 15 dashboard (TypeScript, Tailwind)
├── engine/           Legacy C++ gRPC engine (kept as reference)
└── README.md
```

## How the analog search works

For every candle in history we build a normalized **5-feature vector**:

| Feature | What it is |
|---|---|
| `rsi / 100` | Wilder's RSI, scaled to [0, 1] |
| `(close - ema) / ema` | How far price is from its EMA |
| `(ema[i] - ema[i-5]) / ema[i-5]` | EMA slope over 5 candles |
| `atr / close` | Average True Range as fraction of price (volatility regime) |
| `volume / sma(volume) - 1` | Volume relative to its 20-candle average |

For the current candle's vector we compute Euclidean distance to every
historical vector, pick the **K closest** (default K=20), look at the price
change `horizon` candles later (default 12), and summarize:

- **median** — main projected change (used as the displayed projection)
- **p25 / p75** — half of outcomes fell in this band (used for the confidence
  band shown in the UI)
- **upPct** — share of analogs where price rose
- **mean** — average change (shown for reference)

The displayed projection is the **median**, not a point estimate. The band
between p25 and p75 shows where 50% of historical outcomes landed.

## API

| Endpoint | What it does |
|---|---|
| `GET /api/candles?symbol&interval&limit` | Cached candles, falls back to Binance |
| `GET /api/indicators/sma?…&period=14` | SMA in Java |
| `GET /api/indicators/ema?…&period=14` | EMA in Java |
| `GET /api/indicators/rsi?…&period=14` | RSI in Java |
| `GET /api/analogs?…&k=20&horizon=12` | k-NN analog search + projection stats |
| `POST /api/history/load?symbol&interval&count` | Bulk fetch from Binance via pagination |

## Running

**Prerequisites**: JDK 21, Node 20+, Docker (for Postgres).

```bash
# 1. Postgres
cd backend
docker compose up -d

# 2. Backend (port 8080)
./mvnw spring-boot:run

# 3. Frontend (port 3000)
cd ../frontend
npm install
npm run dev
```

Open http://localhost:3000.

### Loading historical data

The k-NN search needs a lot of candles to be useful. Once the backend is
running, prime the DB:

```bash
curl -X POST 'http://localhost:8080/api/history/load?symbol=BTCUSDT&interval=15m&count=50000'
```

This paginates Binance and stores ~50k 15m candles (~1.4 years of history).
Takes around 30 seconds with the built-in rate limiting (250ms between
requests).

## Notable engineering decisions

- **C++ engine was removed from the request path** after benchmarking. On
  100k vectors with 3 features, the gRPC transport overhead dominated the
  computation (Java ~12 ms vs engine ~30 ms). The C++ code is kept under
  `engine/` as an educational artifact.
- **No JS boxing in the engine path** (when it existed): `double[]` was used
  instead of `List<Double>` to avoid 300k `Double` allocations per request.
- **Auto-refresh of the latest candle**: `CandleService.refreshLatestIfStale`
  detects when the most-recent cached candle is older than its interval and
  pulls fresh data from Binance.
- **Live price via Binance WebSocket** (`@aggTrade`): direct browser-to-Binance
  stream, ~100 ms latency. Backend is not involved in the hot path of the
  ticker.
- **Honest projection display**: median + p25/p75 percentile band instead of a
  single point estimate. The UI also classifies the spread (`tight` /
  `moderate` / `wide` / `very wide, no signal`) so the user can read whether
  the historical sample is informative.
- **Persistence fixes**: `saveNew` rewritten from O(n²) to O(n) with a
  `HashSet<openTime>` dedup; `getCandles` now actually respects its `limit`
  parameter via `PageRequest`; the unique constraint typo
  (`open_timme` → `open_time`) was fixed.

## What's deliberately not in the model

- No volume-weighted price, order-book depth, or funding rates.
- No regime filter (analogs from 2017 are weighted the same as 2024).
- No time-of-day / day-of-week features.
- No magnitude scaling on `volumeAnomaly` — outliers can dominate distance.

These are easy targets if the projection starts feeling actionable enough to
care about its quality.

## Disclaimer

This project explores statistical similarity of historical market states for
educational purposes. It does not predict prices, does not constitute financial
advice, and should not be used to make trading decisions.
