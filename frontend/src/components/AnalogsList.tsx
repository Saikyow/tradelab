"use client";

import type { Analog } from "@/lib/types";

type Props = {
  analogs: Analog[];
};

function formatDate(ts: number): string {
  const d = new Date(ts);
  return d.toISOString().slice(0, 16).replace("T", " ");
}

export function AnalogsList({ analogs }: Props) {
  if (analogs.length === 0) {
    return null;
  }

  return (
    <div className="bg-panel border border-border rounded-lg p-4 font-mono">
      <div className="flex items-baseline justify-between mb-3">
        <span className="text-muted text-xs uppercase tracking-wider">analogs ({analogs.length})</span>
        <span className="text-muted text-[10px]">sorted by similarity</span>
      </div>

      <div className="grid grid-cols-12 gap-2 text-[10px] text-muted uppercase pb-2 border-b border-border">
        <div className="col-span-1">#</div>
        <div className="col-span-5">date (utc)</div>
        <div className="col-span-3 text-right">distance</div>
        <div className="col-span-3 text-right">outcome</div>
      </div>

      <div className="max-h-80 overflow-y-auto">
        {analogs.map((a, i) => {
          const up = a.changePct >= 0;
          return (
            <div
              key={a.index}
              className="grid grid-cols-12 gap-2 text-xs py-2 border-b border-border/40 hover:bg-bg/50"
            >
              <div className="col-span-1 text-muted">{i + 1}</div>
              <div className="col-span-5 text-text">{formatDate(a.openTime)}</div>
              <div className="col-span-3 text-right text-muted">{a.distance.toFixed(4)}</div>
              <div
                className="col-span-3 text-right font-bold"
                style={{ color: up ? "#26a69a" : "#ef5350" }}
              >
                {up ? "+" : ""}
                {a.changePct.toFixed(2)}%
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
