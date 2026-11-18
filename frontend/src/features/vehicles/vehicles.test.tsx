import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { api } from "../../api/http";
import { ApiError } from "../../api/errors";
import { VehiclesPage } from "./VehiclesPage";
import { VehicleFormPage } from "./VehicleFormPage";
import { VehicleDetailPage } from "./VehicleDetailPage";
import { DashboardPage } from "../dashboard/DashboardPage";
import { CostReport } from "./CostReport";
import { DeleteVehicleDialog } from "./DeleteVehicleDialog";
import type { Vehicle } from "./api";

vi.mock("../../api/http", () => ({
  api: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
}));
vi.mock("../auth/useAuth", () => ({
  useAuth: () => ({ user: { firstName: "Anna" } }),
}));
function mount(element: React.ReactNode, initialPath = "/") {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={[initialPath]}>{element}</MemoryRouter>
    </QueryClientProvider>,
  );
  return client;
}
const vehicle: Vehicle = {
  id: 1,
  make: "Toyota",
  model: "Corolla",
  productionYear: 2020,
  fuelType: "HYBRID",
  currentOdometer: 50000,
  purchaseDate: "2020-06-01",
  purchasePrice: 50000,
  createdAt: "2026-01-01T10:00:00Z",
};
beforeEach(() => vi.resetAllMocks());
describe("dashboard and vehicle data", () => {
  it("loads real summary and vehicle endpoints", async () => {
    vi.mocked(api.get).mockImplementation(async (path) => ({
      data:
        path === "/api/dashboard/summary"
          ? {
              vehicleCount: 1,
              fuelEntryCount: 7,
              expenseCount: 3,
              activeReminderCount: 2,
              overdueReminderCount: 1,
              totalMaintenanceCost: 15750,
              totalFuelCost: 12500,
              maintenanceCostLastThreeMonths: 4000,
            }
          : { content: [vehicle], totalElements: 1, totalPages: 1 },
    }));
    mount(<DashboardPage />);
    expect(await screen.findByText("Toyota Corolla")).toBeInTheDocument();
    expect(screen.getByText("Zaległe przypomnienia: 1")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /Wszystkie przypomnienia/ })).toHaveAttribute("href", "/reminders");
    expect(screen.queryByText("Koszty z właściwej perspektywy")).not.toBeInTheDocument();
    expect(api.get).toHaveBeenCalledWith("/api/dashboard/summary");
  });
  it("sends server filtering and pagination parameters", async () => {
    vi.mocked(api.get).mockResolvedValue({
      data: { content: [vehicle], totalElements: 9, totalPages: 2 },
    });
    mount(<VehiclesPage />);
    await screen.findByText("Toyota Corolla");
    fireEvent.click(screen.getByRole("button", { name: /Następna/ }));
    await waitFor(() =>
      expect(api.get).toHaveBeenCalledWith("/api/vehicles", {
        params: { page: 1, size: 8 },
      }),
    );
    fireEvent.change(screen.getByRole("combobox"), {
      target: { value: "DIESEL" },
    });
    await waitFor(() =>
      expect(api.get).toHaveBeenCalledWith("/api/vehicles", {
        params: { page: 0, size: 8, fuelType: "DIESEL" },
      }),
    );
  });
  it("shows API error and retries", async () => {
    vi.mocked(api.get)
      .mockRejectedValueOnce(new ApiError("Brak połączenia"))
      .mockResolvedValue({
        data: { content: [], totalPages: 0, totalElements: 0 },
      });
    mount(<VehiclesPage />);
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Brak połączenia",
    );
    fireEvent.click(screen.getByRole("button", { name: "Spróbuj ponownie" }));
    expect(
      await screen.findByText("Tutaj zaczyna się Twoja historia"),
    ).toBeInTheDocument();
  });
  it("submits validated DTO and invalidates vehicle/dashboard cache", async () => {
    vi.mocked(api.post).mockResolvedValue({ data: vehicle });
    const client = mount(<VehicleFormPage />);
    const invalidate = vi.spyOn(client, "invalidateQueries");
    const user = userEvent.setup();
    await user.type(screen.getByLabelText("Marka"), "Toyota");
    await user.type(screen.getByLabelText("Model"), "Corolla");
    await user.type(screen.getByLabelText("Rok produkcji"), "2020");
    fireEvent.change(screen.getByLabelText("Data zakupu"), {
      target: { value: "2020-06-01" },
    });
    await user.type(screen.getByLabelText(/Cena zakupu/), "50000");
    await user.click(screen.getByRole("button", { name: "Dodaj pojazd" }));
    await waitFor(() =>
      expect(api.post).toHaveBeenCalledWith(
        "/api/vehicles",
        expect.objectContaining({
          make: "Toyota",
          productionYear: 2020,
          purchasePrice: 50000,
        }),
      ),
    );
    await waitFor(() =>
      expect(invalidate).toHaveBeenCalledWith({ queryKey: ["dashboard"] }),
    );
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["vehicles"] });
  });
  it("creates a vehicle with an empty purchase price as zero", async () => {
    vi.mocked(api.post).mockResolvedValue({ data: { ...vehicle, purchasePrice: 0 } });
    mount(<VehicleFormPage />);
    const user = userEvent.setup();
    await user.type(screen.getByLabelText("Marka"), "Toyota");
    await user.type(screen.getByLabelText("Model"), "Corolla");
    await user.type(screen.getByLabelText("Rok produkcji"), "2020");
    fireEvent.change(screen.getByLabelText("Data zakupu"), {
      target: { value: "2020-06-01" },
    });
    await user.click(screen.getByRole("button", { name: "Dodaj pojazd" }));

    await waitFor(() =>
      expect(api.post).toHaveBeenCalledWith(
        "/api/vehicles",
        expect.objectContaining({ purchasePrice: 0 }),
      ),
    );
  });
  it("shows an unspecified zero purchase price as Brak", async () => {
    vi.mocked(api.get).mockImplementation(async (path) => ({
      data: path.endsWith("cost-summary")
        ? {
            vehicleId: 1,
            totalMaintenanceCost: 0,
            averageMonthlyMaintenanceCost: null,
            maintenanceHistoryMonthCount: 0,
            hasMaintenanceHistory: false,
            fuel: {
              totalCost: 0,
              fuelEntryCount: 0,
              hasFuelData: false,
              averageConsumptionAvailable: false,
              averageConsumptionLitersPer100Km: null,
              monthlyTotals: [],
            },
            otherExpenses: { expenseCount: 0, totalAmount: 0, totalsByCategory: [], monthlyTotals: [] },
          }
        : { ...vehicle, purchasePrice: 0 },
    }));
    mount(
      <Routes>
        <Route path="/vehicles/:vehicleId" element={<VehicleDetailPage />} />
      </Routes>,
      "/vehicles/1",
    );

    expect(await screen.findByText("Brak")).toBeInTheDocument();
  });
  it("presents expense totals in PLN and handles unavailable consumption", async () => {
    vi.mocked(api.get).mockResolvedValue({
      data: {
        vehicleId: 1,
        totalMaintenanceCost: 170,
        averageMonthlyMaintenanceCost: 170,
        maintenanceHistoryMonthCount: 1,
        hasMaintenanceHistory: true,
        fuel: {
          totalCost: 0,
          fuelEntryCount: 0,
          hasFuelData: false,
          averageConsumptionAvailable: false,
          averageConsumptionLitersPer100Km: null,
          monthlyTotals: [],
        },
        otherExpenses: {
          expenseCount: 2,
          totalAmount: 150,
          totalsByCategory: [],
          monthlyTotals: [],
        },
      },
    });
    mount(<CostReport id={1} />);
    expect(await screen.findByText("150 PLN")).toBeInTheDocument();
    expect(screen.queryByText("EUR")).not.toBeInTheDocument();
    expect(screen.getAllByText("170 PLN")).toHaveLength(2);
    expect(screen.getByText("Brak danych")).toBeInTheDocument();
  });
  it("loads an existing vehicle and saves edits with PUT", async () => {
    vi.mocked(api.get).mockResolvedValue({ data: vehicle });
    vi.mocked(api.put).mockResolvedValue({
      data: { ...vehicle, model: "Yaris" },
    });
    mount(
      <Routes>
        <Route path="/vehicles/:vehicleId/edit" element={<VehicleFormPage />} />
        <Route path="/vehicles/:vehicleId" element={<p>Vehicle saved</p>} />
      </Routes>,
      "/vehicles/1/edit",
    );
    const model = await screen.findByLabelText("Model");
    fireEvent.change(model, { target: { value: "Yaris" } });
    fireEvent.click(screen.getByRole("button", { name: "Zapisz zmiany" }));
    await waitFor(() =>
      expect(api.put).toHaveBeenCalledWith(
        "/api/vehicles/1",
        expect.objectContaining({ model: "Yaris", currentOdometer: 50000 }),
      ),
    );
  });
  it("shows server validation errors at the related field", async () => {
    vi.mocked(api.get).mockResolvedValue({ data: vehicle });
    vi.mocked(api.put).mockRejectedValue(
      new ApiError("Błąd walidacji", 400, { make: "Nieprawidłowa marka" }),
    );
    mount(
      <Routes>
        <Route path="/vehicles/:vehicleId/edit" element={<VehicleFormPage />} />
        <Route path="/vehicles/:vehicleId" element={<p>Vehicle saved</p>} />
      </Routes>,
      "/vehicles/1/edit",
    );
    await screen.findByLabelText("Model");
    fireEvent.click(screen.getByRole("button", { name: "Zapisz zmiany" }));
    expect(await screen.findByText("Nieprawidłowa marka")).toBeInTheDocument();
    expect(screen.getByLabelText(/Marka/)).toHaveAttribute(
      "aria-invalid",
      "true",
    );
  });
  it("requires explicit delete confirmation and refreshes both caches", async () => {
    HTMLDialogElement.prototype.showModal = vi.fn(function (
      this: HTMLDialogElement,
    ) {
      this.setAttribute("open", "");
    });
    HTMLDialogElement.prototype.close = vi.fn();
    vi.mocked(api.delete).mockResolvedValue({});
    const client = mount(
      <DeleteVehicleDialog vehicle={vehicle} close={vi.fn()} />,
    );
    const invalidate = vi.spyOn(client, "invalidateQueries");
    expect(api.delete).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", { name: "Usuń pojazd" }));
    await waitFor(() =>
      expect(api.delete).toHaveBeenCalledWith("/api/vehicles/1"),
    );
    await waitFor(() =>
      expect(invalidate).toHaveBeenCalledWith({ queryKey: ["dashboard"] }),
    );
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["vehicles"] });
  });
});
