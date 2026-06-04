package com.fedor.tradelab.model;

import jakarta.persistence.*;

@Entity
@Table(
        name = "candles",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"symbol", "interval_name", "open_time"}
        )
)
public class CandleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String symbol;

    @Column(name = "interval_name", nullable = false)
    private String interval;

    @Column(name = "open_time", nullable = false)
    private long openTime;

    private double open;
    private double high;
    private double low;
    private double close;
    private double volume;

    protected  CandleEntity() {}

    public CandleEntity(String symbol, String interval, long openTime,
                        double open, double high, double low,
                        double close, double volume) {
        this.symbol = symbol;
        this.interval = interval;
        this.openTime = openTime;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }

    public Long getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getInterval() {
        return interval;
    }

    public long getOpenTime() {
        return openTime;
    }

    public double getOpen() {
        return open;
    }

    public double getHigh() {
        return high;
    }

    public double getLow() {
        return low;
    }

    public double getClose() {
        return close;
    }

    public double getVolume() {
        return volume;
    }
}
