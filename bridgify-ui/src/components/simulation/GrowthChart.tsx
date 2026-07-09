import { memo } from "react";
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  Line,
  CartesianGrid,
  Legend,
} from "recharts";
import type { YearlyResultResponse } from "../../types/simulation";

interface Props {
  data: YearlyResultResponse[];
}

const PortfolioGrowthChartBase = ({ data }: Props) => {
  if (!data || data.length === 0) return null;

  // Y축·라벨 공통 포맷 — 단위를 "억"으로 통일해서 깔끔하게
  const formatYAxis = (value: number) => {
    if (!value) return "0";
    const eok = value / 100000000;
    return eok >= 1 ? `${eok.toFixed(1)}억` : `${eok.toFixed(2)}억`;
  };

  const renderLabel = (props: any, color: string) => {
    const { x, y, value, index } = props as {
      x: number;
      y: number;
      value: number;
      index: number;
    };

    if (index !== data.length - 1) return null;

    return (
      <g>
        <rect
          x={x + 5}
          y={y - 15}
          width={80}
          height={26}
          rx={13}
          fill={color}
        />
        <text
          x={x + 45}
          y={y + 3}
          textAnchor="middle"
          fontSize="11"
          fill="#fff"
          fontWeight="700"
        >
          {formatYAxis(value)}
        </text>
      </g>
    );
  };

  return (
    <div style={{ width: "100%", height: "280px" }}>
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart
          data={data}
          margin={{ top: 20, right: 90, left: 0, bottom: 20 }}
        >
          <defs>
            <linearGradient id="nominalGrad" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor="#6366f1" stopOpacity={0.18} />
              <stop offset="95%" stopColor="#6366f1" stopOpacity={0} />
            </linearGradient>

            <linearGradient id="realGrad" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor="#a855f7" stopOpacity={0.15} />
              <stop offset="95%" stopColor="#a855f7" stopOpacity={0} />
            </linearGradient>
          </defs>

          {/* 은은한 가로 격자선 — 값 읽기 쉽게 */}
          <CartesianGrid
            vertical={false}
            stroke="#eef0f5"
            strokeDasharray="0"
          />

          <XAxis
            dataKey="year"
            tickFormatter={(v) => (v === 0 || v === "현재" ? "현재" : `${v}년`)}
            tick={{ fontSize: 12, fill: "#9aa0b0" }}
            axisLine={{ stroke: "#e5e7eb" }}
            tickLine={false}
          />

          <YAxis
            tickFormatter={formatYAxis}
            tick={{ fontSize: 12, fill: "#9aa0b0" }}
            axisLine={false}
            tickLine={false}
            width={48}
          />

          <Tooltip
            formatter={(value) => [`${Number(value).toLocaleString()}원`]}
            labelFormatter={(label) =>
              label === 0 || label === "현재" ? "현재" : `${label}년차`
            }
          />

          <Legend
            verticalAlign="top"
            align="right"
            iconType="plainline"
            wrapperStyle={{ fontSize: 12, paddingBottom: 8 }}
          />

          <Area
            type="monotone"
            name="명목 잔고"
            dataKey="nominalBalanceKrw"
            stroke="#6366f1"
            fill="url(#nominalGrad)"
            strokeWidth={2}
            dot={false}
            label={(p) => renderLabel(p, "#6366f1")}
          />

          <Area
            type="monotone"
            name="실질 구매력"
            dataKey="realBalanceKrw"
            stroke="#a855f7"
            fill="url(#realGrad)"
            strokeWidth={2}
            dot={false}
            label={(p) => renderLabel(p, "#a855f7")}
          />

          <Line
            type="monotone"
            name="투자 원금"
            dataKey="principal"
            stroke="#cbd5e1"
            strokeDasharray="5 5"
            strokeWidth={1.5}
            dot={false}
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
};

// 부모(SimulationPage)가 리렌더돼도 props가 같으면 다시 그리지 않는다.
// → 종목 입력창에 타이핑할 때 차트가 매번 재렌더되는 문제를 막는다.
export const PortfolioGrowthChart = memo(PortfolioGrowthChartBase);
