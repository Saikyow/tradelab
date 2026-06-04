"use client";

import type { AnalogParams } from "@/lib/types";

type Props = {
  params: AnalogParams;
  onChange: (next: AnalogParams) => void;
  onReload: () => void;
  loading: boolean;
  live: boolean;
  onToggleLive: () => void;
  lastUpdated: number | null;
};

const SYMBOLS = ["BTCUSDT", "ETHUSDT", "SOLUSDT"];
const INTERVALS = ["1m", "15m", "1h", "4h"];

export function Controls({ params, onChange, onReload, loading, live, onToggleLive, lastUpdated }: Props) {
  const set = <K extends keyof AnalogParams>(key: K, value: AnalogParams[K]) =>
    onChange({ ...params, [key]: value });

  return (
    <div className="flex flex-wrap items-center gap-3">
      <span className="font-mono text-accent text-lg font-bold tracking-wider mr-4">TRADELAB</span>

      <label className="flex items-center gap-1 text-muted text-xs">
        symbol
        <select value={params.symbol} onChange={(e) => set("symbol", e.target.value)}>
          {SYMBOLS.map((s) => (
            <option key={s}>{s}</option>
          ))}
        </select>
      </label>

      <label className="flex items-center gap-1 text-muted text-xs">
        interval
        <select value={params.interval} onChange={(e) => set("interval", e.target.value)}>
          {INTERVALS.map((i) => (
            <option key={i}>{i}</option>
          ))}
        </select>
      </label>

      <label className="flex items-center gap-1 text-muted text-xs">
        k
        <input
          type="number"
          value={params.k}
          min={1}
          max={100}
          onChange={(e) => set("k", Number(e.target.value))}
          className="w-16"
        />
      </label>

      <label className="flex items-center gap-1 text-muted text-xs">
        horizon
        <input
          type="number"
          value={params.horizon}
          min={1}
          max={200}
          onChange={(e) => set("horizon", Number(e.target.value))}
          className="w-16"
        />
      </label>

      <button onClick={onToggleLive} className="flex items-center gap-2">
        <span
          className={`w-2 h-2 rounded-full ${live ? "bg-up animate-pulse" : "bg-muted"}`}
        />
        {live ? "LIVE" : "PAUSED"}
      </button>

      <span className="text-muted text-[10px] font-mono">{formatLastUpdated(lastUpdated)}</span>

      <button onClick={onReload} disabled={loading} className="ml-auto">
        {loading ? "..." : "⟳"}
      </button>
    </div>
  );
}

function formatLastUpdated(ts: number | null): string {
  if (!ts) return "";
  const d = new Date(ts);
  const hh = String(d.getHours()).padStart(2, "0");
  const mm = String(d.getMinutes()).padStart(2, "0");
  const ss = String(d.getSeconds()).padStart(2, "0");
  return `updated ${hh}:${mm}:${ss}`;
}
