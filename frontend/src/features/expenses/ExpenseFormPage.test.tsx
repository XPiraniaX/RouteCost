import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ExpenseFormPage } from "./ExpenseFormPage";

const mocks = vi.hoisted(() => ({
  useVehicle: vi.fn(),
  useExpense: vi.fn(),
  useSaveExpense: vi.fn(),
  mutate: vi.fn(),
}));

vi.mock("../vehicles/api", () => ({ useVehicle: mocks.useVehicle }));
vi.mock("./api", () => ({
  expenseApi: { get: vi.fn() },
  useExpense: mocks.useExpense,
  useSaveExpense: mocks.useSaveExpense,
}));

const vehicle = {
  id: 7,
  make: "Toyota",
  model: "Corolla",
  productionYear: 2019,
  fuelType: "PETROL",
  currentOdometer: 80000,
  purchaseDate: "2020-01-01",
  purchasePrice: 0,
  createdAt: "2025-01-01T00:00:00Z",
};

const expense = {
  id: 12,
  vehicleId: 7,
  expenseDate: "2025-05-01",
  category: "REPAIR",
  amount: 120,
  description: "Serwis",
  odometer: 75000,
  createdAt: "2025-05-01T00:00:00Z",
};

function mount(path: string, entry: string) {
  return render(
    <MemoryRouter initialEntries={[entry]}>
      <Routes>
        <Route path={path} element={<ExpenseFormPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("expense form", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.useVehicle.mockReturnValue({ data: vehicle, isPending: false, isError: false });
    mocks.useExpense.mockReturnValue({ data: undefined, isPending: false, isError: false });
    mocks.useSaveExpense.mockReturnValue({ isPending: false, isError: false, mutate: mocks.mutate });
  });

  it("creates an expense in PLN without exposing a currency field", async () => {
    mount("/expenses/:vehicleId/new", "/expenses/7/new");
    expect(screen.queryByLabelText(/waluta/i)).not.toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Kwota (PLN)"), { target: { value: "125.50" } });
    fireEvent.click(screen.getByRole("button", { name: "Dodaj wydatek" }));

    await waitFor(() => expect(mocks.mutate).toHaveBeenCalled());
    const payload = mocks.mutate.mock.calls[0]?.[0];
    expect(payload).toMatchObject({ amount: 125.5 });
    expect(payload).not.toHaveProperty("currency");
  });

  it("edits an existing expense without a currency field", async () => {
    mocks.useExpense.mockReturnValue({ data: expense, isPending: false, isError: false });
    mount("/expenses/:vehicleId/:expenseId/edit", "/expenses/7/12/edit");
    expect(screen.queryByLabelText(/waluta/i)).not.toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Kwota (PLN)"), { target: { value: "140.00" } });
    fireEvent.click(screen.getByRole("button", { name: "Zapisz zmiany" }));

    await waitFor(() => expect(mocks.mutate).toHaveBeenCalled());
    const payload = mocks.mutate.mock.calls[0]?.[0];
    expect(payload).toMatchObject({ amount: 140 });
    expect(payload).not.toHaveProperty("currency");
  });
});
