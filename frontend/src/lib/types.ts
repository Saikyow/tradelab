export type Candle = {
  openTime: number;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
};

export type FeatureVector = {
  rsi: number;
  priceVsEma: number;
  emaSlope: number;
  volatility: number;
  volumeAnomaly: number;
};

export type Analog = {
  index: number;
  openTime: number;
  distance: number;
  changePct: number;
};

export type AnalogStats = {
  count: number;
  upPct: number;
  meanChangePct: number;
  medianChangePct: number;
  p25ChangePct: number;
  p75ChangePct: number;
};

export type AnalogResponse = {
  currentIndex: number;
  currentOpenTime: number;
  currentVector: FeatureVector;
  analogs: Analog[];
  stats: AnalogStats;
  disclaimer: string;
};

export type AnalogParams = {
  symbol: string;
  interval: string;
  limit: number;
  rsiPeriod: number;
  emaPeriod: number;
  k: number;
  horizon: number;
};
