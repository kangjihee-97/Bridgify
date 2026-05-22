import React from "react";
import { useSimulationStore } from "../../store/simulationStore";

const parseNumber = (value: string) => {
  // 콤마 제거 및 공백 제거
  const cleaned = value.replaceAll(",", "").trim();
  if (cleaned === "") return 0;
  return Number(cleaned);
};

export const SimulationSettingsForm = () => {
  const { form, setForm } = useSimulationStore();

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;

    // 사용자가 소수점('.')이나 빈 값을 입력 중일 때는 parse하지 않고 상태 업데이트 유연성 확보
    if (value.endsWith(".") || value === "") {
      // 퍼센트 단위 필드인데 끝이 '.'으로 끝나면 임시 처리를 위해 바로 저장하거나 분기 처리
      // 여기서는 일반적인 숫자 파싱 예외 처리로 방어
      if (name === "taxRate") {
        const num = parseNumber(value.slice(0, -1));
        setForm("taxRate", num / 100);
        return;
      }
    }

    const numValue = parseNumber(value);
    if (isNaN(numValue)) return;

    // 양도소득세율: 화면(15) -> 스토어(0.15)
    if (name === "taxRate") {
      setForm("taxRate", numValue === 0 ? 0 : numValue / 100);
      return;
    }

    setForm(name as any, numValue);
  };

  // 비율 표시용 헬퍼 함수 (사용자 입력 방해 방지용 처리)
  const getPercentValue = (storedValue: number | null | undefined) => {
    if (storedValue === null || storedValue === undefined || storedValue === 0)
      return "";
    // 부동소수점 오차 해결 (예: 0.15 * 100 = 15.000000000000002 방지)
    return Number((storedValue * 100).toFixed(2)).toString();
  };

  return (
    <div className="settings-form-wrapper">
      {/* 투자 금액 섹션 */}
      <section className="settings-section">
        <h4 className="settings-section-title">투자 금액</h4>
        <div className="settings-column">
          <div className="input-field">
            <label className="settings-label">초기 투자금</label>
            <input
              name="initialAmount"
              type="text"
              inputMode="numeric"
              value={
                form.initialAmount === 0
                  ? ""
                  : form.initialAmount.toLocaleString()
              }
              onChange={handleChange}
              className="settings-input"
              placeholder="0"
            />
          </div>

          <div className="input-field">
            <label className="settings-label">월 적립금</label>
            <input
              name="monthlyDeposit"
              type="text"
              inputMode="numeric"
              value={
                form.monthlyDeposit === 0
                  ? ""
                  : form.monthlyDeposit.toLocaleString()
              }
              onChange={handleChange}
              className="settings-input"
              placeholder="0"
            />
          </div>
        </div>
      </section>

      {/* 수익률 설정 섹션 */}
      <section className="settings-section">
        <h4 className="settings-section-title">수익률 설정</h4>
        <div className="input-field">
          <label className="settings-label">예상 연 수익률</label>
          <div className="input-with-unit">
            <input
              name="expectedReturn"
              type="text"
              inputMode="decimal"
              placeholder="예: 8.5"
              value={
                form.expectedReturn === 0 ? "" : (form.expectedReturn ?? "")
              }
              onChange={handleChange}
              className="settings-input"
            />
            <span className="input-unit">%</span>
          </div>
        </div>
      </section>

      {/* 경제 지표 섹션 */}
      <section className="settings-section">
        <h4 className="settings-section-title">경제 지표</h4>
        <div className="settings-grid">
          <div className="input-field">
            <label className="settings-label">물가 상승률</label>
            <div className="input-with-unit">
              <input
                name="krInflationRate"
                type="text"
                inputMode="decimal"
                placeholder="0"
                value={
                  form.krInflationRate === 0 ? "" : (form.krInflationRate ?? "")
                }
                onChange={handleChange}
                className="settings-input"
              />
              <span className="input-unit">%</span>
            </div>
          </div>

          <div className="input-field">
            <label className="settings-label">투자 기간</label>
            <div className="input-with-unit">
              <input
                name="durationYears"
                type="text"
                inputMode="numeric"
                placeholder="0"
                value={
                  form.durationYears === 0 ? "" : (form.durationYears ?? "")
                }
                onChange={handleChange}
                className="settings-input"
              />
              <span className="input-unit">년</span>
            </div>
          </div>
        </div>
      </section>

      {/* 세금 설정 섹션 */}
      <section className="settings-section">
        <h4 className="settings-section-title">세금 설정</h4>
        <div className="input-field">
          <label className="settings-label">양도소득세율</label>
          <div className="input-with-unit">
            <input
              name="taxRate"
              type="text"
              inputMode="decimal"
              placeholder="0"
              // 인라인 가공 대신 헬퍼 함수를 사용하여 입력 에러 차단
              value={getPercentValue(form.taxRate)}
              onChange={handleChange}
              className="settings-input"
            />
            <span className="input-unit">%</span>
          </div>
        </div>
      </section>
    </div>
  );
};
