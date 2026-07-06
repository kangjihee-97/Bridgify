import type { SimulationResponse } from "../../types/simulation";
import { formatKoreanCurrency } from "../../utils/format";

interface Props {
  result: SimulationResponse;
}

// 미래 시뮬 결과를 "원금 / 순수익 / 세금" 누적 비율 막대로 보여준다.
export const TaxBar = ({ result }: Props) => {
  const totalBalance = Math.max(0, result.nominalBalanceKrw || 0);
  const profit = Math.max(0, result.totalProfit || 0);
  const tax = Math.max(0, result.tax || 0);

  const netProfit = Math.max(0, profit - tax);
  const principal = Math.max(0, totalBalance - profit);
  const total = principal + netProfit + tax;

  const segments = [
    { name: "투자 원금", value: principal, color: "#cbd5e1" },
    { name: "순수익", value: netProfit, color: "#6366f1" },
    { name: "세금", value: tax, color: "#a855f7" },
  ];

  const pct = (v: number) => (total > 0 ? (v / total) * 100 : 0);

  if (total <= 0) {
    return (
      <p style={{ fontSize: 12, color: "#8b87ab", textAlign: "center" }}>
        표시할 데이터가 없습니다.
      </p>
    );
  }

  return (
    <div>
      {/* 누적 비율 막대 */}
      <div
        style={{
          display: "flex",
          width: "100%",
          height: 22,
          borderRadius: 11,
          overflow: "hidden",
          background: "#eef0f5",
        }}
      >
        {segments.map((s, i) =>
          s.value > 0 ? (
            <div
              key={i}
              style={{
                width: `${pct(s.value)}%`,
                background: s.color,
              }}
              title={`${s.name} ${formatKoreanCurrency(s.value)}`}
            />
          ) : null,
        )}
      </div>

      {/* 범례 + 금액 + 비율 */}
      <div style={{ marginTop: 12 }}>
        {segments.map((s, i) => (
          <div
            key={i}
            style={{
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
              fontSize: 12,
              padding: "5px 0",
            }}
          >
            <span style={{ display: "flex", alignItems: "center", gap: 6 }}>
              <span
                style={{
                  width: 10,
                  height: 10,
                  borderRadius: 3,
                  background: s.color,
                  display: "inline-block",
                }}
              />
              {s.name}
            </span>
            <span style={{ fontWeight: 600 }}>
              {formatKoreanCurrency(s.value)}{" "}
              <span style={{ color: "#8b87ab", fontWeight: 400 }}>
                ({pct(s.value).toFixed(1)}%)
              </span>
            </span>
          </div>
        ))}
      </div>
    </div>
  );
};
