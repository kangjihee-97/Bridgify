interface Props {
  title: string;
  value: string;
  subValue?: string;
  variant?: "blue" | "green" | "purple";
}

export const StatBox = ({
  title,
  value,
  subValue,
  variant = "blue",
}: Props) => {
  return (
    /* variant(blue, green, purple)에 따라 클래스가 유동적으로 붙습니다 */
    <div className={`hero-stat-box ${variant}`}>
      <p className="stat-label">{title}</p>
      <h3 className="stat-value">{value}</h3>
      {subValue && <p className="stat-sub-value">{subValue}</p>}
    </div>
  );
};
