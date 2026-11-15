import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useAuth } from "../features/auth/useAuth";
import { AdminRoute } from "./AdminRoute";

vi.mock("../features/auth/useAuth", () => ({ useAuth: vi.fn() }));
const mockUseAuth = vi.mocked(useAuth);

function mount() {
  render(
    <MemoryRouter initialEntries={["/admin"]}>
      <Routes>
        <Route path="/" element={<p>User dashboard</p>} />
        <Route element={<AdminRoute />}>
          <Route path="/admin" element={<p>Admin dashboard</p>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );
}

beforeEach(() => vi.resetAllMocks());

describe("AdminRoute", () => {
  it("redirects a USER away from the admin screen", () => {
    mockUseAuth.mockReturnValue({
      user: { id: 2, role: "USER" } as never,
      isInitializing: false,
    } as never);
    mount();
    expect(screen.getByText("User dashboard")).toBeInTheDocument();
    expect(screen.queryByText("Admin dashboard")).not.toBeInTheDocument();
  });

  it("renders the admin screen for an ADMIN", () => {
    mockUseAuth.mockReturnValue({
      user: { id: 1, role: "ADMIN" } as never,
      isInitializing: false,
    } as never);
    mount();
    expect(screen.getByText("Admin dashboard")).toBeInTheDocument();
  });
});
