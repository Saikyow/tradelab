import type { AnalogParams, AnalogResponse, Candle } from "./types";

const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080";

async function getJson<T>(path: string, params: Record<string, string | number | boolean>): Promise<T> {
  const qs = new URLSearchParams();
  for (const [k, v] of Object.entries(params)) {
    qs.set(k, String(v));
  }
  const res = await fetch(`${API_BASE}${path}?${qs.toString()}`, { cache: "no-store" });
  if (!res.ok) {
    const detail = await res.text().catch(() => res.statusText);
    throw new Error(`${res.status} ${detail}`);
  }
  return res.json();
}

export function fetchCandles(symbol: string, interval: string, limit: number): Promise<Candle[]> {
  return getJson<Candle[]>("/api/candles", { symbol, interval, limit });
}

export function fetchAnalogs(params: AnalogParams): Promise<AnalogResponse> {
  return getJson<AnalogResponse>("/api/analogs", { ...params });
}
