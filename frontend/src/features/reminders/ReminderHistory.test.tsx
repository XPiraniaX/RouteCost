import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import { ReminderHistory } from "./ReminderHistory";
import type { Reminder } from "./types";

const reminders: Reminder[] = [
  { id: 1, vehicleId: 2, type: "SERVICE", title: "Oil service", description: "Use 5W-30", dueDate: "2020-01-01", odometer: 80000, status: "ACTIVE", activationVersion: 0, createdAt: "2020-01-01T00:00:00Z" },
  { id: 2, vehicleId: 2, type: "INSURANCE", title: "Insurance", description: null, dueDate: "2999-01-01", odometer: null, status: "COMPLETED", activationVersion: 0, createdAt: "2020-01-01T00:00:00Z" },
  { id: 3, vehicleId: 2, type: "OTHER", title: "Cancelled", description: null, dueDate: "2999-01-01", odometer: null, status: "CANCELLED", activationVersion: 1, createdAt: "2020-01-01T00:00:00Z" },
];

describe("ReminderHistory", () => {
  it("renders reminder details and distinguishes overdue and final statuses", () => {
    render(<MemoryRouter><ReminderHistory reminders={reminders} vehicleId={2} busy={false} filtered={false} onStatusChange={vi.fn()} onReactivate={vi.fn()} onDelete={vi.fn()} /></MemoryRouter>);
    expect(screen.getByText("Oil service")).toBeInTheDocument();
    expect(screen.getByText("Use 5W-30")).toBeInTheDocument();
    expect(screen.getByText("Przeterminowane")).toBeInTheDocument();
    expect(screen.getByText("Wykonane")).toBeInTheDocument();
    expect(screen.getByText("Anulowane")).toBeInTheDocument();
    expect(screen.getByText(/80\s*000 km/)).toBeInTheDocument();
  });

  it("exposes completion, cancellation, reactivation, edit and delete actions", () => {
    const onStatusChange = vi.fn();
    const onReactivate = vi.fn();
    const onDelete = vi.fn();
    render(<MemoryRouter><ReminderHistory reminders={reminders} vehicleId={2} busy={false} filtered={false} onStatusChange={onStatusChange} onReactivate={onReactivate} onDelete={onDelete} /></MemoryRouter>);
    fireEvent.click(screen.getByRole("button", { name: "Oznacz jako wykonane: Oil service" }));
    expect(onStatusChange).toHaveBeenCalledWith(reminders[0], "COMPLETED");
    fireEvent.click(screen.getByRole("button", { name: "Aktywuj ponownie: Insurance" }));
    expect(onReactivate).toHaveBeenCalledWith(reminders[1]);
    fireEvent.click(screen.getByRole("button", { name: "Usuń przypomnienie: Oil service" }));
    expect(onDelete).toHaveBeenCalledWith(reminders[0]);
    expect(screen.getByRole("link", { name: "Edytuj przypomnienie: Oil service" })).toHaveAttribute("href", "/reminders/2/1/edit");
  });
});
