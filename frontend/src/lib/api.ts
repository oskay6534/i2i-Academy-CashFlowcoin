import type {
  AiResponse,
  ApiErrorPayload,
  LoginResponse,
  MarketPrice,
  Portfolio,
  PriceHistoryItem,
  RegisterResponse,
  SessionResponse,
  TradeResponse,
  TradeSide,
  Transaction,
} from "../types";

const explicitApiBase =
  import.meta.env.VITE_API_BASE_URL?.trim();
const deployedApiHost =
  import.meta.env.VITE_API_HOST?.trim();

const API_BASE =
  explicitApiBase ||
  (deployedApiHost ? `https://${deployedApiHost}` : "");

export class ApiRequestError extends Error {
  readonly status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = "ApiRequestError";
    this.status = status;
  }
}

function translateMessage(message: string): string {
  const translations: Record<string, string> = {
    "Insufficient funds to complete this trade":
      "Bu işlemi tamamlamak için yeterli bakiyeniz yok.",
    "Trade quantity must be greater than zero":
      "İşlem miktarı sıfırdan büyük olmalıdır.",
    "Trade quantity can contain at most 8 decimal places":
      "İşlem miktarı en fazla 8 ondalık basamak içerebilir.",
    "Session is invalid or has expired":
      "Oturumunuz sona erdi. Lütfen yeniden giriş yapın.",
    "Username, email or password is incorrect":
      "Kullanıcı adı, e-posta veya şifre hatalı.",
    "Gemini request limit was reached":
      "Yapay zekâ kullanım sınırına ulaşıldı. Biraz sonra tekrar deneyin.",
    "Gemini service timed out or could not be reached":
      "Yapay zekâ servisine şu anda ulaşılamıyor.",
  };

  if (translations[message]) {
    return translations[message];
  }

  if (message.startsWith("Insufficient ") && message.includes(" quantity")) {
    return "Satış için yeterli kripto bakiyeniz yok.";
  }

  if (message.startsWith("You do not own ")) {
    return "Bu kripto varlığı portföyünüzde bulunmuyor.";
  }

  return message || "Beklenmeyen bir hata oluştu.";
}

async function request<T>(
  path: string,
  options: RequestInit = {},
  token?: string | null,
): Promise<T> {
  const headers = new Headers(options.headers);

  if (options.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json; charset=utf-8");
  }

  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    let payload: ApiErrorPayload | null = null;

    try {
      payload = (await response.json()) as ApiErrorPayload;
    } catch {
      payload = null;
    }

    throw new ApiRequestError(
      translateMessage(
        payload?.message ??
          payload?.error ??
          `İstek başarısız oldu (${response.status}).`,
      ),
      response.status,
    );
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

export const api = {
  auth: {
    login: (identifier: string, password: string) =>
      request<LoginResponse>("/api/auth/login", {
        method: "POST",
        body: JSON.stringify({ identifier, password }),
      }),

    register: (username: string, email: string, password: string) =>
      request<RegisterResponse>("/api/auth/register", {
        method: "POST",
        body: JSON.stringify({ username, email, password }),
      }),

    session: (token: string) =>
      request<SessionResponse>("/api/auth/session", {}, token),

    logout: (token: string) =>
      request<void>(
        "/api/auth/logout",
        {
          method: "POST",
        },
        token,
      ),
  },

  market: {
    prices: () => request<MarketPrice[]>("/api/market/prices"),

    history: (symbol: string, limit = 50) =>
      request<PriceHistoryItem[]>(
        `/api/market/history/${encodeURIComponent(symbol)}?limit=${limit}`,
      ),
  },

  portfolio: {
    get: (token: string) =>
      request<Portfolio>("/api/portfolio", {}, token),
  },

  trades: {
    history: (token: string, limit = 50) =>
      request<Transaction[]>(
        `/api/trades/history?limit=${limit}`,
        {},
        token,
      ),

    execute: (
      token: string,
      side: TradeSide,
      symbol: string,
      quantity: number,
    ) =>
      request<TradeResponse>(
        `/api/trades/${side === "BUY" ? "buy" : "sell"}`,
        {
          method: "POST",
          body: JSON.stringify({ symbol, quantity }),
        },
        token,
      ),
  },

  ai: {
    query: (token: string, question: string) =>
      request<AiResponse>(
        "/api/ai/query",
        {
          method: "POST",
          body: JSON.stringify({ question }),
        },
        token,
      ),
  },
};