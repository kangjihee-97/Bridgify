export default function Header() {
  return (
    <header className="dashboard-header">
      <div className="header-left">
        <div className="logo-icon">
          <svg
            width="24"
            height="24"
            viewBox="0 0 24 24"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
          >
            <path
              d="M3 17L9 11L13 15L21 7"
              stroke="#6366f1"
              strokeWidth="2.5"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
            <path
              d="M18 7H21V10"
              stroke="#6366f1"
              strokeWidth="2.5"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </div>
        <div className="brand-info">
          <h1>Bridgify</h1>
          <span>실질 구매력 기준 투자 성과 분석 리포트</span>
        </div>
      </div>

      <div className="header-right">
        <button className="header-btn secondary">새 시뮬레이션</button>
        <button className="header-btn secondary">결과 저장</button>
        <button className="header-btn primary">
          <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            style={{ marginRight: "6px" }}
          >
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M7 10l5 5 5-5M12 15V3" />
          </svg>
          PDF 다운로드
        </button>
      </div>
    </header>
  );
}
