import { memo } from "react";
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from "recharts";
import { formatKoreanCurrency } from "../../utils/format";

interface Props {
  capitalGainsTaxKrw: number;
  dividendTaxKrw: number;
}

const RealizedTaxDonutBase = ({
  capitalGainsTaxKrw,
  dividendTaxKrw,
}: Props) => {
  const capital = Math.max(0, capitalGainsTaxKrw || 0);
  const dividend = Math.max(0, dividendTaxKrw || 0);
  const totalTax = capital + dividend;

  const data = [
    { name: "양도소득세", value: capital, color: "#6366f1" },
    { name: "배당소득세", value: dividend, color: "#a855f7" },
  ];

  // 세금이 아예 없으면 안내만
  if (totalTax <= 0) {
    return (
      <p
        style={{
          fontSize: 12,
          color: "#8b87ab",
          textAlign: "center",
          margin: "12px 0",
        }}
      >
        공제 범위 내라 부과된 세금이 없습니다.
      </p>
    );
  }

  const pct = (v: number) =>
    totalTax > 0 ? `${((v / totalTax) * 100).toFixed(1)}%` : "0%";

  return (
    <div>
      {/* 도넛 */}
      <div style={{ position: "relative", width: "100%", height: 180 }}>
        <ResponsiveContainer>
          <PieChart>
            <Pie
              data={data}
              innerRadius={55}
              outerRadius={78}
              paddingAngle={4}
              dataKey="value"
              stroke="none"
              cx="50%"
              cy="50%"
            >
              {data.map((entry, index) => (
                <Cell key={index} fill={entry.color} />
              ))}
            </Pie>
            <Tooltip
              formatter={(value: any) => formatKoreanCurrency(Number(value))}
            />
          </PieChart>
        </ResponsiveContainer>

        {/* 도넛 가운데 텍스트 (SVG 대신 겹쳐 놓기) */}
        <div
          style={{
            position: "absolute",
            top: "50%",
            left: "50%",
            transform: "translate(-50%, -50%)",
            textAlign: "center",
            pointerEvents: "none",
          }}
        >
          <div style={{ fontSize: 11, color: "#8b87ab" }}>총 세금</div>
          <div style={{ fontSize: 15, fontWeight: 700, color: "#4b3fa3" }}>
            {formatKoreanCurrency(totalTax)}
          </div>
        </div>
      </div>

      {/* 범례 + 금액 + 비율 (시안 스타일) */}
      <div style={{ marginTop: 12 }}>
        {data.map((d, i) => (
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
                  borderRadius: "50%",
                  background: d.color,
                  display: "inline-block",
                }}
              />
              {d.name}
            </span>
            <span style={{ fontWeight: 600 }}>
              {formatKoreanCurrency(d.value)}{" "}
              <span style={{ color: "#8b87ab", fontWeight: 400 }}>
                ({pct(d.value)})
              </span>
            </span>
          </div>
        ))}
      </div>
    </div>
  );
};

// 부모(SimulationPage)가 리렌더돼도 props가 같으면 다시 그리지 않는다.
// → 종목 입력창에 타이핑할 때 차트가 매번 재렌더되는 문제를 막는다.
export const RealizedTaxDonut = memo(RealizedTaxDonutBase);
