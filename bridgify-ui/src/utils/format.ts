export const formatKoreanCurrency = (amount: number): string => {
  const rounded = Math.round(amount);

  if (rounded >= 100000000) {
    const eok = Math.floor(rounded / 100000000);
    const man = Math.floor((rounded % 100000000) / 10000);

    return man > 0 ? `${eok}억 ${man.toLocaleString()}만 원` : `${eok}억 원`;
  }

  if (rounded >= 10000) {
    return `${Math.floor(rounded / 10000).toLocaleString()}만 원`;
  }

  return `${rounded.toLocaleString()}원`;
};

export const formatCount = (count: number): string => {
  if (count >= 100) return Math.floor(count).toLocaleString();
  if (count >= 1) return count.toFixed(1);
  return count.toFixed(3);
};
