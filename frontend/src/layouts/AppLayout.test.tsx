import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useAuth } from "../features/auth/useAuth";
import { AppLayout } from "./AppLayout";

vi.mock("../features/auth/useAuth", () => ({ useAuth: vi.fn() }));
vi.mock("../features/notifications/NotificationMenu", () => ({ NotificationMenu: () => <button aria-label="Powiadomienia">Powiadomienia</button> }));
const mockUseAuth = vi.mocked(useAuth);
const signOut = vi.fn();

function mount(path = "/", role: "USER" | "ADMIN" = "USER") {
  mockUseAuth.mockReturnValue({
    user: { firstName: "Demo", lastName: "RouteCost", email: "demo@routecost.local", role } as never,
    signOut,
    authError: null,
  } as never);

  render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route element={<AppLayout />}>
          <Route path="*" element={<p>Widok testowy</p>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );
}

beforeEach(() => vi.resetAllMocks());

describe("AppLayout", () => {
  it("keeps the current view title and removes secondary sidebar and footer content", () => {
    mount("/fuel");

    expect(screen.getAllByText("Tankowania")).toHaveLength(2);
    expect(screen.queryByText("Moja przestrzeń")).not.toBeInTheDocument();
    expect(screen.queryByText("TWOJA PRZESTRZEŃ")).not.toBeInTheDocument();
    expect(screen.queryByText("Mniej niewiadomych.")).not.toBeInTheDocument();
    expect(screen.queryByText("Twoje pojazdy, w jednym miejscu.")).not.toBeInTheDocument();
    expect(screen.queryByText("Twoja przestrzeń motoryzacyjna")).not.toBeInTheDocument();
    expect(screen.getByText("Widok testowy")).toBeInTheDocument();
  });

  it("shows administration only for an admin and preserves logout action", () => {
    mount("/admin", "ADMIN");

    expect(screen.getAllByText("Administracja")).toHaveLength(2);
    fireEvent.click(screen.getByRole("button", { name: "Wyloguj się" }));
    expect(signOut).toHaveBeenCalledOnce();
  });
});
