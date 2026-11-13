import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ProtectedRoute } from "./ProtectedRoute";

const authState = vi.hoisted(() => ({ isAuthenticated: false, isInitializing: false }));

vi.mock("../features/auth/useAuth", () => ({
  useAuth: () => ({ isAuthenticated: authState.isAuthenticated, isInitializing: authState.isInitializing }),
}));

describe("ProtectedRoute", () => {
  beforeEach(() => {
    authState.isAuthenticated = false;
    authState.isInitializing = false;
  });

  it.each([
    "/",
    "/vehicles",
    "/vehicles/new",
    "/vehicles/1",
    "/vehicles/1/edit",
    "/fuel",
    "/fuel/1/new",
    "/fuel/1/10/edit",
  ])("redirects an unauthenticated visitor from %s to login", (path) => {
    render(
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route element={<ProtectedRoute />}>
            <Route path="/*" element={<p>Private home</p>} />
          </Route>
          <Route path="/login" element={<p>Login page</p>} />
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByText("Login page")).toBeInTheDocument();
    expect(screen.queryByText("Private home")).not.toBeInTheDocument();
  });

  it("renders the protected route for an authenticated visitor", () => {
    authState.isAuthenticated = true;
    render(
      <MemoryRouter initialEntries={["/"]}>
        <Routes>
          <Route element={<ProtectedRoute />}>
            <Route path="/" element={<p>Private home</p>} />
          </Route>
          <Route path="/login" element={<p>Login page</p>} />
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByText("Private home")).toBeInTheDocument();
    expect(screen.queryByText("Login page")).not.toBeInTheDocument();
  });

  it("waits for session restoration before deciding to redirect", () => {
    authState.isInitializing = true;
    render(
      <MemoryRouter initialEntries={["/"]}>
        <Routes>
          <Route element={<ProtectedRoute />}>
            <Route path="/" element={<p>Private home</p>} />
          </Route>
          <Route path="/login" element={<p>Login page</p>} />
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByRole("status", { name: "Ładowanie danych" })).toBeInTheDocument();
    expect(screen.queryByText("Login page")).not.toBeInTheDocument();
    expect(screen.queryByText("Private home")).not.toBeInTheDocument();
  });
});
