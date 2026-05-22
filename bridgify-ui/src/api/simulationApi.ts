import axios from "axios";
import type {
  SimulationRequest,
  SimulationResponse,
} from "../types/simulation";

const BASE_URL = "http://localhost:8080/api/simulation";

export const runSimulationApi = async (
  request: SimulationRequest,
): Promise<SimulationResponse> => {
  const res = await axios.post<SimulationResponse>(`${BASE_URL}/run`, request);

  return res.data;
};
