import { useEffect, useState } from "react";
import { fetchInflationRates } from "../../api/simulationApi";
import { useSimulationStore } from "../../store/simulationStore";

/**
 * 실시간 물가 자동 반영 + 반영 경로 안내.
 *
 * - 한국 물가: 최종 원화의 "구매력"을 깎는 데 직접 사용 → 입력칸에 자동 채움
 * - 미국 물가: 이미 주가(기업 실적)와 환율(물가 차이)에 반영되어 있음 → 참고 표시
 */
export const InflationHint = () => {
  // setForm만 선택 구독 — form 값을 구독하면 입력할 때마다 이 컴포넌트도 리렌더된다.
  const setForm = useSimulationStore((s) => s.setForm);

  const [rates, setRates] = useState<{
    usInflationRate: number;
    krInflationRate: number;
  } | null>(null);
  const [loading, setLoading] = useState(true);
  const [showInfo, setShowInfo] = useState(false);

  useEffect(() => {
    fetchInflationRates()
      .then((data) => {
        setRates(data);
        // 마운트 시 한국 물가를 무조건 자동으로 채운다 ("원딸깍").
        // 이 훅은 마운트 때 1회만 실행되므로, 이후 사용자가 직접 수정한 값은 덮어쓰지 않는다.
        // (스토어 초기값 3은 API 실패 시의 폴백 역할만 한다)
        setForm("krInflationRate" as any, data.krInflationRate);
      })
      .catch((e) => console.error("물가 조회 실패:", e))
      .finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (loading) {
    return (
      <p style={{ fontSize: 11, color: "#9aa0b0", margin: "4px 0 0" }}>
        실시간 물가 불러오는 중...
      </p>
    );
  }

  if (!rates) return null;

  const row = (
    flag: string,
    name: string,
    rate: number,
    note: string,
    withInfo = false,
  ) => (
    <div
      style={{
        display: "flex",
        alignItems: "center",
        gap: 4,
        whiteSpace: "nowrap",
        fontSize: 11,
        lineHeight: 1.9,
      }}
    >
      <span>{flag}</span>
      <span style={{ color: "#6b6b6b" }}>{name}</span>
      <strong style={{ color: "#4b3fa3" }}>{rate}%</strong>
      <span style={{ color: "#a8a4bd", fontSize: 10 }}>{note}</span>
      {withInfo && (
        <button
          type="button"
          onClick={() => setShowInfo((v) => !v)}
          style={{
            border: "none",
            background: "transparent",
            color: "#a855f7",
            cursor: "pointer",
            fontSize: 11,
            padding: 0,
            lineHeight: 1,
          }}
          title="미국 물가는 어디에 반영되나요?"
        >
          ⓘ
        </button>
      )}
    </div>
  );

  return (
    <div style={{ marginTop: 6 }}>
      <div
        style={{
          fontSize: 10,
          color: "#6366f1",
          fontWeight: 600,
          marginBottom: 2,
        }}
      >
        ✓ 실시간 물가 자동 반영됨
      </div>

      {row("🇰🇷", "한국", rates.krInflationRate, "적용")}
      {row("🇺🇸", "미국", rates.usInflationRate, "주가·환율 반영", true)}

      {showInfo && (
        <div
          style={{
            marginTop: 6,
            padding: "8px 10px",
            background: "#f7f6fe",
            border: "1px solid #e5e0fa",
            borderRadius: 8,
            fontSize: 11,
            color: "#5b5570",
            lineHeight: 1.7,
          }}
        >
          <strong>미국 물가는 어디에 반영되나요?</strong>
          <div style={{ marginTop: 4 }}>
            • <strong>주가</strong> — 미국 물가가 오르면 기업 매출·이익도 올라
            주가(수익률)에 이미 포함됩니다.
          </div>
          <div>
            • <strong>환율</strong> — 미국·한국의 물가 차이는 장기적으로 원/달러
            환율에 반영됩니다.
          </div>
          <div style={{ marginTop: 4 }}>
            그래서 최종 원화 금액에는 <strong>한국 물가</strong>만 나눠 구매력을
            계산합니다. (미국 물가를 또 빼면 이중 차감)
          </div>
        </div>
      )}
    </div>
  );
};
