import { useState } from "react";
import { useSimulationStore } from "../../store/simulationStore";
import { runRealizedProfitApi } from "../../api/simulationApi";
import type { RealizedProfitResponse } from "../../types/simulation";
import Card from "../ui/Card";
import { Button } from "../ui/Button";
import { StatBox } from "../ui/StatBox";
import { formatKoreanCurrency } from "../../utils/format";
import { RealizedTaxDonut } from "./RealizedTaxDonut";

export function RealizedProfitSection() {
  // 필요한 상태만 선택 구독
  const form = useSimulationStore((s) => s.form);
  const assets = useSimulationStore((s) => s.assets);
  const [result, setResult] = useState<RealizedProfitResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [reinvest, setReinvest] = useState(true); // 배당 재투자(DRIP) 기본 ON

  // 실현손익 계산 (reinvest 값을 함께 전달)
  const runCalc = async (useReinvest: boolean) => {
    setLoading(true);
    try {
      const data = await runRealizedProfitApi({
        ...form,
        assets,
        reinvest: useReinvest,
      } as any);
      setResult(data);
    } catch (error) {
      console.error("실현손익 정산 실패:", error);
      alert("서버 연결에 실패했습니다. 백엔드가 켜져 있는지 확인하세요.");
    } finally {
      setLoading(false);
    }
  };

  const handleCalculate = () => runCalc(reinvest);

  // 체크박스 토글 → 이미 결과가 있으면 바로 다시 계산해서 비교되게
  const handleReinvestToggle = (checked: boolean) => {
    setReinvest(checked);
    if (result) runCalc(checked);
  };

  const hasPurchaseInfo = assets.some(
    (a) => a.ticker && a.purchaseDate && a.purchasePrice,
  );

  return (
    <Card title="실현손익 정산 (배당 재투자)" variant="green">
      <p style={{ fontSize: 13, color: "#6b6b6b", marginTop: 0 }}>
        과거에 산 종목을 지금 팔면, 매년 받은 배당을 재투자(DRIP)한 효과까지
        반영해 실제 순수익을 계산합니다.
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

      {/* 배당 재투자(DRIP) 켜고 끄기 */}
      <label
        style={{
          display: "flex",
          alignItems: "center",
          gap: 8,
          fontSize: 13,
          color: "#4b3fa3",
          margin: "2px 0 12px",
          cursor: "pointer",
        }}
      >
        <input
          type="checkbox"
          checked={reinvest}
          onChange={(e) => handleReinvestToggle(e.target.checked)}
        />
        <span style={{ fontWeight: 600 }}>배당 재투자 (DRIP)</span>
        <span style={{ fontSize: 11, color: "#8b87ab" }}>
          — 받은 배당으로 주식을 더 삽니다
        </span>
      </label>

      <Button onClick={handleCalculate} disabled={loading}>
        {loading ? "정산 중..." : "실현손익 정산"}
      </Button>

      {result && (
        <div style={{ marginTop: 16 }}>
          <StatBox
            title="순 실현손익"
            value={formatKoreanCurrency(result.netRealizedProfitKrw)}
            variant="green"
            subValue={
              reinvest
                ? "배당 재투자 + 시세차익 − 세금"
                : "시세차익 + 배당 − 세금"
            }
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
              <span className="summary-label">
                시세차익{reinvest ? " (재투자 포함)" : ""}
              </span>
              <span className="summary-value nominal">
                {formatKoreanCurrency(result.totalCapitalGainKrw)}
              </span>
            </div>
            <div className="summary-item">
              <span className="summary-label">
                {reinvest ? "재투자된 배당" : "세후 배당"}
              </span>
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
            <div className="summary-item">
              <span className="summary-label">배당소득세</span>
              <span className="summary-value negative">
                -{formatKoreanCurrency(result.dividendTaxKrw)}
              </span>
            </div>
          </div>

          {reinvest && (
            <p
              style={{
                fontSize: 11,
                color: "#8b87ab",
                marginTop: 8,
                lineHeight: 1.6,
              }}
            >
              ※ 받은 배당은 그 해 주가로 재매수되어 보유 주식수·평가액에
              반영됩니다. (배당은 시세차익에 포함)
            </p>
          )}

          {/* 세금 구성 도넛 (양도세 + 배당세) */}
          <div style={{ marginTop: 18 }}>
            <div
              style={{
                fontSize: 12,
                fontWeight: 700,
                color: "#555",
                marginBottom: 6,
              }}
            >
              세금 구성
            </div>
            <RealizedTaxDonut
              capitalGainsTaxKrw={result.capitalGainsTaxKrw}
              dividendTaxKrw={result.dividendTaxKrw}
            />
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
