import {
  ArrowDownLeft,
  ArrowUpRight,
  Calculator,
  X,
} from "lucide-react";
import {
  type FormEvent,
  useEffect,
  useMemo,
  useState,
} from "react";
import type {
  MarketPrice,
  Portfolio,
  TradeSide,
} from "../types";

interface TradeModalProps {
  coin: MarketPrice;
  side: TradeSide;
  portfolio: Portfolio;
  submitting: boolean;
  onClose: () => void;
  onSubmit: (
    side: TradeSide,
    symbol: string,
    quantity: number,
  ) => Promise<void>;
}

function formatMoney(value: number) {
  return new Intl.NumberFormat("tr-TR", {
    style: "currency",
    currency: "USD",
    maximumFractionDigits: 2,
  }).format(value);
}

export function TradeModal({
  coin,
  side,
  portfolio,
  submitting,
  onClose,
  onSubmit,
}: TradeModalProps) {
  const [quantity, setQuantity] = useState("");
  const numericQuantity = Number(quantity || 0);

  const ownedQuantity =
    portfolio.assets.find((asset) => asset.symbol === coin.symbol)
      ?.quantity ?? 0;

  const maximumQuantity =
    side === "BUY"
      ? portfolio.cashBalance / coin.price
      : ownedQuantity;

  const estimatedTotal = useMemo(
    () => numericQuantity * coin.price,
    [numericQuantity, coin.price],
  );

  const isInvalid =
    !Number.isFinite(numericQuantity) ||
    numericQuantity <= 0 ||
    numericQuantity > maximumQuantity;

  useEffect(() => {
    function handleEscape(event: KeyboardEvent) {
      if (event.key === "Escape" && !submitting) {
        onClose();
      }
    }

    document.addEventListener("keydown", handleEscape);
    document.body.classList.add("modal-open");

    return () => {
      document.removeEventListener("keydown", handleEscape);
      document.body.classList.remove("modal-open");
    };
  }, [onClose, submitting]);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();

    if (isInvalid) {
      return;
    }

    await onSubmit(side, coin.symbol, numericQuantity);
  }

  function fillMaximum() {
    if (maximumQuantity <= 0) {
      return;
    }

    setQuantity(
      Math.max(0, maximumQuantity).toFixed(8).replace(/0+$/, "").replace(/\.$/, ""),
    );
  }

  const ActionIcon = side === "BUY" ? ArrowDownLeft : ArrowUpRight;

  return (
    <div
      className="modal-backdrop"
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !submitting) {
          onClose();
        }
      }}
    >
      <section
        className="trade-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="trade-modal-title"
      >
        <div className="trade-modal__header">
          <div
            className={`trade-modal__icon ${
              side === "BUY"
                ? "trade-modal__icon--buy"
                : "trade-modal__icon--sell"
            }`}
          >
            <ActionIcon size={22} />
          </div>
          <div>
            <span className="eyebrow">
              {side === "BUY" ? "Alış emri" : "Satış emri"}
            </span>
            <h2 id="trade-modal-title">{coin.symbol} işlemi</h2>
          </div>
          <button
            className="icon-button"
            type="button"
            onClick={onClose}
            disabled={submitting}
            aria-label="Pencereyi kapat"
          >
            <X size={20} />
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="trade-quote">
            <div>
              <span>Güncel fiyat</span>
              <strong>{formatMoney(coin.price)}</strong>
            </div>
            <div>
              <span>
                {side === "BUY"
                  ? "Kullanılabilir bakiye"
                  : "Mevcut varlık"}
              </span>
              <strong>
                {side === "BUY"
                  ? formatMoney(portfolio.cashBalance)
                  : `${ownedQuantity.toFixed(8)} ${coin.symbol}`}
              </strong>
            </div>
          </div>

          <label className="field">
            <span className="field__label">Miktar</span>
            <div className="input-shell input-shell--with-action">
              <input
                autoFocus
                type="number"
                min="0.00000001"
                max={maximumQuantity}
                step="0.00000001"
                inputMode="decimal"
                placeholder="0.00"
                value={quantity}
                onChange={(event) => setQuantity(event.target.value)}
              />
              <button
                type="button"
                className="input-action"
                onClick={fillMaximum}
              >
                Maks.
              </button>
            </div>
            <small>
              En fazla {maximumQuantity.toFixed(8)} {coin.symbol}
            </small>
          </label>

          <div className="estimate-card">
            <Calculator size={19} />
            <div>
              <span>Tahmini işlem tutarı</span>
              <strong>{formatMoney(estimatedTotal)}</strong>
            </div>
          </div>

          {numericQuantity > maximumQuantity && (
            <p className="form-error">
              {side === "BUY"
                ? "Bu işlem için bakiyeniz yetersiz."
                : "Portföyünüzde bu kadar varlık bulunmuyor."}
            </p>
          )}

          <button
            className={`button button--full ${
              side === "BUY"
                ? "button--primary"
                : "button--danger"
            }`}
            type="submit"
            disabled={isInvalid || submitting}
          >
            {submitting ? (
              <>
                <span className="spinner" />
                İşlem gerçekleştiriliyor
              </>
            ) : (
              <>
                <ActionIcon size={18} />
                Emri gerçekleştir
              </>
            )}
          </button>
        </form>
      </section>
    </div>
  );
}