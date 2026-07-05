import { useState } from "react";
import { useSimulationStore } from "../../store/simulationStore";
import { runRealizedProfitApi } from "../../api/simulationApi";
import type { RealizedProfitResponse } from "../../types/simulation";
import Card from "../ui/Card";
import { Button } from "../ui/Button";
import { StatBox } from "../ui/StatBox";
import { formatKoreanCurrency } from "../../utils/format";

export function RealizedProfitSection() {
  const { form, assets } = useSimulationStore();
  const [result, setResult] = useState<RealizedProfitResponse | null>(null);
  const [loading, setLoading] = useState(false);

  const handleCalculate = async () => {
    setLoading(true);
    try {
      const data = await runRealizedProfitApi({ ...form, assets } as any);
      setResult(data);
    } catch (error) {
      console.error("실현손익 정산 실패:", error);
      alert("서버 연결에 실패했습니다. 백엔드가 켜져 있는지 확인하세요.");
    } finally {
      setLoading(false);
    }
  };

  // 매수 정보(매수일·매수가)가 있어야 실현손익 계산 가능
  const hasPurchaseInfo = assets.some(
    (a) => a.ticker && a.purchaseDate && a.purchasePrice,
  );

  return (
    <Card title="실현손익 정산 (배당 포함)" variant="green">
      <p style={{ fontSize: 13, color: "#6b6b6b", marginTop: 0 }}>
        과거에 산 종목을 지금 팔면, 시세차익 + 배당 − 세금까지 반영한 실제
        순수익을 계산합니다.
      </p>

      {!hasPurchaseInfo && (
        <div
          style={{
            padding: "8px 10px",
            background: "#fff7ed",
            border: "1px solid #fed7aa",
            borderRadius: 8,
            fontSize: 12,
            color: "#9a3412",
            marginBottom: 10,
          }}
        >
          종목의 <strong>매수일·매수가·매수환율</strong>을 입력해야 실현손익이
          계산됩니다.
        </div>
      )}

      <Button onClick={handleCalculate} disabled={loading}>
        {loading ? "정산 중..." : "실현손익 정산"}
      </Button>

      {result && (
        <div style={{ marginTop: 16 }}>
          <StatBox
            title="순 실현손익"
            value={formatKoreanCurrency(result.netRealizedProfitKrw)}
            variant="green"
            subValue="시세차익 + 배당 − 양도세"
          />

          <div className="summary-list-content" style={{ marginTop: 12 }}>
            <div className="summary-item">
              <span className="summary-label">총 투입 원금</span>
              <span className="summary-value">
                {formatKoreanCurrency(result.totalCostKrw)}
              </span>
            </div>
            <div className="summary-item">
              <span className="summary-label">현재 평가액</span>
              <span className="summary-value">
                {formatKoreanCurrency(result.totalCurrentValueKrw)}
              </span>
            </div>
            <div className="summary-item">
              <span className="summary-label">시세차익</span>
              <span className="summary-value nominal">
                {formatKoreanCurrency(result.totalCapitalGainKrw)}
              </span>
            </div>
            <div className="summary-item">
              <span className="summary-label">세후 배당</span>
              <span className="summary-value nominal">
                {formatKoreanCurrency(result.totalDividendKrw)}
              </span>
            </div>
            <div className="summary-item">
              <span className="summary-label">양도소득세</span>
              <span className="summary-value negative">
                -{formatKoreanCurrency(result.capitalGainsTaxKrw)}
              </span>
            </div>
          </div>

          {result.assets.length > 0 && (
            <div style={{ marginTop: 14 }}>
              <div
                style={{
                  fontSize: 12,
                  fontWeight: 700,
                  color: "#555",
                  marginBottom: 6,
                }}
              >
                종목별 상세
              </div>
              {result.assets.map((a, i) => (
                <div
                  key={i}
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    fontSize: 12,
                    padding: "6px 0",
                    borderTop: "1px solid #eee",
                  }}
                >
                  <span style={{ fontWeight: 600 }}>
                    {a.ticker}{" "}
                    <span style={{ color: "#999", fontWeight: 400 }}>
                      {a.shares.toFixed(2)}주
                    </span>
                  </span>
                  <span>
                    차익 {formatKoreanCurrency(a.capitalGainKrw)} · 배당{" "}
                    {formatKoreanCurrency(a.dividendKrw)}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </Card>
  );
}
