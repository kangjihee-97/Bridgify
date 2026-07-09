import { memo } from "react";
import {
  PieChart,
  Pie,
  Cell,
  ResponsiveContainer,
  Tooltip,
  Legend,
} from "recharts";
import type { SimulationResponse } from "../../types/simulation";

interface Props {
  result: SimulationResponse;
}

const TaxPieChartBase = ({ result }: Props) => {
  const totalBalance = result.nominalBalanceKrw || 0;
  const safeProfit = Math.max(0, result.totalProfit || 0);
  const totalTax = Math.max(0, result.tax || 0);

  const netProfit = Math.max(0, safeProfit - totalTax);
  const principal = Math.max(0, totalBalance - safeProfit);

  const data = [
    { name: "투자 원금", value: principal, color: "#cbd5e1" },
    { name: "순수익", value: netProfit, color: "#6366f1" },
    { name: "예상 세금", value: totalTax, color: "#a855f7" },
  ];

  return (
    /* 1. 스타일을 pie-chart-container 클래스로 교체 */
    <div className="pie-chart-container">
      <ResponsiveContainer>
        <PieChart>
          <Pie
            data={data}
            innerRadius={60}
            outerRadius={80}
            paddingAngle={5}
            dataKey="value"
            stroke="none"
            cx="50%"
            cy="45%"
          >
            {data.map((entry, index) => (
              <Cell key={index} fill={entry.color} />
            ))}
          </Pie>

          <Tooltip
            formatter={(value: any) => `${Number(value).toLocaleString()}원`}
          />

          <Legend verticalAlign="bottom" align="center" iconType="circle" />

          {/* 2. 텍스트 스타일을 클래스로 관리 (SVG는 className 지원함) */}
          <text x="50%" y="45%" textAnchor="middle" dominantBaseline="middle">
            <tspan x="50%" dy="-10" className="chart-label-title">
              총 세금
            </tspan>
            <tspan x="50%" dy="22" className="chart-label-value">
              {totalTax >= 10000
                ? `${(totalTax / 10000).toLocaleString(undefined, {
                    maximumFractionDigits: 1,
                  })}만`
                : totalTax.toLocaleString()}
              원
            </tspan>
          </text>
        </PieChart>
      </ResponsiveContainer>
    </div>
  );
};

// 부모(SimulationPage)가 리렌더돼도 props가 같으면 다시 그리지 않는다.
// → 종목 입력창에 타이핑할 때 차트가 매번 재렌더되는 문제를 막는다.
export const TaxPieChart = memo(TaxPieChartBase);
