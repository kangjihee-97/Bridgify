interface CardProps {
  title?: string;
  children: React.ReactNode;
  className?: string;
  variant?: "purple" | "blue" | "green" | "default";
  /** 제목 줄 오른쪽 끝에 배치할 요소 (버튼, 배지 등) */
  action?: React.ReactNode;
}

export default function Card({
  title,
  children,
  className,
  variant = "default",
  action,
}: CardProps) {
  return (
    <div className={`dashboard-card ${variant} ${className || ""}`}>
      {title &&
        (action ? (
          // 제목과 액션을 한 줄에 양끝 정렬
          <div
            style={{
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
              gap: 12,
              marginBottom: 12,
            }}
          >
            <h3 className="card-title" style={{ margin: 0 }}>
              {title}
            </h3>
            {action}
          </div>
        ) : (
          <h3 className="card-title">{title}</h3>
        ))}
      <div className="card-content">{children}</div>
    </div>
  );
}
