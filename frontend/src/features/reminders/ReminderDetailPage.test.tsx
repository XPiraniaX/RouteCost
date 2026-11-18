import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { StrictMode } from "react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "../../api/errors";
import { api } from "../../api/http";
import { ReminderDetailPage } from "./ReminderDetailPage";
import type { Reminder } from "./types";
import type { Vehicle } from "../vehicles/api";

vi.mock("../../api/http", () => ({
  api: { get: vi.fn(), post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn() },
}));

const vehicle: Vehicle = {
  id: 1, make: "Toyota", model: "Corolla", productionYear: 2020,
  fuelType: "PETROL", currentOdometer: 90000, purchaseDate: "2020-01-01",
  purchasePrice: 50000, createdAt: "2026-01-01T00:00:00Z",
};
const reminder: Reminder = {
  id: 5, vehicleId: 1, type: "SERVICE", title: "Oil service", description: "Annual maintenance",
  dueDate: "2026-12-01", odometer: 95000, status: "ACTIVE",
  activationVersion: 1, createdAt: "2026-01-01T00:00:00Z",
};
let currentStatus: Reminder["status"];

function mount() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  render(<StrictMode><QueryClientProvider client={client}><MemoryRouter initialEntries={["/reminders/1/5"]}>
    <Routes>
      <Route path="/reminders/:vehicleId/:reminderId" element={<ReminderDetailPage />} />
      <Route path="/reminders" element={<p>Lista przypomnień</p>} />
    </Routes>
  </MemoryRouter></QueryClientProvider></StrictMode>);
  return client;
}

beforeEach(() => {
  vi.resetAllMocks();
  currentStatus = "ACTIVE";
  HTMLDialogElement.prototype.showModal = vi.fn(function (this: HTMLDialogElement) { this.setAttribute("open", ""); });
  HTMLDialogElement.prototype.close = vi.fn(function (this: HTMLDialogElement) {
    this.removeAttribute("open");
    this.dispatchEvent(new Event("close"));
  });
  vi.mocked(api.get).mockImplementation(async (url) => {
    if (url === "/api/vehicles/1") return { data: vehicle } as never;
    if (url === "/api/vehicles/1/reminders/5") return { data: { ...reminder, status: currentStatus } } as never;
    throw new Error(`Unexpected GET ${url}`);
  });
  vi.mocked(api.patch).mockImplementation(async (_url, body) => {
    currentStatus = (body as { status: Reminder["status"] }).status;
    return { data: { ...reminder, status: currentStatus } } as never;
  });
  vi.mocked(api.post).mockImplementation(async () => {
    currentStatus = "ACTIVE";
    return { data: { ...reminder, status: currentStatus } } as never;
  });
  vi.mocked(api.delete).mockResolvedValue({ status: 204 } as never);
});

describe("ReminderDetailPage actions", () => {
  it("shows edit, complete, cancel and delete for an active reminder", async () => {
    mount();
    expect(await screen.findByText("Oil service")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Edytuj" })).toHaveAttribute("href", "/reminders/1/5/edit");
    expect(screen.getByRole("button", { name: "Oznacz jako wykonane" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Anuluj" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Usuń" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Aktywuj ponownie" })).not.toBeInTheDocument();
  });

  it.each(["COMPLETED", "CANCELLED"] as const)("shows reactivate, edit and delete for %s reminders", async (status) => {
    currentStatus = status;
    mount();
    expect(await screen.findByText("Oil service")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Aktywuj ponownie" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Edytuj" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Usuń" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Oznacz jako wykonane" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Anuluj" })).not.toBeInTheDocument();
  });

  it("marks an active reminder as completed and refreshes its status", async () => {
    mount();
    fireEvent.click(await screen.findByRole("button", { name: "Oznacz jako wykonane" }));
    await waitFor(() => expect(api.patch).toHaveBeenCalledWith("/api/vehicles/1/reminders/5/status", { status: "COMPLETED" }));
    expect(await screen.findByText("Wykonane")).toBeInTheDocument();
    expect(await screen.findByText("Przypomnienie oznaczono jako wykonane.")).toBeInTheDocument();
  });

  it("cancels an active reminder", async () => {
    mount();
    fireEvent.click(await screen.findByRole("button", { name: "Anuluj" }));
    await waitFor(() => expect(api.patch).toHaveBeenCalledWith("/api/vehicles/1/reminders/5/status", { status: "CANCELLED" }));
    expect(await screen.findByText("Anulowane")).toBeInTheDocument();
    expect(await screen.findByText("Przypomnienie zostało anulowane.")).toBeInTheDocument();
  });

  it("reactivates completed or cancelled reminders", async () => {
    currentStatus = "COMPLETED";
    mount();
    fireEvent.click(await screen.findByRole("button", { name: "Aktywuj ponownie" }));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/vehicles/1/reminders/5/reactivate"));
    expect(await screen.findByText("Aktywne")).toBeInTheDocument();
    expect(await screen.findByText("Przypomnienie zostało ponownie aktywowane.")).toBeInTheDocument();
  });

  it("requires confirmation before delete and navigates back to the vehicle reminder list", async () => {
    mount();
    fireEvent.click(await screen.findByRole("button", { name: "Usuń" }));
    const dialog = screen.getByRole("dialog");
    expect(api.delete).not.toHaveBeenCalled();
    fireEvent.click(within(dialog).getByRole("button", { name: "Usuń przypomnienie" }));
    expect(await screen.findByText("Lista przypomnień")).toBeInTheDocument();
    expect(api.delete).toHaveBeenCalledWith("/api/vehicles/1/reminders/5");
  });

  it("shows status and delete failures, and keeps the delete dialog open", async () => {
    vi.mocked(api.patch).mockRejectedValueOnce(new ApiError("Status update failed", 409));
    mount();
    fireEvent.click(await screen.findByRole("button", { name: "Oznacz jako wykonane" }));
    expect(await screen.findByText("Status update failed")).toBeInTheDocument();

    vi.mocked(api.delete).mockRejectedValueOnce(new ApiError("Delete failed", 500));
    fireEvent.click(screen.getByRole("button", { name: "Usuń" }));
    fireEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "Usuń przypomnienie" }));
    expect(await screen.findByText("Delete failed")).toBeInTheDocument();
    expect(screen.getByRole("dialog")).toBeInTheDocument();
  });

  it("shows a loading state while the detail is loading", () => {
    vi.mocked(api.get).mockReturnValue(new Promise(() => {}) as never);
    mount();
    expect(screen.getByRole("status", { name: "Ładowanie danych" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Anuluj" })).not.toBeInTheDocument();
  });
});
