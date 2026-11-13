import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { api } from "../../api/http";
import { ApiError } from "../../api/errors";
import { FuelPage } from "./FuelPage";
import { FuelFormPage } from "./FuelFormPage";
import type { FuelEntry } from "./api";

vi.mock("../../api/http", () => ({
  api: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
}));
vi.mock("./FuelCharts", () => ({ FuelCharts: () => <div>Wykresy</div> }));
const vehicle = {
  id: 1,
  make: "Toyota",
  model: "Corolla",
  productionYear: 2020,
  fuelType: "PETROL",
  currentOdometer: 50000,
  purchaseDate: "2020-01-01",
  purchasePrice: 50000,
  createdAt: "2026-01-01T10:00:00Z",
};
const entry: FuelEntry = {
  id: 10,
  vehicleId: 1,
  entryDate: "2026-01-02",
  odometer: 1000,
  liters: 42.123,
  totalCost: 250,
  fullTank: true,
  pricePerLiter: 5.935,
  consumptionFromPreviousFullTank: null,
};
const statistics = {
  vehicleId: 1,
  averageConsumptionLitersPer100Km: null,
  totalLiters: 42.123,
  totalCost: 250,
  fuelEntryCount: 1,
  validConsumptionIntervals: 0,
};
function mount(path = "/fuel?vehicleId=1") {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/fuel" element={<FuelPage />} />
          <Route path="/fuel/:vehicleId/new" element={<FuelFormPage />} />
          <Route
            path="/fuel/:vehicleId/:entryId/edit"
            element={<FuelFormPage />}
          />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
  return client;
}
beforeEach(() => {
  vi.resetAllMocks();
  HTMLDialogElement.prototype.showModal = vi.fn(function (
    this: HTMLDialogElement,
  ) {
    this.setAttribute("open", "");
  });
  HTMLDialogElement.prototype.close = vi.fn();
  vi.mocked(api.get).mockImplementation(async (path) => {
    if (path === "/api/vehicles/2")
      return { data: { ...vehicle, id: 2, make: "Volvo" } };
    if (path === "/api/vehicles")
      return {
        data: {
          content: [vehicle, { ...vehicle, id: 2, make: "Volvo" }],
          page: 0,
          totalPages: 1,
        },
      };
    if (String(path).endsWith("/fuel-statistics")) return { data: statistics };
    if (String(path).endsWith("/fuel-entries/10")) return { data: entry };
    if (String(path).endsWith("/fuel-entries"))
      return {
        data: { content: [entry], page: 0, totalPages: 2, totalElements: 11 },
      };
    return { data: vehicle };
  });
});
describe("fuel history and CRUD", () => {
  it("shows server values, missing consumption and full precision", async () => {
    mount();
    expect(await screen.findByText("5,935")).toBeInTheDocument();
    expect(screen.getAllByText(/42,123/).length).toBeGreaterThan(0);
    expect(
      screen.getByText("Poprawne interwały spalania: 0"),
    ).toBeInTheDocument();
    expect(screen.getAllByText("Brak danych").length).toBe(2);
    expect(api.get).toHaveBeenCalledWith("/api/vehicles/1/fuel-statistics");
  });
  it("sends stable chronological sorting and pagination to API", async () => {
    mount();
    await screen.findByText("5,935");
    fireEvent.click(screen.getByRole("button", { name: /Następna/ }));
    await waitFor(() =>
      expect(api.get).toHaveBeenCalledWith(
        "/api/vehicles/1/fuel-entries",
        expect.objectContaining({
          params: { page: 1, size: 10, sort: ["entryDate,desc", "id,desc"] },
          paramsSerializer: { indexes: null },
        }),
      ),
    );
    fireEvent.change(screen.getByLabelText("Kolejność tankowań"), {
      target: { value: "asc" },
    });
    await waitFor(() =>
      expect(api.get).toHaveBeenCalledWith(
        "/api/vehicles/1/fuel-entries",
        expect.objectContaining({
          params: { page: 0, size: 10, sort: ["entryDate,asc", "id,asc"] },
        }),
      ),
    );
  });
  it("switches vehicle and resets pagination", async () => {
    mount("/fuel?vehicleId=1&page=1");
    await screen.findByText("5,935");
    fireEvent.change(screen.getByLabelText("Wybrany pojazd"), {
      target: { value: "2" },
    });
    await waitFor(() =>
      expect(api.get).toHaveBeenCalledWith(
        "/api/vehicles/2/fuel-entries",
        expect.objectContaining({
          params: { page: 0, size: 10, sort: ["entryDate,desc", "id,desc"] },
        }),
      ),
    );
  });
  it("shows the empty garage without requesting fuel data", async () => {
    vi.mocked(api.get).mockResolvedValue({
      data: { content: [], page: 0, totalPages: 0 },
    });
    mount("/fuel");
    expect(
      await screen.findByText("Tutaj zaczyna się Twoja historia"),
    ).toBeInTheDocument();
    expect(
      vi.mocked(api.get).mock.calls.every(([path]) => path === "/api/vehicles"),
    ).toBe(true);
  });
  it("adds only supported request fields and invalidates dependent data", async () => {
    vi.mocked(api.post).mockResolvedValue({ data: entry });
    const client = mount("/fuel/1/new");
    const invalidate = vi.spyOn(client, "invalidateQueries");
    await screen.findByLabelText("Data tankowania");
    fireEvent.change(screen.getByLabelText("Data tankowania"), {
      target: { value: "2026-01-02" },
    });
    fireEvent.change(screen.getByLabelText("Przebieg (km)"), {
      target: { value: "1000" },
    });
    fireEvent.change(screen.getByLabelText("Ilość paliwa (l)"), {
      target: { value: "42.123" },
    });
    fireEvent.change(screen.getByLabelText("Całkowity koszt (bez waluty)"), {
      target: { value: "250" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Dodaj tankowanie" }));
    await waitFor(() =>
      expect(api.post).toHaveBeenCalledWith("/api/vehicles/1/fuel-entries", {
        entryDate: "2026-01-02",
        odometer: 1000,
        liters: 42.123,
        totalCost: 250,
        fullTank: false,
      }),
    );
    for (const queryKey of [["fuel", 1], ["vehicles"], ["dashboard"]])
      expect(invalidate).toHaveBeenCalledWith({ queryKey });
  });
  it("loads an entry and edits it with PUT", async () => {
    vi.mocked(api.put).mockResolvedValue({ data: entry });
    mount("/fuel/1/10/edit");
    fireEvent.change(await screen.findByLabelText("Ilość paliwa (l)"), {
      target: { value: "45" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Zapisz zmiany" }));
    await waitFor(() =>
      expect(api.put).toHaveBeenCalledWith(
        "/api/vehicles/1/fuel-entries/10",
        expect.objectContaining({ liters: 45, fullTank: true }),
      ),
    );
  });
  it("disables submit while a save is pending", async () => {
    let resolveSave: ((value: { data: FuelEntry }) => void) | undefined;
    vi.mocked(api.put).mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveSave = resolve;
        }),
    );
    mount("/fuel/1/10/edit");
    await screen.findByLabelText("Data tankowania");
    fireEvent.click(screen.getByRole("button", { name: "Zapisz zmiany" }));
    const pending = await screen.findByRole("button", { name: "Zapisywanie…" });
    expect(pending).toBeDisabled();
    fireEvent.click(pending);
    expect(api.put).toHaveBeenCalledTimes(1);
    resolveSave?.({ data: entry });
    await screen.findByText("Historia tankowań");
  });
  it("shows chronology errors from backend without losing input", async () => {
    vi.mocked(api.put).mockRejectedValue(
      new ApiError("Nieprawidłowa chronologia", 400, {
        odometer: "Przebieg koliduje z historią",
      }),
    );
    mount("/fuel/1/10/edit");
    await screen.findByLabelText("Data tankowania");
    fireEvent.click(screen.getByRole("button", { name: "Zapisz zmiany" }));
    expect(
      await screen.findByText("Nieprawidłowa chronologia"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("Przebieg koliduje z historią"),
    ).toBeInTheDocument();
    expect(screen.getByLabelText(/Przebieg \(km\)/)).toHaveValue(1000);
  });
  it("deletes only after confirmation and refreshes statistics", async () => {
    vi.mocked(api.delete).mockResolvedValue({});
    const client = mount();
    const invalidate = vi.spyOn(client, "invalidateQueries");
    fireEvent.click(
      await screen.findByRole("button", { name: /Usuń tankowanie z/ }),
    );
    expect(api.delete).not.toHaveBeenCalled();
    const dialog = screen.getByRole("dialog");
    fireEvent.click(
      within(dialog).getByRole("button", { name: "Usuń tankowanie" }),
    );
    await waitFor(() =>
      expect(api.delete).toHaveBeenCalledWith(
        "/api/vehicles/1/fuel-entries/10",
      ),
    );
    await waitFor(() =>
      expect(invalidate).toHaveBeenCalledWith({ queryKey: ["fuel", 1] }),
    );
  });
  it.each([401, 404])("shows %s error from fuel request", async (status) => {
    const original = vi.mocked(api.get).getMockImplementation()!;
    vi.mocked(api.get).mockImplementation((path, config) =>
      String(path).endsWith("/fuel-entries")
        ? Promise.reject(new ApiError(`Błąd ${status}`, status))
        : original(path, config),
    );
    mount();
    expect(await screen.findByText(`Błąd ${status}`)).toBeInTheDocument();
  });
});
