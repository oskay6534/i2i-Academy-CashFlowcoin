import { Landmark } from "lucide-react";

export function Brand({ compact = false }: { compact?: boolean }) {
  return (
    <div className={`brand ${compact ? "brand--compact" : ""}`}>
      <div className="brand__mark" aria-hidden="true">
        <span className="brand__coin-line" />
        <Landmark size={19} />
      </div>

      <div>
        <strong>CashFlowCoin</strong>
        {!compact && <span>Piyasa ve portföy yönetimi</span>}
      </div>
    </div>
  );
}