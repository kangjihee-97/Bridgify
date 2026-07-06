export interface AssetAllocation {
  ticker: string;
  ratio: number | null;
  // 과거 매수 정보 — 셋 다 입력해야 실제 매수가 기준으로 계산된다. 비워두면 "오늘부터 투자"로 간주.
  purchaseDate?: string | null; // "YYYY-MM-DD"
  purchasePrice?: number | null;
  purchaseRate?: number | null;
}

export interface SimulationRequest {
  initialAmount: number;
  monthlyDeposit: number;
  durationYears: number;
  krInflationRate: number;
  taxRate: number;
  expectedReturn?: number;
  assets: AssetAllocation[];
}

export interface YearlyResultResponse {
  year: number;
  nominalBalanceKrw: number;
  realBalanceKrw: number;
  annualProfit: number;
  totalProfit: number;
  principal: number;
  cumulativeReturnRate: number;
  assetComparison?: string;
}

export interface SimulationResponse {
  configId?: string | number;
  nominalBalanceKrw: number;
  realBalanceKrw: number;
  totalProfit: number;
  tax: number;
  totalTax: number;
  returnRate: number;
  totalPrincipal: number;
  yearlyResults: YearlyResultResponse[];
  assetComparisonText?: string;
}

// ===== 실현손익 정산 (배당 포함) =====
export interface RealizedAssetResult {
  ticker: string;
  shares: number;
  costBasisKrw: number;
  currentValueKrw: number;
  capitalGainKrw: number;
  dividendKrw: number;
}

export interface RealizedProfitResponse {
  assets: RealizedAssetResult[];
  totalCostKrw: number;
  totalCurrentValueKrw: number;
  totalCapitalGainKrw: number;
  totalDividendKrw: number;
  capitalGainsTaxKrw: number;
  dividendTaxKrw: number;
  netRealizedProfitKrw: number;
}
