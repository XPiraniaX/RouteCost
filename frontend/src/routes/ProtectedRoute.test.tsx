import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ProtectedRoute } from "./ProtectedRoute";

const authState = vi.hoisted(() => ({ isAuthenticated: false }));

vi.mock("../features/auth/useAuth", () => ({
  useAuth: () => ({ isAuthenticated: authState.isAuthenticated }),
}));

describe("ProtectedRoute", () => {
  beforeEach(() => {
    authState.isAuthenticated = false;
  });

  it.each([
    "/",
    "/vehicles",
    "/vehicles/new",
    "/vehicles/1",
    "/vehicles/1/edit",
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
});
