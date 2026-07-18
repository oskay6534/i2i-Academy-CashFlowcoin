export type TradeSide = "BUY" | "SELL";

export interface LoginResponse {
  token: string;
  tokenType: string;
  expiresInSeconds: number;
  userId: number;
  username: string;
  email: string;
  cashBalance: number;
}

export interface SessionResponse {
  userId: number;
  username: string;
  email: string;
  cashBalance: number;
}

export interface RegisterResponse {
  userId: number;
  username: string;
  email: string;
  initialBalance: number;
}

export interface MarketPrice {
  symbol: string;
  price: number;
  updatedAt: string;
}

export interface PriceHistoryItem {
  id: number;
  symbol: string;
  price: number;
  recordedAt: string;
}

export interface PortfolioAsset {
  symbol: string;
  quantity: number;
  currentPrice: number;
  currentValue: number;
}

export interface Portfolio {
  userId: number;
  cashBalance: number;
  cryptoValue: number;
  totalPortfolioValue: number;
  assets: PortfolioAsset[];
}

export interface Transaction {
  id: number;
  type: TradeSide;
  symbol: string;
  quantity: number;
  executionPrice: number;
  totalAmount: number;
  createdAt: string;
}

export interface TradeResponse {
  transactionId: number;
  type: TradeSide;
  symbol: string;
  quantity: number;
  executionPrice: number;
  totalAmount: number;
  cashBalance: number;
  assetQuantity: number;
  executedAt: string;
}

export interface AiResponse {
  answer: string;
  model: string;
  generatedAt: string;
}

export interface ApiErrorPayload {
  timestamp?: string;
  status?: number;
  error?: string;
  message?: string;
  path?: string;
}