import { useSimulationStore } from "../../store/simulationStore";
import type { AssetAllocation } from "../../types/simulation";

export const AssetAllocationForm = () => {
  const { assets, setAssets } = useSimulationStore();

  const handleAssetChange = (
    index: number,
    field: keyof AssetAllocation,
    value: string,
  ) => {
    const updated = [...assets];
    const processedValue = field === "ticker" ? value.toUpperCase() : value;

    updated[index] = {
      ...updated[index],
      [field]:
        field === "ratio"
          ? processedValue === ""
            ? null
            : Number(processedValue)
          : processedValue,
    };
    setAssets(updated);
  };

  const addAsset = () => setAssets([...assets, { ticker: "", ratio: null }]);

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
        <h4 className="asset-title">자산 구성</h4>
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
    </div>
  );
};
