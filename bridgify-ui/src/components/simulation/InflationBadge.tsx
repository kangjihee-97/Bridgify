import { useEffect, useState } from "react";
import { fetchInflationRates } from "../../api/simulationApi";

/**
 * 결과 카드용 물가 반영 배지.
 *
 * "실질 구매력"이 어떻게 나온 값인지 한 줄로 설명한다.
 *  - 미국 물가: 주가(기업 실적)와 환율에 이미 반영됨
 *  - 한국 물가: 최종 원화의 구매력을 직접 차감
 *
 * 사용자가 "미국 물가는 왜 안 쓰였지?" 하고 오해하지 않도록,
 * 두 물가가 모두 계산에 관여했음을 보여준다.
 */
export const InflationBadge = () => {
  const [rates, setRates] = useState<{
    usInflationRate: number;
    krInflationRate: number;
  } | null>(null);

  useEffect(() => {
    fetchInflationRates()
      .then(setRates)
      .catch(() => setRates(null));
  }, []);

  if (!rates) return null;

  return (
    <div
      style={{
        marginTop: 10,
        padding: "8px 12px",
        background: "#f7f6fe",
        border: "1px solid #e5e0fa",
        borderRadius: 8,
        fontSize: 11,
        color: "#5b5570",
        lineHeight: 1.7,
        display: "flex",
        flexWrap: "wrap",
        alignItems: "center",
        gap: 6,
      }}
    >
      <strong style={{ color: "#4b3fa3" }}>물가 반영 내역</strong>
      <span style={{ color: "#c9c4dd" }}>|</span>
      <span>
        🇺🇸 미국 <strong>{rates.usInflationRate}%</strong>
        <span style={{ color: "#8b87ab" }}> → 주가·환율</span>
      </span>
      <span style={{ color: "#c9c4dd" }}>+</span>
      <span>
        🇰🇷 한국 <strong>{rates.krInflationRate}%</strong>
        <span style={{ color: "#8b87ab" }}> → 구매력 차감</span>
      </span>
      <span style={{ color: "#8b87ab" }}>= 실질 구매력</span>
    </div>
  );
};
