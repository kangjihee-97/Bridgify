interface Props extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: "primary" | "secondary";
}

export const Button = ({
  variant = "primary",
  children,
  style,
  ...props
}: Props) => {
  const baseStyle: React.CSSProperties = {
    padding: "12px 24px",
    borderRadius: "8px",
    fontWeight: "bold",
    fontSize: "14px",
    cursor: "pointer",
    border: "none",
    transition: "all 0.2s",
    width: "100%",
  };

  const variants = {
    primary: { backgroundColor: "#6366f1", color: "white" },
    secondary: { backgroundColor: "#f1f5f9", color: "#64748b" },
  };

  return (
    <button style={{ ...baseStyle, ...variants[variant], ...style }} {...props}>
      {children}
    </button>
  );
};
