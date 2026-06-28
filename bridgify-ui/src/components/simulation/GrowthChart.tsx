import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  Line,
} from "recharts";
import type { YearlyResultResponse } from "../../types/simulation";

interface Props {
  data: YearlyResultResponse[];
}

export const PortfolioGrowthChart = ({ data }: Props) => {
  if (!data || data.length === 0) return null;

  const formatYAxis = (value: number) => {
    if (value >= 100000000) return `${(value / 100000000).toFixed(1)}억`;
    return `${Math.floor(value / 10000).toLocaleString()}만`;
  };

  // Recharts 내부 타입과 충돌을 피하기 위해 props를 any로 선언하되,
  // 내부에서 구조 분해 할당 시 타입을 강제로 지정(Type Casting)하여 안전성을 확보합니다.
  const renderLabel = (props: any, color: string) => {
    const { x, y, value, index } = props as {
      x: number;
      y: number;
      value: number;
      index: number;
    };

    // 마지막 데이터 포인트에만 라벨을 표시
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
              <stop offset="5%" stopColor="#3182ce" stopOpacity={0.15} />
              <stop offset="95%" stopColor="#3182ce" stopOpacity={0} />
            </linearGradient>

            <linearGradient id="realGrad" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor="#9f7aea" stopOpacity={0.1} />
              <stop offset="95%" stopColor="#9f7aea" stopOpacity={0} />
            </linearGradient>
          </defs>

          {/* 서비스에서 year를 숫자로 주기로 했으므로 0일 때만 "현재"로 처리 */}
          <XAxis
            dataKey="year"
            tickFormatter={(v) => (v === 0 || v === "현재" ? "현재" : `${v}년`)}
          />

          <YAxis tickFormatter={formatYAxis} />

          <Tooltip
            formatter={(value: number) => [`${value.toLocaleString()}원`]}
            labelFormatter={(label) =>
              label === 0 || label === "현재" ? "현재" : `${label}년차`
            }
          />

          <Area
            type="monotone"
            dataKey="realBalanceKrw"
            stroke="#9f7aea"
            fill="url(#realGrad)"
            strokeWidth={3}
            // 여기서 발생하던 타입 에러를 해결했습니다.
            label={(p) => renderLabel(p, "#9f7aea")}
          />

          <Area
            type="monotone"
            dataKey="nominalBalanceKrw"
            stroke="#3182ce"
            fill="url(#nominalGrad)"
            strokeWidth={3}
            label={(p) => renderLabel(p, "#3182ce")}
          />

          <Line
            dataKey="principal"
            stroke="#cbd5e1"
            strokeDasharray="5 5"
            dot={false}
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
};
