import { useSimulationStore } from "../../store/simulationStore";
import type { AssetAllocation } from "../../types/simulation";

type AssetFieldValue = string | number | null;

/**
 * 자주 찾는 종목 프리셋.
 * 티커를 모르는 사용자도 클릭 한 번으로 시작할 수 있게 한다.
 * 장기투자 성격에 맞춰 지수 추종 + 배당주 위주로 구성.
 */
const PRESET_TICKERS: { ticker: string; name: string; tag: string }[] = [
  { ticker: "SPY", name: "S&P 500", tag: "지수" },
  { ticker: "KO", name: "코카콜라", tag: "배당" },
  { ticker: "JNJ", name: "존슨앤존슨", tag: "배당" },
  { ticker: "PG", name: "P&G", tag: "배당" },
  { ticker: "AAPL", name: "애플", tag: "성장" },
  { ticker: "MSFT", name: "마이크로소프트", tag: "성장" },
];

const TAG_COLOR: Record<string, string> = {
  지수: "#6366f1",
  배당: "#0ea5e9",
  성장: "#a855f7",
};

// 종목 수에 맞춰 비중을 100%로 균등 배분 (나머지는 첫 종목에 몰아줌)
const distributeRatios = (list: AssetAllocation[]): AssetAllocation[] => {
  const n = list.length;
  if (n === 0) return list;
  const base = Math.floor(100 / n);
  const remainder = 100 - base * n;
  return list.map((a, i) => ({
    ...a,
    ratio: i === 0 ? base + remainder : base,
  }));
};

export const AssetAllocationForm = () => {
  // 필요한 상태만 선택 구독 → 다른 상태(form/result)가 바뀌어도 리렌더되지 않음
  const assets = useSimulationStore((s) => s.assets);
  const setAssets = useSimulationStore((s) => s.setAssets);

  const handleAssetChange = (
    index: number,
    field: keyof AssetAllocation,
    value: AssetFieldValue,
  ) => {
    const updated = [...assets];
    let processedValue = value;

    if (field === "ticker" && typeof value === "string") {
      processedValue = value.toUpperCase();
    }

    if (
      (field === "ratio" ||
        field === "purchasePrice" ||
        field === "purchaseRate") &&
      typeof value === "string"
    ) {
      processedValue = value === "" ? null : Number(value);
    }

    updated[index] = {
      ...updated[index],
      [field]: processedValue,
    };
    setAssets(updated);
  };

  const addAsset = () =>
    setAssets([
      ...assets,
      {
        ticker: "",
        ratio: null,
        purchaseDate: null,
        purchasePrice: null,
        purchaseRate: null,
      },
    ]);

  /**
   * 프리셋 종목 선택
   *  - 이미 있으면 무시
   *  - 빈 티커 칸이 있으면 그 칸을 채우고, 없으면 새 행을 추가
   *  - 선택 후 비중을 100% 기준으로 균등 배분 ("원딸깍")
   */
  const addPreset = (ticker: string) => {
    if (assets.some((a) => a.ticker === ticker)) return;

    const emptyIndex = assets.findIndex((a) => !a.ticker);
    let updated: AssetAllocation[];

    if (emptyIndex >= 0) {
      updated = [...assets];
      updated[emptyIndex] = { ...updated[emptyIndex], ticker };
    } else {
      updated = [
        ...assets,
        {
          ticker,
          ratio: null,
          purchaseDate: null,
          purchasePrice: null,
          purchaseRate: null,
        },
      ];
    }

    setAssets(distributeRatios(updated));
  };

  const removeAsset = (index: number) => {
    if (assets.length <= 1) {
      alert("최소 하나의 종목은 있어야 합니다.");
      return;
    }
    setAssets(assets.filter((_, i) => i !== index));
  };

  const totalRatio = assets.reduce((sum, asset) => sum + (asset.ratio ?? 0), 0);
  const diff = 100 - totalRatio;
  const isValid = totalRatio === 100;

  return (
    <div className="asset-form-wrapper">
      <header className="asset-header">
        <h4 className="asset-title">종목 설정</h4>
        <div className={`ratio-badge ${isValid ? "valid" : "invalid"}`}>
          합계: {totalRatio}%
          {!isValid && (
            <span className="ratio-diff">
              ({diff > 0 ? `+${diff}% 부족` : `${Math.abs(diff)}% 초과`})
            </span>
          )}
        </div>
      </header>

      <div className="asset-list">
        {assets.map((a, i) => (
          <div key={i} className="asset-row">
            <div className="ticker-avatar" aria-hidden="true">
              {a.ticker ? a.ticker.charAt(0) : "?"}
            </div>

            <div className="input-box ticker">
              <input
                className="input-field"
                value={a.ticker}
                placeholder="Ticker (예: SPY)"
                onChange={(e) => handleAssetChange(i, "ticker", e.target.value)}
              />
            </div>

            <div className="input-box ratio">
              <input
                type="number"
                className="input-field"
                value={a.ratio ?? ""}
                placeholder="0"
                onChange={(e) => handleAssetChange(i, "ratio", e.target.value)}
              />
              <span className="unit-text">%</span>
            </div>

            <button
              className="remove-btn"
              onClick={() => removeAsset(i)}
              type="button"
            >
              ×
            </button>
          </div>
        ))}
      </div>

      <button onClick={addAsset} className="add-asset-btn" type="button">
        + 종목 추가
      </button>

      {/* 자주 찾는 종목 — 티커를 몰라도 클릭 한 번으로 담을 수 있게 */}
      <div style={{ marginTop: 10 }}>
        <div style={{ fontSize: 11, color: "#9aa0b0", marginBottom: 6 }}>
          자주 찾는 종목
        </div>
        <div style={{ display: "flex", flexWrap: "wrap", gap: 6 }}>
          {PRESET_TICKERS.map((p) => {
            const selected = assets.some((a) => a.ticker === p.ticker);
            const color = TAG_COLOR[p.tag] ?? "#6366f1";
            return (
              <button
                key={p.ticker}
                type="button"
                onClick={() => addPreset(p.ticker)}
                disabled={selected}
                title={`${p.name} (${p.tag})`}
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: 4,
                  border: `1px solid ${selected ? "#e5e7eb" : color}`,
                  background: selected ? "#f3f4f6" : "transparent",
                  color: selected ? "#b6bac6" : color,
                  borderRadius: 999,
                  padding: "4px 10px",
                  fontSize: 11,
                  fontWeight: 600,
                  cursor: selected ? "default" : "pointer",
                  whiteSpace: "nowrap",
                }}
              >
                {p.ticker}
                <span
                  style={{
                    fontWeight: 400,
                    fontSize: 10,
                    color: selected ? "#c9ccd6" : "#8b87ab",
                  }}
                >
                  {p.name}
                </span>
              </button>
            );
          })}
        </div>
      </div>

      {!isValid && totalRatio !== 0 && (
        <p className="error-message">
          * 비중의 합이 100%가 되도록 조정해주세요.
        </p>
      )}

      <div className="divider spacing-divider" />

      <header className="asset-header">
        <h4 className="asset-title">과거 매수 정보</h4>
      </header>
      <p className="helper-text purchase-helper">
        매수일자와 평단가를 입력한 종목은 실제 매수가 기준으로 수익을
        계산합니다. 환율은 매수일자 기준으로 자동 조회되며, 비워두면 오늘부터
        투자한 것으로 가정합니다.
      </p>

      <div className="purchase-card-list">
        {assets.map((a, i) => (
          <div key={`purchase-${a.ticker}-${i}`} className="purchase-card">
            <div className="purchase-card-ticker">
              <span className="ticker-avatar small" aria-hidden="true">
                {a.ticker ? a.ticker.charAt(0) : "?"}
              </span>
              {a.ticker || "종목 미입력"}
            </div>

            <div className="purchase-card-fields">
              <div className="purchase-field">
                <span className="purchase-field-label">매수일자</span>
                <div className="input-box purchase-date">
                  <input
                    type="date"
                    className="input-field"
                    value={a.purchaseDate ?? ""}
                    onChange={(e) =>
                      handleAssetChange(i, "purchaseDate", e.target.value)
                    }
                  />
                </div>
              </div>

              <div className="purchase-field">
                <span className="purchase-field-label">평단가 (USD)</span>
                <div className="input-box purchase-price">
                  <input
                    type="number"
                    className="input-field"
                    placeholder="예: 150.00"
                    value={a.purchasePrice ?? ""}
                    onChange={(e) =>
                      handleAssetChange(i, "purchasePrice", e.target.value)
                    }
                  />
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
