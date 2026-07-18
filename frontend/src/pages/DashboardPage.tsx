import {
  Activity,
  ArrowDownLeft,
  ArrowRight,
  ArrowUpRight,
  Bot,
  ChevronDown,
  CircleDollarSign,
  Clock3,
  Coins,
  History,
  LayoutDashboard,
  LogOut,
  Menu,
  MessageSquareText,
  RefreshCw,
  Send,
  Sparkles,
  TrendingUp,
  WalletCards,
  X,
  Zap,
} from "lucide-react";
import {
  type FormEvent,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import ReactMarkdown from "react-markdown";
import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { Brand } from "../components/Brand";
import { Skeleton } from "../components/Skeleton";
import { TradeModal } from "../components/TradeModal";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import { api, ApiRequestError } from "../lib/api";
import type {
  MarketPrice,
  Portfolio,
  PriceHistoryItem,
  TradeSide,
  Transaction,
} from "../types";

interface ChatMessage {
  id: string;
  role: "assistant" | "user";
  text: string;
}

const COINS: Record<
  string,
  { name: string; mark: string; className: string }
> = {
  BTC: { name: "Bitcoin", mark: "₿", className: "coin--btc" },
  ETH: { name: "Ethereum", mark: "Ξ", className: "coin--eth" },
  SOL: { name: "Solana", mark: "S", className: "coin--sol" },
};

const QUICK_PROMPTS = [
  "Portföyümü kısaca değerlendir.",
  "Son işlemlerimi özetle.",
  "Bakiyeme göre genel bir değerlendirme yap.",
];

function formatMoney(value: number, maximumFractionDigits = 2) {
  return new Intl.NumberFormat("tr-TR", {
    style: "currency",
    currency: "USD",
    maximumFractionDigits,
  }).format(value);
}

function formatNumber(value: number, maximumFractionDigits = 8) {
  return new Intl.NumberFormat("tr-TR", {
    maximumFractionDigits,
  }).format(value);
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("tr-TR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}

function getErrorMessage(error: unknown) {
  return error instanceof ApiRequestError
    ? error.message
    : "İşlem sırasında beklenmeyen bir hata oluştu.";
}

export function DashboardPage() {
  const { token, user, logout } = useAuth();
  const { showToast } = useToast();

  const [prices, setPrices] = useState<MarketPrice[]>([]);
  const [portfolio, setPortfolio] = useState<Portfolio | null>(null);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [history, setHistory] = useState<PriceHistoryItem[]>([]);
  const [selectedSymbol, setSelectedSymbol] = useState("BTC");
  const [loading, setLoading] = useState(true);
  const [refreshingPrices, setRefreshingPrices] = useState(false);
  const [tradeState, setTradeState] = useState<{
    coin: MarketPrice;
    side: TradeSide;
  } | null>(null);
  const [tradeSubmitting, setTradeSubmitting] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);

  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: "welcome",
      role: "assistant",
      text:
        "Merhaba! Ben **AkÄ±llÄ± Analiz**. Bakiyeniz, portföyünüz, işlemleriniz ve mevcut piyasa verileri hakkında sorularınızı yanıtlayabilirim.",
    },
  ]);
  const [question, setQuestion] = useState("");
  const [aiLoading, setAiLoading] = useState(false);
  const chatEndRef = useRef<HTMLDivElement | null>(null);

  const loadPrices = useCallback(
    async (showIndicator = false) => {
      if (showIndicator) {
        setRefreshingPrices(true);
      }

      try {
        setPrices(await api.market.prices());
      } catch (error) {
        showToast(getErrorMessage(error), "error");
      } finally {
        setRefreshingPrices(false);
      }
    },
    [showToast],
  );

  const loadAccountData = useCallback(async () => {
    if (!token) {
      return;
    }

    const [nextPortfolio, nextTransactions] = await Promise.all([
      api.portfolio.get(token),
      api.trades.history(token, 50),
    ]);

    setPortfolio(nextPortfolio);
    setTransactions(nextTransactions);
  }, [token]);

  const loadAll = useCallback(async () => {
    if (!token) {
      return;
    }

    setLoading(true);

    try {
      await Promise.all([loadPrices(), loadAccountData()]);
    } catch (error) {
      showToast(getErrorMessage(error), "error");
    } finally {
      setLoading(false);
    }
  }, [loadAccountData, loadPrices, showToast, token]);

  useEffect(() => {
    void loadAll();
  }, [loadAll]);

  useEffect(() => {
    const intervalId = window.setInterval(() => {
      void loadPrices();
    }, 15_000);

    return () => window.clearInterval(intervalId);
  }, [loadPrices]);

  useEffect(() => {
    let active = true;

    async function loadHistory() {
      try {
        const result = await api.market.history(selectedSymbol, 50);
        if (active) {
          setHistory(result);
        }
      } catch (error) {
        if (active) {
          showToast(getErrorMessage(error), "error");
        }
      }
    }

    void loadHistory();

    const intervalId = window.setInterval(() => {
      void loadHistory();
    }, 60_000);

    return () => {
      active = false;
      window.clearInterval(intervalId);
    };
  }, [selectedSymbol, showToast]);

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, aiLoading]);

  const chartData = useMemo(
    () =>
      [...history]
        .reverse()
        .map((item) => ({
          time: new Intl.DateTimeFormat("tr-TR", {
            hour: "2-digit",
            minute: "2-digit",
          }).format(new Date(item.recordedAt)),
          price: Number(item.price),
        })),
    [history],
  );

  const selectedPrice = prices.find(
    (price) => price.symbol === selectedSymbol,
  );

  const lastUpdatedAt = useMemo(() => {
    const timestamps = prices
      .map((price) => new Date(price.updatedAt).getTime())
      .filter(Number.isFinite);

    if (!timestamps.length) {
      return null;
    }

    return new Date(Math.max(...timestamps));
  }, [prices]);

  async function handleTrade(
    side: TradeSide,
    symbol: string,
    quantity: number,
  ) {
    if (!token) {
      return;
    }

    setTradeSubmitting(true);

    try {
      const response = await api.trades.execute(
        token,
        side,
        symbol,
        quantity,
      );

      showToast(
        `${formatNumber(response.quantity)} ${response.symbol} ${
          side === "BUY" ? "satın alındı" : "satıldı"
        }.`,
        "success",
      );

      setTradeState(null);
      await Promise.all([loadAccountData(), loadPrices()]);
    } catch (error) {
      showToast(getErrorMessage(error), "error");
    } finally {
      setTradeSubmitting(false);
    }
  }

  async function handleAiSubmit(
    event?: FormEvent,
    promptOverride?: string,
  ) {
    event?.preventDefault();

    const nextQuestion = (promptOverride ?? question).trim();

    if (!nextQuestion || !token || aiLoading) {
      return;
    }

    setMessages((current) => [
      ...current,
      {
        id: `user-${Date.now()}`,
        role: "user",
        text: nextQuestion,
      },
    ]);
    setQuestion("");
    setAiLoading(true);

    try {
      const response = await api.ai.query(token, nextQuestion);

      setMessages((current) => [
        ...current,
        {
          id: `assistant-${Date.now()}`,
          role: "assistant",
          text: response.answer,
        },
      ]);
    } catch (error) {
      showToast(getErrorMessage(error), "error");
      setMessages((current) => [
        ...current,
        {
          id: `assistant-error-${Date.now()}`,
          role: "assistant",
          text:
            "Şu anda yanıt oluşturamıyorum. Lütfen kısa bir süre sonra tekrar deneyin.",
        },
      ]);
    } finally {
      setAiLoading(false);
    }
  }

  async function handleLogout() {
    try {
      await logout();
    } catch (error) {
      showToast(getErrorMessage(error), "error");
    }
  }

  function scrollToSection(id: string) {
    document.getElementById(id)?.scrollIntoView({
      behavior: "smooth",
      block: "start",
    });
    setMobileMenuOpen(false);
  }

  const initials =
    user?.username
      ?.split(/\s+/)
      .map((part) => part[0])
      .join("")
      .slice(0, 2)
      .toUpperCase() ?? "CP";

  return (
    <div className="dashboard-shell">
      <aside
        className={`sidebar ${
          mobileMenuOpen ? "sidebar--mobile-open" : ""
        }`}
      >
        <div className="sidebar__top">
          <Brand compact />
          <button
            className="sidebar__mobile-close icon-button"
            type="button"
            aria-label="Menüyü kapat"
            onClick={() => setMobileMenuOpen(false)}
          >
            <X size={20} />
          </button>
        </div>

        <nav className="sidebar-nav" aria-label="Ana menü">
          <button onClick={() => scrollToSection("overview")}>
            <LayoutDashboard size={19} />
            Genel bakış
          </button>
          <button onClick={() => scrollToSection("market")}>
            <Activity size={19} />
            Piyasa
          </button>
          <button onClick={() => scrollToSection("portfolio")}>
            <WalletCards size={19} />
            Portföy
          </button>
          <button onClick={() => scrollToSection("transactions")}>
            <History size={19} />
            İşlemler
          </button>
          <button onClick={() => scrollToSection("ai")}>
            <MessageSquareText size={19} />
            AkÄ±llÄ± Analiz
            <span className="nav-badge">Analiz</span>
          </button>
        </nav>

        <div className="sidebar-live-card">
          <div className="sidebar-live-card__head">
            <span className="live-dot" />
            Sistem aktif
          </div>
          <p>
            Fiyatlar otomatik olarak 15 saniyede bir yenileniyor.
          </p>
          <div className="sidebar-live-card__meter">
            <span />
          </div>
        </div>

        <button
          className="sidebar-logout"
          type="button"
          onClick={() => void handleLogout()}
        >
          <LogOut size={18} />
          Güvenli çıkış
        </button>
      </aside>

      {mobileMenuOpen && (
        <button
          className="mobile-overlay"
          type="button"
          aria-label="Menüyü kapat"
          onClick={() => setMobileMenuOpen(false)}
        />
      )}

      <main className="dashboard-main">
        <header className="topbar">
          <div className="topbar__left">
            <button
              className="mobile-menu-button icon-button"
              type="button"
              aria-label="Menüyü aç"
              onClick={() => setMobileMenuOpen(true)}
            >
              <Menu size={21} />
            </button>
            <div>
              <span className="topbar__greeting">Günaydın,</span>
              <h1>{user?.username ?? "CashFlowCoin kullanıcısı"} 👋</h1>
            </div>
          </div>

          <div className="topbar__right">
            <div className="market-status">
              <span className="live-dot" />
              <div>
                <strong>Piyasa canlı</strong>
                <small>
                  {lastUpdatedAt
                    ? `Son güncelleme ${lastUpdatedAt.toLocaleTimeString(
                        "tr-TR",
                        {
                          hour: "2-digit",
                          minute: "2-digit",
                          second: "2-digit",
                        },
                      )}`
                    : "Veriler yükleniyor"}
                </small>
              </div>
            </div>

            <div className="profile-menu">
              <button
                className="profile-button"
                type="button"
                aria-expanded={profileOpen}
                onClick={() => setProfileOpen((current) => !current)}
              >
                <span className="avatar">{initials}</span>
                <span className="profile-button__copy">
                  <strong>{user?.username}</strong>
                  <small>{user?.email}</small>
                </span>
                <ChevronDown size={17} />
              </button>

              {profileOpen && (
                <div className="profile-popover">
                  <div>
                    <span className="avatar avatar--large">
                      {initials}
                    </span>
                    <strong>{user?.username}</strong>
                    <small>{user?.email}</small>
                  </div>
                  <button
                    type="button"
                    onClick={() => void handleLogout()}
                  >
                    <LogOut size={17} />
                    Çıkış yap
                  </button>
                </div>
              )}
            </div>
          </div>
        </header>

        <div className="dashboard-content">
          <section className="hero-panel" id="overview">
            <div className="hero-panel__copy">
              <span className="eyebrow">
                <Zap size={15} />
                Anlık portföy görünümü
              </span>
              <h2>Finansal durumunuz tek bakışta.</h2>
              <p>
                Nakit bakiyenizi, kripto varlıklarınızı ve işlemlerinizi
                gerçek zamanlı takip edin.
              </p>
            </div>
            <div className="hero-panel__art" aria-hidden="true">
              <div className="orb orb--one" />
              <div className="orb orb--two" />
              <div className="hero-chip">
                <TrendingUp size={21} />
                <span>
                  <small>Toplam portföy</small>
                  <strong>
                    {portfolio
                      ? formatMoney(portfolio.totalPortfolioValue)
                      : "—"}
                  </strong>
                </span>
              </div>
            </div>
          </section>

          <section className="stats-grid" aria-label="Portföy özeti">
            <article className="stat-card stat-card--primary">
              <div className="stat-card__icon">
                <WalletCards size={22} />
              </div>
              <div className="stat-card__content">
                <span>Toplam portföy</span>
                {loading ? (
                  <Skeleton className="skeleton--stat" />
                ) : (
                  <strong>
                    {formatMoney(portfolio?.totalPortfolioValue ?? 0)}
                  </strong>
                )}
                <small>
                  <span className="status-pill status-pill--positive">
                    <Activity size={13} />
                    Canlı
                  </span>
                  Nakit + kripto toplamı
                </small>
              </div>
            </article>

            <article className="stat-card">
              <div className="stat-card__icon stat-card__icon--blue">
                <CircleDollarSign size={22} />
              </div>
              <div className="stat-card__content">
                <span>Nakit bakiye</span>
                {loading ? (
                  <Skeleton className="skeleton--stat" />
                ) : (
                  <strong>
                    {formatMoney(portfolio?.cashBalance ?? 0)}
                  </strong>
                )}
                <small>İşleme hazır kullanılabilir tutar</small>
              </div>
            </article>

            <article className="stat-card">
              <div className="stat-card__icon stat-card__icon--violet">
                <Coins size={22} />
              </div>
              <div className="stat-card__content">
                <span>Kripto değeri</span>
                {loading ? (
                  <Skeleton className="skeleton--stat" />
                ) : (
                  <strong>
                    {formatMoney(portfolio?.cryptoValue ?? 0)}
                  </strong>
                )}
                <small>
                  {portfolio?.assets.length ?? 0} aktif varlık
                </small>
              </div>
            </article>

            <article className="stat-card">
              <div className="stat-card__icon stat-card__icon--amber">
                <Clock3 size={22} />
              </div>
              <div className="stat-card__content">
                <span>Toplam işlem</span>
                {loading ? (
                  <Skeleton className="skeleton--stat" />
                ) : (
                  <strong>{transactions.length}</strong>
                )}
                <small>Son 50 işlem içinde</small>
              </div>
            </article>
          </section>

          <section className="dashboard-grid dashboard-grid--market" id="market">
            <article className="panel market-panel">
              <div className="panel-heading">
                <div>
                  <span className="eyebrow">Canlı fiyatlar</span>
                  <h2>Piyasa</h2>
                </div>
                <button
                  className="button button--secondary button--small"
                  type="button"
                  onClick={() => void loadPrices(true)}
                  disabled={refreshingPrices}
                >
                  <RefreshCw
                    size={16}
                    className={
                      refreshingPrices ? "is-spinning" : ""
                    }
                  />
                  Yenile
                </button>
              </div>

              <div className="market-list">
                {loading
                  ? [0, 1, 2].map((index) => (
                      <Skeleton
                        className="skeleton--market-row"
                        key={index}
                      />
                    ))
                  : prices.map((coin) => {
                      const metadata = COINS[coin.symbol] ?? {
                        name: coin.symbol,
                        mark: coin.symbol[0],
                        className: "",
                      };
                      const asset = portfolio?.assets.find(
                        (item) => item.symbol === coin.symbol,
                      );

                      return (
                        <article
                          className={`market-row ${
                            selectedSymbol === coin.symbol
                              ? "market-row--selected"
                              : ""
                          }`}
                          key={coin.symbol}
                          onClick={() =>
                            setSelectedSymbol(coin.symbol)
                          }
                        >
                          <div className="asset-identity">
                            <span
                              className={`coin-icon ${metadata.className}`}
                            >
                              {metadata.mark}
                            </span>
                            <span>
                              <strong>{metadata.name}</strong>
                              <small>{coin.symbol}</small>
                            </span>
                          </div>

                          <div className="market-row__price">
                            <strong>{formatMoney(coin.price)}</strong>
                            <span>
                              <span className="live-dot live-dot--small" />
                              Canlı
                            </span>
                          </div>

                          <div className="market-row__actions">
                            <button
                              className="trade-action trade-action--buy"
                              type="button"
                              onClick={(event) => {
                                event.stopPropagation();
                                setTradeState({
                                  coin,
                                  side: "BUY",
                                });
                              }}
                            >
                              <ArrowDownLeft size={15} />
                              Al
                            </button>
                            <button
                              className="trade-action trade-action--sell"
                              type="button"
                              disabled={!asset || asset.quantity <= 0}
                              onClick={(event) => {
                                event.stopPropagation();
                                setTradeState({
                                  coin,
                                  side: "SELL",
                                });
                              }}
                            >
                              <ArrowUpRight size={15} />
                              Sat
                            </button>
                          </div>
                        </article>
                      );
                    })}
              </div>
            </article>

            <article className="panel chart-panel">
              <div className="panel-heading">
                <div>
                  <span className="eyebrow">Fiyat geçmişi</span>
                  <h2>{selectedSymbol} grafiği</h2>
                </div>
                <div className="selected-coin-price">
                  <span>{selectedSymbol}</span>
                  <strong>
                    {selectedPrice
                      ? formatMoney(selectedPrice.price)
                      : "—"}
                  </strong>
                </div>
              </div>

              <div className="chart-wrap">
                {chartData.length > 1 ? (
                  <ResponsiveContainer width="100%" height="100%">
                    <AreaChart
                      data={chartData}
                      margin={{
                        top: 12,
                        right: 10,
                        left: 0,
                        bottom: 0,
                      }}
                    >
                      <defs>
                        <linearGradient
                          id="priceGradient"
                          x1="0"
                          y1="0"
                          x2="0"
                          y2="1"
                        >
                          <stop
                            offset="0%"
                            stopColor="#6ee7f9"
                            stopOpacity={0.38}
                          />
                          <stop
                            offset="100%"
                            stopColor="#6ee7f9"
                            stopOpacity={0}
                          />
                        </linearGradient>
                      </defs>
                      <CartesianGrid
                        vertical={false}
                        stroke="rgba(148, 163, 184, 0.12)"
                      />
                      <XAxis
                        dataKey="time"
                        axisLine={false}
                        tickLine={false}
                        tick={{
                          fill: "#7f90a8",
                          fontSize: 11,
                        }}
                        minTickGap={26}
                      />
                      <YAxis
                        width={72}
                        axisLine={false}
                        tickLine={false}
                        domain={["auto", "auto"]}
                        tick={{
                          fill: "#7f90a8",
                          fontSize: 11,
                        }}
                        tickFormatter={(value) =>
                          `$${new Intl.NumberFormat("en-US", {
                            notation: "compact",
                            maximumFractionDigits: 1,
                          }).format(Number(value))}`
                        }
                      />
                      <Tooltip
                        cursor={{
                          stroke: "rgba(110,231,249,.35)",
                        }}
                        contentStyle={{
                          background: "#0d1c2c",
                          border:
                            "1px solid rgba(148,163,184,.18)",
                          borderRadius: "12px",
                          color: "#f8fafc",
                        }}
                        formatter={(value) => [
                          formatMoney(Number(value), 4),
                          "Fiyat",
                        ]}
                      />
                      <Area
                        type="monotone"
                        dataKey="price"
                        stroke="#6ee7f9"
                        strokeWidth={2.5}
                        fill="url(#priceGradient)"
                        activeDot={{ r: 5 }}
                      />
                    </AreaChart>
                  </ResponsiveContainer>
                ) : (
                  <div className="chart-empty">
                    <Activity size={30} />
                    <strong>Grafik verisi hazırlanıyor</strong>
                    <span>
                      Fiyat geçmişi oluştukça grafik burada görünecek.
                    </span>
                  </div>
                )}
              </div>
            </article>
          </section>

          <section className="dashboard-grid" id="portfolio">
            <article className="panel portfolio-panel">
              <div className="panel-heading">
                <div>
                  <span className="eyebrow">Varlık dağılımı</span>
                  <h2>Portföyüm</h2>
                </div>
                <span className="panel-meta">
                  {portfolio?.assets.length ?? 0} varlık
                </span>
              </div>

              <div className="table-scroll">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Varlık</th>
                      <th>Miktar</th>
                      <th>Güncel fiyat</th>
                      <th>Toplam değer</th>
                    </tr>
                  </thead>
                  <tbody>
                    {loading ? (
                      [0, 1, 2].map((index) => (
                        <tr key={index}>
                          <td colSpan={4}>
                            <Skeleton className="skeleton--table" />
                          </td>
                        </tr>
                      ))
                    ) : portfolio?.assets.length ? (
                      portfolio.assets.map((asset) => {
                        const metadata = COINS[asset.symbol] ?? {
                          name: asset.symbol,
                          mark: asset.symbol[0],
                          className: "",
                        };

                        return (
                          <tr key={asset.symbol}>
                            <td>
                              <div className="asset-identity">
                                <span
                                  className={`coin-icon coin-icon--small ${metadata.className}`}
                                >
                                  {metadata.mark}
                                </span>
                                <span>
                                  <strong>{metadata.name}</strong>
                                  <small>{asset.symbol}</small>
                                </span>
                              </div>
                            </td>
                            <td>
                              {formatNumber(asset.quantity)}
                            </td>
                            <td>
                              {formatMoney(asset.currentPrice)}
                            </td>
                            <td className="table-value">
                              {formatMoney(asset.currentValue)}
                            </td>
                          </tr>
                        );
                      })
                    ) : (
                      <tr>
                        <td colSpan={4}>
                          <div className="table-empty">
                            <Coins size={28} />
                            <strong>Henüz kripto varlığınız yok</strong>
                            <span>
                              Piyasa bölümünden ilk alış işleminizi
                              gerçekleştirebilirsiniz.
                            </span>
                          </div>
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </article>

            <article className="panel ai-preview-panel">
              <div className="ai-preview-panel__icon">
                <Sparkles size={24} />
              </div>
              <span className="eyebrow">Veriye dayalÄ±</span>
              <h2>Portföyünüzü birlikte yorumlayalım.</h2>
              <p>
                AkÄ±llÄ± Analiz, hesabınızdaki gerçek bakiye, varlık ve
                işlem verilerini kullanarak size anlaşılır bir özet
                sunar.
              </p>
              <button
                className="button button--primary"
                type="button"
                onClick={() => scrollToSection("ai")}
              >
                Analizi aÃ§
                <ArrowRight size={17} />
              </button>
            </article>
          </section>

          <section className="panel transaction-panel" id="transactions">
            <div className="panel-heading">
              <div>
                <span className="eyebrow">Hareketler</span>
                <h2>Son işlemler</h2>
              </div>
              <span className="panel-meta">
                En yeni işlemler üstte
              </span>
            </div>

            <div className="table-scroll">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>İşlem</th>
                    <th>Varlık</th>
                    <th>Miktar</th>
                    <th>Fiyat</th>
                    <th>Toplam</th>
                    <th>Tarih</th>
                  </tr>
                </thead>
                <tbody>
                  {loading ? (
                    [0, 1, 2].map((index) => (
                      <tr key={index}>
                        <td colSpan={6}>
                          <Skeleton className="skeleton--table" />
                        </td>
                      </tr>
                    ))
                  ) : transactions.length ? (
                    transactions.map((transaction) => (
                      <tr key={transaction.id}>
                        <td>
                          <span
                            className={`transaction-type ${
                              transaction.type === "BUY"
                                ? "transaction-type--buy"
                                : "transaction-type--sell"
                            }`}
                          >
                            {transaction.type === "BUY" ? (
                              <ArrowDownLeft size={14} />
                            ) : (
                              <ArrowUpRight size={14} />
                            )}
                            {transaction.type === "BUY"
                              ? "Alış"
                              : "Satış"}
                          </span>
                        </td>
                        <td>
                          <strong>{transaction.symbol}</strong>
                        </td>
                        <td>
                          {formatNumber(transaction.quantity)}
                        </td>
                        <td>
                          {formatMoney(transaction.executionPrice)}
                        </td>
                        <td className="table-value">
                          {formatMoney(transaction.totalAmount)}
                        </td>
                        <td>{formatDate(transaction.createdAt)}</td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td colSpan={6}>
                        <div className="table-empty">
                          <History size={28} />
                          <strong>Henüz işlem bulunmuyor</strong>
                          <span>
                            Gerçekleştirdiğiniz alış ve satışlar burada
                            listelenecek.
                          </span>
                        </div>
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </section>

          <section className="ai-section" id="ai">
            <div className="ai-section__intro">
              <span className="ai-kicker">
                <Bot size={18} />
                AkÄ±llÄ± Analiz
              </span>
              <h2>Verilerinize özel akıllı asistan.</h2>
              <p>
                Hesabınız, portföyünüz, son işlemleriniz ve mevcut piyasa
                fiyatları hakkında doğal dilde sorular sorun.
              </p>
              <div className="ai-capabilities">
                <span><Sparkles size={15} /> Portföy özeti</span>
                <span><Sparkles size={15} /> İşlem analizi</span>
                <span><Sparkles size={15} /> Piyasa yorumu</span>
              </div>
            </div>

            <article className="chat-card">
              <header className="chat-header">
                <div className="chat-bot-avatar">
                  <Bot size={21} />
                </div>
                <div>
                  <strong>AkÄ±llÄ± Analiz</strong>
                  <span>
                    <span className="live-dot live-dot--small" />
                    Çevrimiçi · Hesap verileriyle Ã§alÄ±ÅŸÄ±r
                  </span>
                </div>
              </header>

              <div className="chat-messages">
                {messages.map((message) => (
                  <div
                    className={`chat-message chat-message--${message.role}`}
                    key={message.id}
                  >
                    {message.role === "assistant" && (
                      <span className="chat-avatar">
                        <Bot size={16} />
                      </span>
                    )}
                    <div className="chat-bubble">
                      {message.role === "assistant" ? (
                        <ReactMarkdown>{message.text}</ReactMarkdown>
                      ) : (
                        <p>{message.text}</p>
                      )}
                    </div>
                  </div>
                ))}

                {aiLoading && (
                  <div className="chat-message chat-message--assistant">
                    <span className="chat-avatar">
                      <Bot size={16} />
                    </span>
                    <div className="chat-bubble chat-bubble--typing">
                      <span />
                      <span />
                      <span />
                    </div>
                  </div>
                )}
                <div ref={chatEndRef} />
              </div>

              {messages.length === 1 && (
                <div className="quick-prompts">
                  {QUICK_PROMPTS.map((prompt) => (
                    <button
                      type="button"
                      key={prompt}
                      onClick={() =>
                        void handleAiSubmit(undefined, prompt)
                      }
                    >
                      {prompt}
                    </button>
                  ))}
                </div>
              )}

              <form
                className="chat-composer"
                onSubmit={(event) => void handleAiSubmit(event)}
              >
                <textarea
                  rows={1}
                  maxLength={1000}
                  placeholder="AkÄ±llÄ± Analiz'a bir soru sorun..."
                  value={question}
                  onChange={(event) => setQuestion(event.target.value)}
                  onKeyDown={(event) => {
                    if (
                      event.key === "Enter" &&
                      !event.shiftKey &&
                      !event.nativeEvent.isComposing
                    ) {
                      event.preventDefault();
                      void handleAiSubmit();
                    }
                  }}
                />
                <button
                  type="submit"
                  disabled={!question.trim() || aiLoading}
                  aria-label="Mesaj gönder"
                >
                  {aiLoading ? (
                    <span className="spinner" />
                  ) : (
                    <Send size={18} />
                  )}
                </button>
              </form>
              <p className="chat-disclaimer">
                AI yanıtları eğitim amaçlıdır ve finansal tavsiye değildir.
              </p>
            </article>
          </section>

          <footer className="dashboard-footer">
            <span>© 2026 CashFlowCoin</span>
            <span>Eğitim amaçlı kripto işlem simülasyonu</span>
          </footer>
        </div>
      </main>

      {tradeState && portfolio && (
        <TradeModal
          coin={tradeState.coin}
          side={tradeState.side}
          portfolio={portfolio}
          submitting={tradeSubmitting}
          onClose={() => setTradeState(null)}
          onSubmit={handleTrade}
        />
      )}
    </div>
  );
}