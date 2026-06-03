# TradeLab

Fintech platform for market data analysis. It fetches exchange candles, computes
technical indicators, and (on later stages) searches for historical analogs of the
current market state to estimate likely outcomes, then raises alerts.

Heavy numerical work is designed to run in a C++ engine, while Java/Spring handles
the orchestration: REST API, database, caching, and the connection to the engine.

> Status: work in progress. This is a learning/portfolio project, not financial
> advice or a trading signal.

## Architecture

```
Client  ──REST──>  Java / Spring Boot  ──gRPC──>  C++ engine
                          │
                          ▼
                     PostgreSQL                Binance (candles)
```

- Java / Spring Boot — API, persistence, caching, orchestration.
- C++ engine — fast numerical core (indicators, historical-analog search). *Planned.*
- gRPC — language-agnostic contract between Java and C++. *Planned.*
- PostgreSQL — stores candles and computed data.
- Binance public API — source of historical candles (free, no key required).
## Tech stack

Implemented:
- Java 21, Spring Boot 3.5 (Spring Web, Spring Data JPA, Hibernate)
- PostgreSQL 16 running in Docker (Docker Compose)
- Binance REST API integration with candle caching in the database
- Simple Moving Average (SMA) indicator
- Unit tests with JUnit 5
  Planned:
- C++ numerical engine (indicators: EMA, RSI; historical-analog search)
- gRPC + Protobuf contract between Java and C++
- Redis caching, WebSocket for realtime quotes, alerting
- CI with GitHub Actions
## Roadmap

1. Indicators API — candles from the exchange, indicator calculation, DB caching, tests. *(current)*
2. Backtesting / historical-analog engine — describe each moment by indicator
   features, find similar past states, summarize what price did next.
3. Realtime + alerts — stream quotes over WebSocket, alert when the current state
   matches a historically notable pattern.
## Getting started

Prerequisites: JDK 21, Docker, Maven (the bundled `mvnw` wrapper works too).

Start PostgreSQL:

```bash
docker compose up -d
```

Run the application:

```bash
./mvnw spring-boot:run
```

Run the tests:

```bash
./mvnw test
```

## API

Health check:

```
GET /api/ping
```

Candles (cached in the database after first fetch):

```
GET /api/candles?symbol=BTCUSDT&interval=1h&limit=100
```

Simple Moving Average over close prices:

```
GET /api/indicators/sma?symbol=BTCUSDT&interval=1h&limit=100&period=14
```

The SMA response is the same length as the input; the first `period - 1` values
are `null` because there is not yet enough data to fill the window.

## Notes

Historical similarity does not guarantee future behavior — markets change regimes,
and past patterns are not predictions. The analog search is intended as an
analytical/research tool, not a trading signal.