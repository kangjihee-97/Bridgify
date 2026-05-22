interface Props extends React.InputHTMLAttributes<HTMLInputElement> {
  label: string;
  suffix?: string;
}

export const Input = ({ label, suffix, style, ...props }: Props) => {
  return (
    <div
      style={{
        marginBottom: "16px",
        display: "flex",
        flexDirection: "column",
        gap: "6px",
      }}
    >
      <label style={{ fontSize: "12px", color: "#64748b", fontWeight: 600 }}>
        {label}
      </label>
      <div
        style={{ position: "relative", display: "flex", alignItems: "center" }}
      >
        <input
          style={{
            width: "100%",
            padding: "10px 12px",
            borderRadius: "8px",
            border: "1px solid #e2e8f0",
            fontSize: "14px",
            outline: "none",
            ...style,
          }}
          {...props}
        />
        {suffix && (
          <span
            style={{
              position: "absolute",
              right: "12px",
              color: "#94a3b8",
              fontSize: "13px",
            }}
          >
            {suffix}
          </span>
        )}
      </div>
    </div>
  );
};
