import { useState } from "react";
import { useSimulationStore } from "../store/simulationStore";
import { runSimulationApi } from "../api/simulationApi";

import DashboardLayout from "../components/layout/DashboardLayout";
import Card from "../components/ui/Card";
import { Button } from "../components/ui/Button";
import { StatBox } from "../components/ui/StatBox";

import { AssetAllocationForm } from "../components/simulation/SimulationForm";
import { SimulationSettingsForm } from "../components/simulation/SettingsForm";
import { SimulationResultSection } from "../components/simulation/SimulationResultSection";
import { GoodsComparisonCard } from "../components/simulation/GoodsComparison";
import { TaxPieChart } from "../components/simulation/TaxPieChart";

import { formatKoreanCurrency } from "../utils/format";

import "../styles/dashboard.css";

export default function SimulationPage() {
  const { result, form, assets, setResult } = useSimulationStore();

  const [isCalculating, setIsCalculating] = useState(false);

  const handleRun = async () => {
    setIsCalculating(true);

    try {
      const data = await runSimulationApi({
        ...form,
        assets,
      } as any);

      setResult(data);
    } catch (error) {
      console.error("시뮬레이션 실패:", error);

      alert("서버 연결에 실패했습니다. 백엔드 주소나 네트워크를 확인하세요.");
    } finally {
      setIsCalculating(false);
    }
  };

  return (
    <DashboardLayout>
      <div className="dashboard-wrapper">
        {/* 좌측 입력 패널 */}
        <aside className="panel left-panel">
          <Card title="1. 시뮬레이션 입력" variant="purple">
            <div className="input-section">
              <AssetAllocationForm />

              <div className="divider spacing-divider" />

              <SimulationSettingsForm />

              <Button
                onClick={handleRun}
                className="run-button"
                disabled={isCalculating}
              >
                {isCalculating ? "계산 중..." : "시뮬레이션 실행"}
              </Button>
            </div>
          </Card>
        </aside>

        {/* 중앙 결과 패널 */}
        <main className="panel center-panel">
          {result ? (
            <div className="report-stack">
              <Card title="2. 자산 구매력 환산 리포트" variant="blue">
                <div className="stat-group">
                  <StatBox
                    title="최종 명목 자산"
                    value={formatKoreanCurrency(result.nominalBalanceKrw)}
                    variant="purple"
                  />

                  <div className="stat-vs-badge">VS</div>

                  <StatBox
                    title="실질 구매력 가치"
                    value={formatKoreanCurrency(result.realBalanceKrw)}
                    variant="blue"
                    subValue={`물가상승률 ${form.krInflationRate}% 반영`}
                  />
                </div>

                <p className="value-statement">
                  {form.durationYears}년 뒤 내 돈{" "}
                  <strong className="value-statement-nominal">
                    {formatKoreanCurrency(result.nominalBalanceKrw)}
                  </strong>
                  은, 현재의{" "}
                  <strong className="value-statement-real">
                    {formatKoreanCurrency(result.realBalanceKrw)}
                  </strong>
                  의 가치를 가집니다.
                </p>

                <GoodsComparisonCard result={result} />
              </Card>

              <Card title="자산 성장 추이 추정">
                <SimulationResultSection
                  results={result.yearlyResults}
                  request={form as any}
                />
              </Card>
            </div>
          ) : (
            <div className="empty-placeholder">
              <p>
                {isCalculating
                  ? "서버에서 데이터를 계산하고 있습니다..."
                  : "시뮬레이션 조건을 입력하고 버튼을 눌러주세요."}
              </p>
            </div>
          )}
        </main>

        {/* 우측 요약 패널 */}
        <aside className="panel right-panel">
          {result && (
            <div className="summary-stack">
              <Card title="3. 결과 요약" variant="green">
                <StatBox
                  title="누적 수익률"
                  value={`${result.returnRate}%`}
                  variant="green"
                />

                <div className="summary-list-content">
                  <div className="summary-item">
                    <span className="summary-label">총 투자 원금</span>
                    <span className="summary-value">
                      {formatKoreanCurrency(result.totalPrincipal)}
                    </span>
                  </div>

                  <div className="summary-item">
                    <span className="summary-label">최종 명목 자산</span>
                    <span className="summary-value">
                      {formatKoreanCurrency(result.nominalBalanceKrw)}
                    </span>
                  </div>

                  <div className="summary-item">
                    <span className="summary-label">실질 구매력 가치</span>
                    <span className="summary-value">
                      {formatKoreanCurrency(result.realBalanceKrw)}
                    </span>
                  </div>

                  <div className="summary-item">
                    <span className="summary-label">총 예상 수익</span>
                    <span className="summary-value">
                      {formatKoreanCurrency(result.totalProfit)}
                    </span>
                  </div>

                  <div className="summary-item">
                    <span className="summary-label">총 예상 세금</span>
                    <span className="summary-value negative">
                      -{formatKoreanCurrency(result.tax ?? 0)}
                    </span>
                  </div>

                  <div className="summary-item">
                    <span className="summary-label">투자 기간</span>
                    <span className="summary-value">
                      {form.durationYears}년
                    </span>
                  </div>

                  <div className="summary-item">
                    <span className="summary-label">물가 상승률</span>
                    <span className="summary-value">
                      {form.krInflationRate}%
                    </span>
                  </div>
                </div>
              </Card>

              <Card title="세금 및 비용 구성">
                <div className="chart-container">
                  <TaxPieChart result={result} />
                </div>
              </Card>
            </div>
          )}
        </aside>
      </div>
    </DashboardLayout>
  );
}
