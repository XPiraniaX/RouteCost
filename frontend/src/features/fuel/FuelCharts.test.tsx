import type { ReactNode } from "react";
import { render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { api } from "../../api/http";
import { FuelCharts } from "./FuelCharts";
import type { FuelEntry } from "./api";

vi.mock("../../api/http", () => ({ api: { get: vi.fn() } }));
vi.mock("recharts", () => ({
  ResponsiveContainer: ({ children }: { children: ReactNode }) => (
    <>{children}</>
  ),
  LineChart: ({ data, children }: { data: unknown[]; children: ReactNode }) => (
    <div data-testid="consumption-series" data-rows={JSON.stringify(data)}>
      {children}
    </div>
  ),
  Line: ({ connectNulls }: { connectNulls: boolean }) => (
    <span data-testid="line" data-connect-nulls={String(connectNulls)} />
  ),
  CartesianGrid: () => null,
  Tooltip: () => null,
  XAxis: () => null,
  YAxis: () => null,
}));
vi.mock("../../components/charts/MonthlyChart", () => ({
  MonthlyChart: ({ rows }: { rows: unknown[] }) => (
    <div data-testid="monthly-series" data-rows={JSON.stringify(rows)} />
  ),
}));
const base: FuelEntry = {
  id: 1,
  vehicleId: 1,
  entryDate: "2026-01-01",
  odometer: 1000,
  liters: 40,
  totalCost: 240,
  pricePerLiter: 6,
  fullTank: true,
  consumptionFromPreviousFullTank: null,
};
function mount(entries: FuelEntry[]) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  render(
    <QueryClientProvider client={client}>
      <FuelCharts id={1} entries={entries} />
    </QueryClientProvider>,
  );
}
beforeEach(() => {
  vi.resetAllMocks();
  vi.mocked(api.get).mockResolvedValue({
    data: {
      fuel: {
        monthlyTotals: [
          { year: 2025, month: 12, totalCost: 300, count: 2 },
          { year: 2026, month: 1, totalCost: 240, count: 1 },
        ],
      },
    },
  });
});
describe("fuel chart data boundaries", () => {
  it("preserves server consumption, null gaps and chronological order", async () => {
    const last = {
      ...base,
      id: 3,
      entryDate: "2026-03-01",
      consumptionFromPreviousFullTank: 8.57,
    };
    const middle = { ...base, id: 2, entryDate: "2026-02-01", fullTank: false };
    mount([last, middle, base]);
    expect(
      JSON.parse(
        screen.getByTestId("consumption-series").getAttribute("data-rows")!,
      ),
    ).toEqual([base, middle, last]);
    expect(screen.getByTestId("line")).toHaveAttribute(
      "data-connect-nulls",
      "false",
    );
    await screen.findByTestId("monthly-series");
  });
  it("uses whole-history monthly data independently of visible entries", async () => {
    mount([base]);
    expect(screen.queryByTestId("consumption-series")).not.toBeInTheDocument();
    expect(
      screen.getByText(/Brak danych spalania dla wpisów/),
    ).toBeInTheDocument();
    const chart = await screen.findByTestId("monthly-series");
    expect(JSON.parse(chart.getAttribute("data-rows")!)).toHaveLength(2);
    expect(api.get).toHaveBeenCalledWith("/api/vehicles/1/cost-summary");
  });
  it("isolates monthly report errors from consumption data", async () => {
    vi.mocked(api.get).mockRejectedValue(new Error("Raport niedostępny"));
    mount([{ ...base, consumptionFromPreviousFullTank: 7 }]);
    await waitFor(() =>
      expect(screen.getByRole("alert")).toHaveTextContent("Raport niedostępny"),
    );
    expect(screen.getByTestId("consumption-series")).toBeInTheDocument();
  });
});
