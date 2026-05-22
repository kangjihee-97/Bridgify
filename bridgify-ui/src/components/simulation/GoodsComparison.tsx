import type { SimulationResponse } from "../../types/simulation";
import { COMPARISON_ITEMS } from "../../constants/goods";
// 1. formatKoreanCurrency 임포트 추가
import { formatCount, formatKoreanCurrency } from "../../utils/format";
import Card from "../ui/Card";

interface Props {
  result: SimulationResponse;
}

export const GoodsComparisonCard = ({ result }: Props) => {
  if (!result) return null;

  const realBalance = result.realBalanceKrw ?? 0;
  const currentYear = new Date().getFullYear();

  return (
    <Card
      title="실물 가치 체감 지표 (현재 가치 기준)"
      className="comparison-section-card"
    >
      <div className="comparison-header-content">
        <p className="comparison-subtitle">
          물가 상승률이 반영된 실질 가치(
          {/* 2. 실질 가치 한글 포맷 유틸 적용 */}
          <strong>{formatKoreanCurrency(realBalance)}</strong>) 기준입니다.
        </p>
      </div>

      <div className="goods-grid">
        {COMPARISON_ITEMS.map((item) => {
          const count = item.price > 0 ? realBalance / item.price : 0;

          return (
            <div key={item.id} className="goods-item-inner-card">
              <div className="item-emoji-wrapper">{item.emoji}</div>
              <div className="item-info-wrapper">
                <p className="item-name-label">{item.name}</p>
                <p className="item-value-text">
                  {formatCount(count)}
                  <span className="item-unit-text">{item.unit}</span>
                </p>
              </div>
            </div>
          );
        })}
      </div>

      <footer className="comparison-footer-area">
        {(result.tax ?? 0) > 0 && (
          <p className="tax-notice">
            {/* 3. 예상 세금 한글 포맷 유틸 및 nullish 병합 연산자(?? 0) 적용 */}
            ※ 예상 세금 약 {formatKoreanCurrency(result.tax ?? 0)} 포함
          </p>
        )}
        <p className="year-notice">
          * 기준 물가는 {currentYear}년 시장 평균가를 기반으로 합니다.
        </p>
      </footer>
    </Card>
  );
};
