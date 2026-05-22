import { create } from "zustand";
import type { SimulationResponse, AssetAllocation } from "../types/simulation";

type FormKey =
  | "initialAmount"
  | "monthlyDeposit"
  | "durationYears"
  | "krInflationRate"
  | "expectedReturn"
  | "taxRate";

interface SimulationState {
  form: {
    initialAmount: number;
    monthlyDeposit: number;
    durationYears: number;
    krInflationRate: number;
    expectedReturn: number | null;
    taxRate: number;
  };

  assets: AssetAllocation[];

  result: SimulationResponse | null;

  loading: boolean;

  setForm: (name: FormKey, value: number) => void;
  setAssets: (assets: AssetAllocation[]) => void;
  setResult: (result: SimulationResponse | null) => void;

  setLoading: (loading: boolean) => void;

  startLoading: () => void;
  stopLoading: () => void;
}

export const useSimulationStore = create<SimulationState>((set) => ({
  form: {
    initialAmount: 1000000,
    monthlyDeposit: 500000,
    durationYears: 10,
    krInflationRate: 3,
    taxRate: 0.15,
    expectedReturn: null,
  },

  assets: [
    { ticker: "AAPL", ratio: 50 },
    { ticker: "SPY", ratio: 50 },
  ],

  result: null,

  loading: false,

  setForm: (name, value) =>
    set((state) => ({
      form: { ...state.form, [name]: value },
    })),

  setAssets: (assets) => set({ assets }),

  setResult: (result) => set({ result }),

  setLoading: (loading) => set({ loading }),

  startLoading: () => set({ loading: true }),

  stopLoading: () => set({ loading: false }),
}));
