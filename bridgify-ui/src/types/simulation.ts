export interface AssetAllocation {
  ticker: string;
  ratio: number | null;
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
