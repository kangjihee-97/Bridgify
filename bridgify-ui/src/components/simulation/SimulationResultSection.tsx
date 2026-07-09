import { useMemo, memo } from "react";
import { PortfolioGrowthChart } from "./GrowthChart";
import { formatKoreanCurrency } from "../../utils/format";
import type {
  YearlyResultResponse,
  SimulationRequest,
} from "../../types/simulation";

interface Props {
  results: YearlyResultResponse[];
  request: SimulationRequest;
}

const SimulationResultSectionBase = ({ results, request }: Props) => {
  if (!results || results.length === 0) return null;

  const lastResult = results[results.length - 1];
  if (!lastResult) return null;

  const initial = Number(request.initialAmount) || 0;
  const monthly = Number(request.monthlyDeposit) || 0;
  const years = Number(request.durationYears) || results.length;

  const totalPrincipal = useMemo(() => {
    return initial + monthly * 12 * years;
  }, [initial, monthly, years]);

  const totalProfit = lastResult.nominalBalanceKrw - totalPrincipal;

  const chartData = useMemo(() => {
    return results.map((item) => ({
      ...item,
      principal: initial + monthly * 12 * item.year,
    }));
  }, [results, initial, monthly]);

  const metrics = useMemo(() => {
    if (totalPrincipal === 0) return { profitRate: 0, realProfitRate: 0 };

    return {
      profitRate: (totalProfit / totalPrincipal) * 100,
      realProfitRate:
        ((lastResult.realBalanceKrw - totalPrincipal) / totalPrincipal) * 100,
    };
  }, [totalProfit, totalPrincipal, lastResult]);

  return (
    <div className="chart-section-card">
      <div className="chart-container">
        <PortfolioGrowthChart data={chartData} />
      </div>

      <div className="result-summary-grid">
        <div className="summary-item-box">
          <p className="summary-label">최종 명목 잔고</p>
          <h3 className="summary-value">
            {formatKoreanCurrency(lastResult.nominalBalanceKrw)}
          </h3>
          <span className="summary-badge profit">
            수익률 +{metrics.profitRate.toFixed(1)}%
          </span>
        </div>

        <div className="summary-item-box">
          <p className="summary-label">실질 구매력 가치</p>
          <h3 className="summary-value real">
            {formatKoreanCurrency(lastResult.realBalanceKrw)}
          </h3>
          <span className="summary-badge real-profit">
            실질 수익률 {metrics.realProfitRate.toFixed(1)}%
          </span>
        </div>

        <div className="summary-item-box">
          <p className="summary-label">총 투자 원금</p>
          <h3 className="summary-value">
            {formatKoreanCurrency(totalPrincipal)}
          </h3>
        </div>
      </div>
    </div>
  );
};

// 부모(SimulationPage)가 리렌더돼도 props가 같으면 다시 그리지 않는다.
// → 종목 입력창에 타이핑할 때 차트가 매번 재렌더되는 문제를 막는다.
export const SimulationResultSection = memo(SimulationResultSectionBase);
