import { useSimulationStore } from "../../store/simulationStore";
import type { AssetAllocation } from "../../types/simulation";

type AssetFieldValue = string | number | null;

export const AssetAllocationForm = () => {
  const { assets, setAssets } = useSimulationStore();

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
          <div key={`${a.ticker}-${i}`} className="asset-row">
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
