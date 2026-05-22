interface CardProps {
  title?: string;
  children: React.ReactNode;
  className?: string;
  variant?: "purple" | "blue" | "green" | "default";
}

export default function Card({
  title,
  children,
  className,
  variant = "default",
}: CardProps) {
  return (
    <div className={`dashboard-card ${variant} ${className || ""}`}>
      {title && <h3 className="card-title">{title}</h3>}
      <div className="card-content">{children}</div>
    </div>
  );
}
