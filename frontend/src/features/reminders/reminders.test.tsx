import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { StrictMode } from "react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { api } from "../../api/http";
import { ApiError } from "../../api/errors";
import { ReminderForm } from "./ReminderFormPage";
import { RemindersContent } from "./RemindersContent";
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
  id: 5, vehicleId: 1, type: "SERVICE", title: "Oil service", description: null,
  dueDate: "2026-12-01", odometer: 95000, status: "ACTIVE",
  activationVersion: 0, createdAt: "2026-01-01T00:00:00Z",
};
const page = { content: [reminder], page: 0, size: 10, totalElements: 1, totalPages: 1 };

function mount(element: React.ReactNode, path = "/") {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  render(<StrictMode><QueryClientProvider client={client}><MemoryRouter initialEntries={[path]}>{element}</MemoryRouter></QueryClientProvider></StrictMode>);
  return client;
}

beforeEach(() => {
  vi.resetAllMocks();
  HTMLDialogElement.prototype.showModal = vi.fn(function (this: HTMLDialogElement) { this.setAttribute("open", ""); });
  HTMLDialogElement.prototype.close = vi.fn(function (this: HTMLDialogElement) {
    this.removeAttribute("open");
    this.dispatchEvent(new Event("close"));
  });
  vi.mocked(api.get).mockResolvedValue({ data: page });
});

describe("RemindersContent", () => {
  it("renders entries, filters by status and invokes status transitions", async () => {
    mount(<RemindersContent vehicle={vehicle} />, "/reminders?vehicleId=1");
    expect(await screen.findByText("Oil service")).toBeInTheDocument();
    expect(screen.getByText(/1 wpis ·/)).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Status"), { target: { value: "ACTIVE" } });
    await waitFor(() => expect(api.get).toHaveBeenLastCalledWith("/api/vehicles/1/reminders", expect.objectContaining({ params: expect.objectContaining({ status: "ACTIVE", size: 10 }) })));
    fireEvent.click(await screen.findByRole("button", { name: "Oznacz jako wykonane: Oil service" }));
    await waitFor(() => expect(api.patch).toHaveBeenCalledWith("/api/vehicles/1/reminders/5/status", { status: "COMPLETED" }));
  });

  it("uses a Polish plural and preserves the empty state without pagination", async () => {
    vi.mocked(api.get).mockResolvedValue({
      data: { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 },
    });
    mount(<RemindersContent vehicle={vehicle} />, "/reminders?vehicleId=1");
    expect(await screen.findByText("Nie masz jeszcze przypomnień")).toBeInTheDocument();
    expect(screen.getByText(/0 wpisów ·/)).toBeInTheDocument();
    expect(screen.queryByText(/Strona 0 z 0/)).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Poprzednia/ })).not.toBeInTheDocument();
  });

  it("requires confirmation before deleting and shows request errors", async () => {
    mount(<RemindersContent vehicle={vehicle} />, "/reminders?vehicleId=1");
    fireEvent.click(await screen.findByRole("button", { name: "Usuń przypomnienie: Oil service" }));
    const dialog = screen.getByRole("dialog");
    expect(api.delete).not.toHaveBeenCalled();
    vi.mocked(api.delete).mockRejectedValueOnce(new ApiError("Delete failed", 500));
    fireEvent.click(within(dialog).getByRole("button", { name: "Usuń przypomnienie" }));
    expect(await screen.findByText("Delete failed")).toBeInTheDocument();
    expect(api.delete).toHaveBeenCalledWith("/api/vehicles/1/reminders/5");
  });

  it("removes the final row after successful DELETE and refreshes the query", async () => {
    vi.mocked(api.get)
      .mockResolvedValueOnce({ data: page })
      .mockResolvedValue({ data: { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 } });
    vi.mocked(api.delete).mockResolvedValue({ status: 204 });
    const client = mount(<RemindersContent vehicle={vehicle} />, "/reminders?vehicleId=1");
    const invalidate = vi.spyOn(client, "invalidateQueries");
    fireEvent.click(await screen.findByRole("button", { name: "Usuń przypomnienie: Oil service" }));
    fireEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "Usuń przypomnienie" }));
    expect(await screen.findByText("Nie masz jeszcze przypomnień")).toBeInTheDocument();
    expect(api.delete).toHaveBeenCalledWith("/api/vehicles/1/reminders/5");
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["reminders", 1] });
  });

  it("shows list loading failures", async () => {
    vi.mocked(api.get).mockRejectedValueOnce(new ApiError("List unavailable", 503));
    mount(<RemindersContent vehicle={vehicle} />, "/reminders?vehicleId=1");
    expect(await screen.findByText("List unavailable")).toBeInTheDocument();
  });
});

describe("ReminderForm", () => {
  it("validates required fields, then creates a reminder using the backend DTO", async () => {
    vi.mocked(api.post).mockResolvedValue({ data: reminder });
    mount(<ReminderForm vehicle={vehicle} />);
    fireEvent.click(screen.getByRole("button", { name: "Dodaj przypomnienie" }));
    expect(await screen.findByText("Podaj tytuł przypomnienia.")).toBeInTheDocument();
    expect(api.post).not.toHaveBeenCalled();
    fireEvent.change(screen.getByPlaceholderText("np. Przegląd techniczny"), { target: { value: "Annual service" } });
    fireEvent.click(screen.getByRole("button", { name: "Dodaj przypomnienie" }));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/vehicles/1/reminders", expect.objectContaining({ type: "OTHER", title: "Annual service", description: null, dueDate: expect.any(String), odometer: null })));
  });

  it("loads existing values for editing and maps backend validation errors", async () => {
    vi.mocked(api.put).mockRejectedValue(new ApiError("Invalid reminder", 400, { title: "Title rejected" }));
    mount(<ReminderForm vehicle={vehicle} reminder={reminder} />);
    expect(screen.getByPlaceholderText("np. Przegląd techniczny")).toHaveValue("Oil service");
    fireEvent.change(screen.getByPlaceholderText("np. Przegląd techniczny"), { target: { value: "Updated service" } });
    fireEvent.click(screen.getByRole("button", { name: "Zapisz zmiany" }));
    await waitFor(() => expect(api.put).toHaveBeenCalledWith("/api/vehicles/1/reminders/5", expect.objectContaining({ title: "Updated service", type: "SERVICE", dueDate: "2026-12-01" })));
    expect(await screen.findByText("Title rejected")).toBeInTheDocument();
  });
});
