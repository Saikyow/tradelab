"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { fetchAnalogs } from "@/lib/api";
import type { AnalogParams, AnalogResponse } from "@/lib/types";
import { AnalogsList } from "@/components/AnalogsList";
import { Controls } from "@/components/Controls";
import { ProjectionPanel } from "@/components/ProjectionPanel";

const DEFAULT_PARAMS: AnalogParams = {
  symbol: "BTCUSDT",
  interval: "15m",
  limit: 100000,
  rsiPeriod: 14,
  emaPeriod: 14,
  k: 20,
  horizon: 12,
};

const POLL_INTERVAL_MS = 5000;

export default function Home() {
  const [params, setParams] = useState<AnalogParams>(DEFAULT_PARAMS);
  const [analogData, setAnalogData] = useState<AnalogResponse | null>(null);
  const [currentPrice, setCurrentPrice] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [live, setLive] = useState(true);
  const [lastUpdated, setLastUpdated] = useState<number | null>(null);

  const inFlight = useRef(false);

  const reload = useCallback(async () => {
    if (inFlight.current) return;
    inFlight.current = true;
    setLoading(true);
    setError(null);
    try {
      const analogs = await fetchAnalogs(params);
      setAnalogData(analogs);
      setLastUpdated(Date.now());
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
      inFlight.current = false;
    }
  }, [params]);

  useEffect(() => {
    reload();
  }, [reload]);

  useEffect(() => {
    if (!live) return;
    const id = setInterval(reload, POLL_INTERVAL_MS);
    return () => clearInterval(id);
  }, [live, reload]);

  useEffect(() => {
    setCurrentPrice(null);
    const stream = `${params.symbol.toLowerCase()}@aggTrade`;
    const ws = new WebSocket(`wss://stream.binance.com:9443/ws/${stream}`);

    ws.onmessage = (evt) => {
      try {
        const msg = JSON.parse(evt.data);
        if (msg?.p) setCurrentPrice(parseFloat(msg.p));
      } catch {
        // ignore
      }
    };

    return () => {
      ws.close();
    };
  }, [params.symbol]);

  return (
    <main className="min-h-screen p-4 max-w-3xl mx-auto">
      <header className="bg-panel border border-border rounded-lg p-3 mb-4">
        <Controls
          params={params}
          onChange={setParams}
          onReload={reload}
          loading={loading}
          live={live}
          onToggleLive={() => setLive((v) => !v)}
          lastUpdated={lastUpdated}
        />
      </header>

      {error && (
        <div className="bg-down/10 border border-down/40 text-down rounded-lg p-3 mb-4 text-sm font-mono">
          {error}
        </div>
      )}

      <div className="mb-4">
        <ProjectionPanel
          data={analogData}
          symbol={params.symbol}
          interval={params.interval}
          horizon={params.horizon}
          currentPrice={currentPrice}
        />
      </div>

      <AnalogsList analogs={analogData?.analogs ?? []} />
    </main>
  );
}
