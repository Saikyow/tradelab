"use client";

import type { AnalogResponse } from "@/lib/types";

type Props = {
  data: AnalogResponse | null;
  symbol: string;
  interval: string;
  horizon: number;
  currentPrice: number | null;
};

const INTERVAL_MINUTES: Record<string, number> = {
  "1m": 1,
  "3m": 3,
  "5m": 5,
  "15m": 15,
  "30m": 30,
  "1h": 60,
  "4h": 240,
  "1d": 1440,
};

export function ProjectionPanel({ data, symbol, interval, horizon, currentPrice }: Props) {
  if (!data) {
    return (
      <div className="bg-panel border border-border rounded-2xl p-10 min-h-[400px] flex items-center justify-center text-muted">
        loading...
      </div>
    );
  }

  const { stats, currentVector, analogs, disclaimer } = data;
  const bearish = stats.medianChangePct < 0;
  const directionColor = bearish ? "#ef5350" : "#26a69a";
  const arrow = bearish ? "↓" : "↑";

  const priceFromPct = (pct: number) =>
    currentPrice !== null ? currentPrice * (1 + pct / 100) : null;

  const medianPrice = priceFromPct(stats.medianChangePct);
  const p25Price = priceFromPct(stats.p25ChangePct);
  const p75Price = priceFromPct(stats.p75ChangePct);

  const horizonLabel = formatHorizon(horizon, interval);
  const spread = stats.p75ChangePct - stats.p25ChangePct;

  return (
    <div className="bg-panel border border-border rounded-2xl p-8 font-mono flex flex-col gap-8">
      <section className="text-center">
        <div className="text-muted text-xs uppercase tracking-widest mb-2">{symbol} · {interval}</div>
        <div className="text-6xl font-bold tabular-nums text-text">
          {currentPrice !== null ? formatPrice(currentPrice) : "—"}
        </div>
        <div className="text-muted text-[11px] mt-2">live spot price</div>
      </section>

      <div className="border-t border-border" />

      <section className="text-center">
        <div className="text-muted text-xs uppercase tracking-widest mb-3">
          projection · {horizonLabel}
        </div>
        <div className="flex flex-col items-center gap-2">
          <div style={{ color: directionColor }} className="text-7xl font-bold leading-none">
            {arrow}
          </div>
          <div className="text-4xl font-bold tabular-nums" style={{ color: directionColor }}>
            {medianPrice !== null ? formatPrice(medianPrice) : "—"}
          </div>
          <div className="text-text text-sm">
            median {stats.medianChangePct >= 0 ? "+" : ""}
            {stats.medianChangePct.toFixed(2)}%
          </div>
        </div>
      </section>

      <section>
        <div className="text-muted text-[10px] mb-3 uppercase tracking-widest text-center">
          50% confidence band (p25 — p75)
        </div>
        <div className="flex items-baseline justify-between text-sm mb-2 tabular-nums">
          <div className="text-down">
            <div className="text-[10px] text-muted uppercase">p25</div>
            <div className="font-bold">{p25Price !== null ? formatPrice(p25Price) : "—"}</div>
            <div className="text-[10px] text-muted">{stats.p25ChangePct.toFixed(2)}%</div>
          </div>
          <div className="text-up text-right">
            <div className="text-[10px] text-muted uppercase">p75</div>
            <div className="font-bold">{p75Price !== null ? formatPrice(p75Price) : "—"}</div>
            <div className="text-[10px] text-muted">+{stats.p75ChangePct.toFixed(2)}%</div>
          </div>
        </div>
        <ConfidenceBar p25={stats.p25ChangePct} p75={stats.p75ChangePct} median={stats.medianChangePct} />
        <div className="text-[10px] text-muted text-center mt-2">
          spread {spread.toFixed(2)}% — {classifySpread(spread)}
        </div>
      </section>

      <section className="grid grid-cols-3 gap-3">
        <Cell label="up rate" value={`${stats.upPct.toFixed(0)}%`} color={bearish ? "#ef5350" : "#26a69a"} />
        <Cell
          label="mean"
          value={`${stats.meanChangePct >= 0 ? "+" : ""}${stats.meanChangePct.toFixed(2)}%`}
        />
        <Cell label="analogs" value={String(stats.count)} />
      </section>

      <section>
        <div className="text-muted text-[10px] mb-2 uppercase tracking-widest">current state vector</div>
        <div className="grid grid-cols-5 gap-2">
          <Cell label="rsi" value={(currentVector.rsi * 100).toFixed(0)} small />
          <Cell label="vs ema" value={`${(currentVector.priceVsEma * 100).toFixed(2)}%`} small />
          <Cell label="slope" value={`${(currentVector.emaSlope * 100).toFixed(2)}%`} small />
          <Cell label="vol" value={`${(currentVector.volatility * 100).toFixed(2)}%`} small />
          <Cell label="vol-z" value={`${currentVector.volumeAnomaly >= 0 ? "+" : ""}${currentVector.volumeAnomaly.toFixed(2)}`} small />
        </div>
      </section>

      <div className="border-t border-border pt-4 text-[10px] text-muted leading-relaxed text-center">
        ⚠ {disclaimer}
      </div>
    </div>
  );
}

function Cell({
  label,
  value,
  color,
  small,
}: {
  label: string;
  value: string;
  color?: string;
  small?: boolean;
}) {
  return (
    <div className="bg-bg border border-border rounded-lg p-3 text-center">
      <div className="text-[10px] text-muted uppercase tracking-wider">{label}</div>
      <div
        className={`${small ? "text-sm" : "text-2xl"} font-bold tabular-nums mt-1`}
        style={color ? { color } : undefined}
      >
        {value}
      </div>
    </div>
  );
}

function ConfidenceBar({ p25, p75, median }: { p25: number; p75: number; median: number }) {
  const halfRange = Math.max(Math.abs(p25), Math.abs(p75), 0.5);
  const toPct = (v: number) => ((v + halfRange) / (halfRange * 2)) * 100;
  const left = toPct(p25);
  const right = toPct(p75);
  const med = toPct(median);
  const center = 50;
  return (
    <div className="relative h-3 bg-bg border border-border rounded overflow-hidden">
      <div
        className="absolute top-0 bottom-0 bg-accent/30"
        style={{ left: `${left}%`, width: `${Math.max(right - left, 1)}%` }}
      />
      <div
        className="absolute top-0 bottom-0 w-px bg-muted"
        style={{ left: `${center}%` }}
      />
      <div
        className="absolute top-0 bottom-0 w-1 bg-accent shadow-[0_0_8px_#f7b500]"
        style={{ left: `${med}%` }}
      />
    </div>
  );
}

function classifySpread(spread: number): string {
  if (spread < 0.5) return "tight, signal";
  if (spread < 1.5) return "moderate";
  if (spread < 3) return "wide, noisy";
  return "very wide, no signal";
}

function formatPrice(p: number): string {
  if (p >= 1000) return p.toLocaleString("en-US", { maximumFractionDigits: 2 });
  if (p >= 1) return p.toFixed(4);
  return p.toFixed(6);
}

function formatHorizon(horizon: number, interval: string): string {
  const minutes = (INTERVAL_MINUTES[interval] ?? 0) * horizon;
  if (minutes < 60) return `next ${minutes}m`;
  const hours = minutes / 60;
  if (hours < 24) return `next ${hours % 1 === 0 ? hours : hours.toFixed(1)}h`;
  const days = hours / 24;
  return `next ${days % 1 === 0 ? days : days.toFixed(1)}d`;
}
