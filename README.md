# TradeLab

Fintech platform for market data analysis. It fetches exchange candles, computes
technical indicators, and (on later stages) searches for historical analogs of the
current market state to estimate likely outcomes, then raises alerts.

The numerical work runs in a C++ engine, while Java/Spring handles the
orchestration: REST API, database, caching, and the connection to the engine.
The two communicate over gRPC.

> Status: work in progress. This is a learning/portfolio project, not financial
> advice or a trading signal.

## Architecture

```
Client  --REST-->  backend (Java / Spring Boot)  --gRPC-->  engine (C++)
                          |
                          v
                     PostgreSQL                 Binance (candles)
```

- backend - Spring Boot service: REST API, persistence, caching, orchestration,
  and a gRPC client to the engine.
- engine - C++ gRPC server: the fast numerical core (indicators; later the
  historical-analog search).
- The contract between them is defined once in a `.proto` file and used to
  generate code for both languages.

## Repository layout

```
tradelab/
|-- backend/          Spring Boot application (Java 21, Maven)
|   `-- src/main/proto/indicator.proto    gRPC contract (Java side)
|-- engine/           C++ gRPC server (CMake)
|   `-- proto/indicator.proto             gRPC contract (C++ side)
`-- README.md
```

## Tech stack

Backend:
- Java 21, Spring Boot 3.5 (Spring Web, Spring Data JPA, Hibernate)
- PostgreSQL 16 in Docker (Docker Compose)
- Binance REST API integration with candle caching in the database
- gRPC client (grpc-java) calling the C++ engine

Engine:
- C++17, gRPC, Protobuf
- CMake build

Tooling:
- JUnit 5 for unit tests
- Protobuf / gRPC for the cross-language contract

## How it fits together

1. A client calls a REST endpoint on the backend (e.g. request an SMA).
2. The backend loads candles (from PostgreSQL, or from Binance on a cache miss).
3. The backend sends the close prices and the period to the engine over gRPC.
4. The C++ engine computes the indicator and returns the result.
5. The backend maps the result back to JSON and responds to the client.

The same SMA is also implemented in Java and covered by unit tests, so the engine
output can be checked against a known-correct reference.

## Running it

Prerequisites: JDK 21, Docker, Maven, and a C++ toolchain with CMake, Protobuf and
gRPC (on macOS: `brew install cmake protobuf grpc`).

1) Start PostgreSQL:

```bash
cd backend
docker compose up -d
```

2) Build and start the C++ engine (listens on port 50051):

```bash
cd engine
cmake -S . -B build
cmake --build build
./build/server
```

3) Start the backend:

```bash
cd backend
./mvnw spring-boot:run
```

Run the backend tests:

```bash
cd backend
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

Simple Moving Average - computed in Java (reference implementation):

```
GET /api/indicators/sma?symbol=BTCUSDT&interval=1h&limit=100&period=14
```

Simple Moving Average - computed in the C++ engine over gRPC:

```
GET /api/indicators/sma-engine?symbol=BTCUSDT&interval=1h&limit=100&period=14
```

Both SMA endpoints return an array the same length as the input; the first
`period - 1` values are `null` because there is not yet enough data to fill the
window. On the engine side these gaps are sent as NaN and converted back to
`null` on the backend.

## Roadmap

1. Indicators API - candles from the exchange, indicator calculation, DB caching,
   tests, and a C++ engine reached over gRPC. *(current)*
2. Historical-analog engine - describe each moment by indicator features, find
   similar past states, and summarize what price did next. The heavy search runs
   in the C++ engine.
3. Realtime + alerts - stream quotes over WebSocket and alert when the current
   state matches a historically notable pattern.

## Notes

Historical similarity does not guarantee future behavior - markets change regimes,
and past patterns are not predictions. The analog search is intended as an
analytical/research tool, not a trading signal.
