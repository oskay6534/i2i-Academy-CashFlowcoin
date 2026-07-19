import {
  ArrowRight,
  BarChart3,
  Check,
  Eye,
  EyeOff,
  LockKeyhole,
  Mail,
  RefreshCw,
  ShieldCheck,
  User,
  WalletCards,
} from "lucide-react";
import {
  type FormEvent,
  useEffect,
  useState,
} from "react";
import { Brand } from "../components/Brand";
import { ApiRequestError, api } from "../lib/api";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import type { MarketPrice } from "../types";

type AuthMode = "login" | "register";

const COIN_NAMES: Record<string, string> = {
  BTC: "Bitcoin",
  ETH: "Ethereum",
  SOL: "Solana",
  BNB: "BNB",
  XRP: "XRP",
  ADA: "Cardano",
  DOGE: "Dogecoin",
  AVAX: "Avalanche",
  DOT: "Polkadot",
  LINK: "Chainlink",
};

function formatMoney(value: number) {
  return new Intl.NumberFormat("tr-TR", {
    style: "currency",
    currency: "USD",
    maximumFractionDigits: 2,
  }).format(value);
}

export function AuthPage() {
  const { login, register } = useAuth();
  const { showToast } = useToast();

  const [mode, setMode] = useState<AuthMode>("login");
  const [identifier, setIdentifier] = useState("");
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  const [prices, setPrices] = useState<MarketPrice[]>([]);
  const [pricesLoading, setPricesLoading] = useState(true);
  const [pricesRefreshing, setPricesRefreshing] = useState(false);

  async function loadPrices(manual = false) {
    if (manual) {
      setPricesRefreshing(true);
    }

    try {
      setPrices(await api.market.prices());
    } catch {
      // The login form remains usable even when the public market feed is unavailable.
    } finally {
      setPricesLoading(false);
      setPricesRefreshing(false);
    }
  }

  useEffect(() => {
    void loadPrices();

    const intervalId = window.setInterval(() => {
      void loadPrices();
    }, 5_000);

    return () => window.clearInterval(intervalId);
  }, []);

  function switchMode(nextMode: AuthMode) {
    setMode(nextMode);
    setError("");
    setPassword("");
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setSubmitting(true);
    setError("");

    try {
      if (mode === "login") {
        await login(identifier.trim(), password);
        showToast("Oturumunuz güvenli şekilde açıldı.", "success");
      } else {
        const response = await register(
          username.trim(),
          email.trim(),
          password,
        );

        showToast(
          `Hesabınız oluşturuldu. Başlangıç bakiyeniz ${formatMoney(
            response.initialBalance,
          )}.`,
          "success",
        );

        setIdentifier(email.trim());
        switchMode("login");
      }
    } catch (caughtError) {
      setError(
        caughtError instanceof ApiRequestError
          ? caughtError.message
          : "İşlem tamamlanamadı. Lütfen tekrar deneyin.",
      );
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="auth-layout">
      <section className="auth-showcase">
        <div className="auth-showcase__content">
          <Brand />

          <div className="auth-copy">
            <span className="auth-badge">
              <ShieldCheck size={16} />
              Güvenli işlem simülasyonu
            </span>

            <h1>
              Piyasanızı, bakiyenizi ve işlemlerinizi
              <span> sade bir akışta</span> yönetin.
            </h1>

            <p>
              CashFlowCoin; canlı fiyatları, portföyünüzü ve işlem
              geçmişinizi karmaşa olmadan tek ekranda bir araya getirir.
            </p>
          </div>

          <div className="auth-features">
            <article>
              <div>
                <BarChart3 size={21} />
              </div>
              <span>
                <strong>Canlı piyasa</strong>
                15 saniyede bir yenilenen fiyat akışı
              </span>
            </article>

            <article>
              <div>
                <WalletCards size={21} />
              </div>
              <span>
                <strong>Portföy kontrolü</strong>
                Bakiye, varlık ve işlem takibi
              </span>
            </article>

            <article>
              <div>
                <ShieldCheck size={21} />
              </div>
              <span>
                <strong>Güvenli oturum</strong>
                Redis tabanlı süreli erişim
              </span>
            </article>
          </div>

          <div className="auth-trust">
            <span><Check size={15} /> Canlı fiyat takibi</span>
            <span><Check size={15} /> Güvenli veri saklama</span>
            <span><Check size={15} /> Kullanıcıya özel analiz</span>
          </div>
        </div>
      </section>

      <section className="auth-panel">
        <div className="auth-card">
          <div className="auth-card__mobile-brand">
            <Brand compact />
          </div>

          <section
            className="public-market-preview"
            aria-label="Güncel kripto fiyatları"
          >
            <div className="public-market-preview__header">
              <div>
                <span className="eyebrow">Piyasa özeti</span>
                <h2>Güncel fiyatlar</h2>
              </div>

              <button
                className="market-refresh-button"
                type="button"
                aria-label="Fiyatları yenile"
                disabled={pricesRefreshing}
                onClick={() => void loadPrices(true)}
              >
                <RefreshCw
                  size={16}
                  className={pricesRefreshing ? "is-spinning" : ""}
                />
              </button>
            </div>

            <div className="public-market-grid">
              {pricesLoading
                ? [0, 1, 2].map((item) => (
                    <div className="public-market-card is-loading" key={item}>
                      <span />
                      <strong />
                    </div>
                  ))
                : prices.slice(0, 3).map((price) => (
                    <article
                      className="public-market-card"
                      key={price.symbol}
                    >
                      <div>
                        <span className="public-market-symbol">
                          {price.symbol}
                        </span>
                        <small>
                          {COIN_NAMES[price.symbol] ?? price.symbol}
                        </small>
                      </div>

                      <strong>{formatMoney(price.price)}</strong>
                    </article>
                  ))}
            </div>

            <p className="public-market-note">
              Fiyatları görmek için giriş yapmanız gerekmez.
            </p>
          </section>

          <div className="auth-card__heading">
            <span className="eyebrow">
              {mode === "login" ? "Hesabınıza dönün" : "Yeni hesap"}
            </span>

            <h2>
              {mode === "login"
                ? "Güvenli giriş"
                : "CashFlowCoin hesabı oluşturun"}
            </h2>

            <p>
              {mode === "login"
                ? "Portföyünüze ve işlem geçmişinize devam edin."
                : "Simülasyon bakiyenizle piyasa panelini kullanmaya başlayın."}
            </p>
          </div>

          <div className="auth-tabs" role="tablist">
            <button
              className={mode === "login" ? "is-active" : ""}
              type="button"
              role="tab"
              aria-selected={mode === "login"}
              onClick={() => switchMode("login")}
            >
              Giriş yap
            </button>

            <button
              className={mode === "register" ? "is-active" : ""}
              type="button"
              role="tab"
              aria-selected={mode === "register"}
              onClick={() => switchMode("register")}
            >
              Kayıt ol
            </button>
          </div>

          <form className="auth-form" onSubmit={handleSubmit}>
            {mode === "register" && (
              <>
                <label className="field">
                  <span className="field__label">Kullanıcı adı</span>
                  <div className="input-shell">
                    <User size={18} />
                    <input
                      required
                      minLength={3}
                      autoComplete="username"
                      placeholder="kullaniciadi"
                      value={username}
                      onChange={(event) =>
                        setUsername(event.target.value)
                      }
                    />
                  </div>
                </label>

                <label className="field">
                  <span className="field__label">E-posta adresi</span>
                  <div className="input-shell">
                    <Mail size={18} />
                    <input
                      required
                      type="email"
                      autoComplete="email"
                      placeholder="ornek@email.com"
                      value={email}
                      onChange={(event) => setEmail(event.target.value)}
                    />
                  </div>
                </label>
              </>
            )}

            {mode === "login" && (
              <label className="field">
                <span className="field__label">
                  Kullanıcı adı veya e-posta
                </span>
                <div className="input-shell">
                  <User size={18} />
                  <input
                    required
                    autoComplete="username"
                    placeholder="Kullanıcı adı veya e-posta"
                    value={identifier}
                    onChange={(event) =>
                      setIdentifier(event.target.value)
                    }
                  />
                </div>
              </label>
            )}

            <label className="field">
              <span className="field__label">Şifre</span>
              <div className="input-shell">
                <LockKeyhole size={18} />
                <input
                  required
                  minLength={8}
                  type={showPassword ? "text" : "password"}
                  autoComplete={
                    mode === "login"
                      ? "current-password"
                      : "new-password"
                  }
                  placeholder="En az 8 karakter"
                  value={password}
                  onChange={(event) =>
                    setPassword(event.target.value)
                  }
                />

                <button
                  className="input-icon-action"
                  type="button"
                  aria-label={
                    showPassword ? "Şifreyi gizle" : "Şifreyi göster"
                  }
                  onClick={() => setShowPassword((current) => !current)}
                >
                  {showPassword ? (
                    <EyeOff size={18} />
                  ) : (
                    <Eye size={18} />
                  )}
                </button>
              </div>
            </label>

            {error && (
              <div className="auth-error" role="alert">
                {error}
              </div>
            )}

            <button
              className="button button--primary button--full button--large"
              type="submit"
              disabled={submitting}
            >
              {submitting ? (
                <>
                  <span className="spinner" />
                  Lütfen bekleyin
                </>
              ) : (
                <>
                  {mode === "login" ? "Panele giriş yap" : "Hesap oluştur"}
                  <ArrowRight size={18} />
                </>
              )}
            </button>
          </form>

          <p className="auth-footnote">
            Eğitim amaçlı kripto işlem simülasyonu
          </p>
        </div>
      </section>
    </main>
  );
}
