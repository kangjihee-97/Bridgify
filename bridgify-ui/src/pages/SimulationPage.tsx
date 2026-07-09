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

import { TaxBar } from "../components/simulation/TaxBar";

import { formatKoreanCurrency } from "../utils/format";
import { RealizedProfitSection } from "../components/simulation/RealizedProfitSection";
import { AiSummaryCard } from "../components/simulation/AiSummaryCard";
import { InflationBadge } from "../components/simulation/InflationBadge";
import "../styles/dashboard.css";

export default function SimulationPage() {
  // 스토어를 통째로 구조분해하면 어떤 상태가 바뀌어도 이 페이지(=모든 자식)가 리렌더된다.
  // 필요한 값만 개별 선택 구독해서, 타이핑할 때 차트까지 다시 그리는 일을 막는다.
  const result = useSimulationStore((s) => s.result);
  const form = useSimulationStore((s) => s.form);
  const assets = useSimulationStore((s) => s.assets);
  const setResult = useSimulationStore((s) => s.setResult);
  const setAssets = useSimulationStore((s) => s.setAssets);
  const setForm = useSimulationStore((s) => s.setForm);

  const [isCalculating, setIsCalculating] = useState(false);
  const [toast, setToast] = useState<string | null>(null);

  const showToast = (msg: string) => {
    setToast(msg);
    setTimeout(() => setToast(null), 3000);
  };

  const handleRun = async () => {
    setIsCalculating(true);
    try {
      const data = await runSimulationApi({ ...form, assets } as any);
      setResult(data);
    } catch (error) {
      console.error("시뮬레이션 실패:", error);
      alert("서버 연결에 실패했습니다. 백엔드 주소나 네트워크를 확인하세요.");
    } finally {
      setIsCalculating(false);
    }
  };

  const handleReset = () => {
    setResult(null);
    setAssets([
      {
        ticker: "",
        ratio: null,
        purchaseDate: null,
        purchasePrice: null,
        purchaseRate: null,
      },
    ]);
    setForm("initialAmount", 1000000);
    setForm("monthlyDeposit", 500000);
    setForm("durationYears", 10);
    setForm("krInflationRate", 3);
    setForm("taxRate", 0.15);
    setForm("expectedReturn", null as any);
  };

  const handleSave = () => {
    if (!result) return;
    const simId = `SIM-${result.configId?.toString().padStart(6, "0")}`;
    showToast(`저장 완료! ${simId} 번호로 조회하세요.`);
  };

  return (
    <DashboardLayout
      onReset={handleReset}
      onSave={handleSave}
      onPdf={() => window.print()}
    >
      {/* 토스트 메시지 */}
      {toast && (
        <div
          style={{
            position: "fixed",
            top: "20px",
            left: "50%",
            transform: "translateX(-50%)",
            background: "#3730a3",
            color: "#fff",
            padding: "12px 24px",
            borderRadius: "12px",
            fontSize: "14px",
            fontWeight: 600,
            zIndex: 9999,
            boxShadow: "0 4px 20px rgba(0,0,0,0.2)",
          }}
        >
          {toast}
        </div>
      )}
      <div className="dashboard-wrapper">
        {/* 좌측 입력 패널 */}
        <aside className="panel left-panel" style={{ alignSelf: "start" }}>
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

                {/* 미국·한국 물가가 각각 어디에 반영됐는지 표시 */}
                <InflationBadge />

                <GoodsComparisonCard result={result} />
              </Card>

              {/* AI 결과 해설 — 명목 vs 실질 숫자를 본 직후 해설을 읽도록 배치 */}
              <AiSummaryCard result={result} />

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

        {/* 우측 패널 - 실현손익(항상 표시) + 시뮬 실행 시 요약들 */}
        <aside className="panel right-panel">
          <div className="summary-stack">
            {/* 실현손익 정산 - 미래 시뮬 실행과 무관하게 항상 표시 */}
            <RealizedProfitSection />

            {result && (
              <>
                <Card title="시뮬레이션 정보">
                  <div className="summary-list-content">
                    <div className="summary-item">
                      <span className="summary-label">시뮬레이션 ID</span>
                      <span
                        className="summary-value"
                        style={{
                          fontSize: "12px",
                          color: "#6366f1",
                          fontWeight: 600,
                        }}
                      >
                        SIM-{result.configId?.toString().padStart(6, "0")}
                      </span>
                    </div>
                    <div className="summary-item">
                      <span className="summary-label">생성일</span>
                      <span
                        className="summary-value"
                        style={{ fontSize: "12px" }}
                      >
                        {new Date().toLocaleDateString("ko-KR", {
                          year: "numeric",
                          month: "2-digit",
                          day: "2-digit",
                          hour: "2-digit",
                          minute: "2-digit",
                        })}
                      </span>
                    </div>
                  </div>
                </Card>

                {/* 투자 성과 요약 — 위 카드와 중복되는 명목/실질 자산은 제외 */}
                <Card title="투자 성과 요약">
                  <div className="summary-list-content">
                    <div className="summary-item">
                      <span className="summary-label">총 투자 원금</span>
                      <span className="summary-value">
                        {formatKoreanCurrency(result.totalPrincipal)}
                      </span>
                    </div>
                    <div className="summary-item">
                      <span className="summary-label">총 투자 수익(명목)</span>
                      <span className="summary-value nominal">
                        {formatKoreanCurrency(result.totalProfit)}
                      </span>
                    </div>
                    <div className="summary-item">
                      <span className="summary-label">연평균 수익률</span>
                      <span className="summary-value nominal">
                        {result.returnRate}%
                      </span>
                    </div>
                  </div>
                </Card>

                <Card title="주요 가정 요약">
                  <div className="summary-list-content">
                    <div className="summary-item">
                      <span className="summary-label">양도소득세율</span>
                      <span className="summary-value">
                        {((form.taxRate ?? 0) * 100).toFixed(0)} %
                      </span>
                    </div>
                    <div className="summary-item">
                      <span className="summary-label">초기 투자금</span>
                      <span className="summary-value">
                        {formatKoreanCurrency(form.initialAmount)}
                      </span>
                    </div>
                    <div className="summary-item">
                      <span className="summary-label">월 적립금</span>
                      <span className="summary-value">
                        {formatKoreanCurrency(form.monthlyDeposit)}
                      </span>
                    </div>
                    <div className="summary-item">
                      <span className="summary-label">예상 연 수익률</span>
                      <span className="summary-value">
                        {form.expectedReturn
                          ? `${form.expectedReturn} %`
                          : "종목 자동 계산"}
                      </span>
                    </div>
                    {assets
                      .filter((a) => a.ticker && a.purchaseRate)
                      .map((a, i) => (
                        <div className="summary-item" key={i}>
                          <span className="summary-label">
                            {a.ticker} 매수 환율
                          </span>
                          <span className="summary-value">
                            {a.purchaseRate?.toLocaleString()} 원
                          </span>
                        </div>
                      ))}
                  </div>
                  <div
                    style={{
                      marginTop: "10px",
                      padding: "8px 10px",
                      background: "#f3f6fe",
                      borderRadius: "8px",
                      fontSize: "11px",
                      color: "#8b87ab",
                      lineHeight: 1.6,
                    }}
                  >
                    이 결과는 가정된 조건과 과거 데이터에 기반한 시뮬레이션이며,
                    실제 투자 결과와 다를 수 있습니다.
                  </div>
                </Card>

                <Card title="세금 및 비용 구성">
                  <TaxBar result={result} />
                </Card>
              </>
            )}
          </div>
        </aside>
      </div>
    </DashboardLayout>
  );
}
