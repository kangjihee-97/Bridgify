import { useEffect, useState } from "react";
import { fetchInflationRates } from "../../api/simulationApi";
import { useSimulationStore } from "../../store/simulationStore";

/**
 * 물가 상승률 입력칸 밑에 실시간 물가(미국·한국)를 보여주고,
 * 클릭하면 해당 값을 물가 입력칸(krInflationRate)에 자동으로 채워준다.
 */
export const InflationHint = () => {
  const { setForm } = useSimulationStore();
  const [rates, setRates] = useState<{
    usInflationRate: number;
    krInflationRate: number;
  } | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchInflationRates()
      .then(setRates)
      .catch((e) => console.error("물가 조회 실패:", e))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <p style={{ fontSize: 11, color: "#9aa0b0", margin: "4px 0 0" }}>
        실시간 물가 불러오는 중...
      </p>
    );
  }

  if (!rates) return null;

  const chip = (label: string, value: number, color: string) => (
    <button
      type="button"
      onClick={() => setForm("krInflationRate" as any, value)}
      style={{
        border: `1px solid ${color}`,
        background: "transparent",
        color,
        borderRadius: 999,
        padding: "3px 10px",
        fontSize: 11,
        fontWeight: 600,
        cursor: "pointer",
      }}
      title="클릭하면 물가 상승률에 적용됩니다"
    >
      {label} {value}%
    </button>
  );

  return (
    <div style={{ marginTop: 6 }}>
      <div style={{ fontSize: 10, color: "#9aa0b0", marginBottom: 4 }}>
        실시간 물가 (클릭해 적용)
      </div>
      <div style={{ display: "flex", gap: 6 }}>
        {chip("🇰🇷 한국", rates.krInflationRate, "#6366f1")}
        {chip("🇺🇸 미국", rates.usInflationRate, "#a855f7")}
      </div>
    </div>
  );
};
