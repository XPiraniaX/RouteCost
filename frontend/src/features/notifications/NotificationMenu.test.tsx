import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useAuth } from "../auth/useAuth";
import { useNotificationActions, useNotifications, useUnreadNotificationCount } from "./api";
import { NotificationMenu } from "./NotificationMenu";

vi.mock("../auth/useAuth", () => ({ useAuth: vi.fn() }));
vi.mock("./api", () => ({
  useUnreadNotificationCount: vi.fn(), useNotifications: vi.fn(), useNotificationActions: vi.fn(),
}));

const mockedAuth = vi.mocked(useAuth);
const mockedCount = vi.mocked(useUnreadNotificationCount);
const mockedNotifications = vi.mocked(useNotifications);
const mockedActions = vi.mocked(useNotificationActions);
const markRead = { mutateAsync: vi.fn(), isPending: false, isError: false, error: null };
const markAllRead = { mutate: vi.fn(), isPending: false, isError: false, error: null };
const notification = {
  id: 9, title: "Przegląd techniczny", message: "Zbliża się termin: 2026-12-01.",
  vehicleId: 4, vehicleName: "Toyota Corolla", reminderId: 12,
  createdAt: "2026-11-18T10:00:00Z", read: false, readAt: null,
};

function mount() {
  render(<MemoryRouter><Routes>
    <Route path="/reminders/:vehicleId/:reminderId" element={<p>Szczegóły przypomnienia</p>} />
    <Route path="*" element={<><NotificationMenu /><div data-testid="location" /></>} />
  </Routes></MemoryRouter>);
}

beforeEach(() => {
  vi.resetAllMocks();
  mockedAuth.mockReturnValue({ user: { id: 3 } } as never);
  mockedCount.mockReturnValue({ data: 1 } as never);
  mockedNotifications.mockReturnValue({ isPending: false, isError: false, data: { content: [notification] } } as never);
  markRead.mutateAsync.mockResolvedValue({ ...notification, read: true });
  mockedActions.mockReturnValue({ markRead, markAllRead } as never);
});

describe("notification menu", () => {
  it("shows unread count, opens the list and navigates to its reminder after reading", async () => {
    mount();
    expect(screen.getByLabelText("1 nieprzeczytanych")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Powiadomienia" }));
    expect(await screen.findByText("Toyota Corolla")).toBeInTheDocument();
    expect(screen.getByText("Nieprzeczytane")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: /Przegląd techniczny/ }));
    await waitFor(() => expect(markRead.mutateAsync).toHaveBeenCalledWith(9));
    expect(await screen.findByText("Szczegóły przypomnienia")).toBeInTheDocument();
  });

  it("shows an empty state", () => {
    mockedNotifications.mockReturnValue({ isPending: false, isError: false, data: { content: [] } } as never);
    mount();
    fireEvent.click(screen.getByRole("button", { name: "Powiadomienia" }));
    expect(screen.getByText("Brak powiadomień")).toBeInTheDocument();
  });

  it("hides the unread badge when the count is zero", () => {
    mockedCount.mockReturnValue({ data: 0 } as never);
    mockedNotifications.mockReturnValue({ isPending: false, isError: false, data: { content: [{ ...notification, read: true }] } } as never);
    mount();
    expect(screen.queryByLabelText(/nieprzeczytanych/)).not.toBeInTheDocument();
  });

  it("marks all as read", () => {
    mockedNotifications.mockReturnValue({ isPending: false, isError: false, data: { content: [notification] } } as never);
    mount();
    fireEvent.click(screen.getByRole("button", { name: "Powiadomienia" }));
    fireEvent.click(screen.getByRole("button", { name: /Odczytaj wszystkie/ }));
    expect(markAllRead.mutate).toHaveBeenCalledOnce();
  });

  it("shows loading and API error states", () => {
    mockedNotifications.mockReturnValue({ isPending: true, isError: false } as never);
    mount();
    fireEvent.click(screen.getByRole("button", { name: "Powiadomienia" }));
    expect(screen.getByRole("status", { name: "Ładowanie danych" })).toBeInTheDocument();
  });

  it("renders API failures", () => {
    mockedNotifications.mockReturnValue({ isPending: false, isError: true, error: new Error("offline"), refetch: vi.fn() } as never);
    mount();
    fireEvent.click(screen.getByRole("button", { name: "Powiadomienia" }));
    expect(screen.getByText("offline")).toBeInTheDocument();
  });

  it("does not render for signed-out users", () => {
    mockedAuth.mockReturnValue({ user: null } as never);
    mount();
    expect(screen.queryByRole("button", { name: "Powiadomienia" })).not.toBeInTheDocument();
  });
});
