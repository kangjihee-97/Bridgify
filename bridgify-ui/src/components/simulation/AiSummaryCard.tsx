import { useState } from "react";
import { fetchAiSummary } from "../../api/simulationApi";
import { useSimulationStore } from "../../store/simulationStore";
import type { SimulationResponse } from "../../types/simulation";
import Card from "../ui/Card";

interface Props {
  result: SimulationResponse;
}

/**
 * AI 결과 해설 카드 (Gemini).
 *
 * 버튼을 눌러야 호출한다 — 무료 등급 한도(하루 20회)를 아끼기 위해
 * 결과가 나올 때마다 자동 생성하지 않는다.
 */
export const AiSummaryCard = ({ result }: Props) => {
  const form = useSimulationStore((s) => s.form);
  const assets = useSimulationStore((s) => s.assets);

  const [summary, setSummary] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleGenerate = async () => {
    setLoading(true);
    try {
      const text = await fetchAiSummary({
        durationYears: form.durationYears,
        totalPrincipal: result.totalPrincipal,
        nominalBalanceKrw: result.nominalBalanceKrw,
        realBalanceKrw: result.realBalanceKrw,
        totalProfit: result.totalProfit,
        returnRate: result.returnRate,
        tax: result.tax,
        tickers: assets.map((a) => a.ticker).filter(Boolean),
      });
      setSummary(text);
    } catch (e) {
      console.error("AI 해설 실패:", e);
      setSummary("AI 해설을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.");
    } finally {
      setLoading(false);
    }
  };

  // 제목 줄 오른쪽에 배치할 배지 + 버튼
  const action = (
    <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
      <span
        style={{
          fontSize: 11,
          fontWeight: 600,
          color: "#fff",
          background: "linear-gradient(135deg, #6366f1, #a855f7)",
          padding: "4px 10px",
          borderRadius: 999,
          lineHeight: 1.2,
        }}
      >
        Gemini AI
      </span>
      <button
        type="button"
        onClick={handleGenerate}
        disabled={loading}
        style={{
          border: "1px solid #d9d4f0",
          background: "#f7f6fe",
          color: "#4b3fa3",
          borderRadius: 999,
          padding: "4px 12px",
          fontSize: 12,
          fontWeight: 600,
          lineHeight: 1.3,
          cursor: loading ? "default" : "pointer",
          opacity: loading ? 0.6 : 1,
          whiteSpace: "nowrap",
        }}
      >
        {loading ? "분석 중..." : summary ? "다시 분석" : "AI 해설 보기"}
      </button>
    </div>
  );

  return (
    <Card title="AI 결과 해설" action={action}>
      {summary ? (
        // 빈 줄을 기준으로 문단을 나눠 각각 <p>로 렌더링한다.
        // 한 덩어리로 두면 줄바꿈만 생기고 문단 간 여백이 없어 가독성이 떨어진다.
        <div>
          {summary
            .split(/\n\s*\n/)
            .map((para) => para.trim())
            .filter(Boolean)
            .map((para, i) => (
              <p
                key={i}
                style={{
                  fontSize: 13.5,
                  lineHeight: 1.9,
                  color: "#3d3a52",
                  margin: i === 0 ? "0 0 12px" : 0,
                }}
              >
                {para}
              </p>
            ))}
        </div>
      ) : (
        <p
          style={{
            fontSize: 12.5,
            color: "#8b87ab",
            textAlign: "center",
            padding: "18px 0",
            margin: 0,
          }}
        >
          {loading
            ? "AI가 결과를 분석하고 있습니다..."
            : "버튼을 눌러 이번 시뮬레이션 결과에 대한 해설을 받아보세요."}
        </p>
      )}
    </Card>
  );
};
