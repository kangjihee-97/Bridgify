import axios from "axios";
import type {
  SimulationRequest,
  SimulationResponse,
  RealizedProfitResponse,
} from "../types/simulation";

const BASE_URL = "http://localhost:8080/api/simulation";
const REALIZED_URL = "http://localhost:8080/api/realized-profit";

export const runSimulationApi = async (
  request: SimulationRequest,
): Promise<SimulationResponse> => {
  const res = await axios.post<SimulationResponse>(`${BASE_URL}/run`, request);

  return res.data;
};

// 실현손익 정산 (과거 매수 → 현재까지 시세차익 + 배당 − 세금)
export const runRealizedProfitApi = async (
  request: SimulationRequest,
): Promise<RealizedProfitResponse> => {
  const res = await axios.post<RealizedProfitResponse>(REALIZED_URL, request);

  return res.data;
};
