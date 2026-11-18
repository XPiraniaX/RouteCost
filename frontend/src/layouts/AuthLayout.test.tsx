import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { AuthLayout } from "./AuthLayout";

function mountAuthLayout(path = "/login") {
  render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route element={<AuthLayout />}>
          <Route path="/login" element={<div>Login form</div>} />
          <Route path="/register" element={<div>Register form</div>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );
}

describe("AuthLayout", () => {
  it("shows shared branding and the login content", () => {
    mountAuthLayout();

    expect(screen.getByRole("heading", { name: "RouteCost", level: 1 })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Witaj ponownie", level: 2 })).toBeInTheDocument();
    expect(screen.getByText("Login form")).toBeInTheDocument();
    expect(screen.queryByText("Bezpieczny dostęp do Twojego konta")).not.toBeInTheDocument();
    expect(screen.queryByText("Każda trasa")).not.toBeInTheDocument();
    expect(screen.queryByText("Stworzone z myślą o spokojnej drodze.")).not.toBeInTheDocument();
  });

  it("switches between the login and registration content", () => {
    mountAuthLayout();

    fireEvent.click(screen.getByRole("link", { name: /Utwórz konto/ }));
    expect(screen.getByRole("heading", { name: "Dołącz do RouteCost", level: 2 })).toBeInTheDocument();
    expect(screen.getByText("Register form")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("link", { name: /Zaloguj się/ }));
    expect(screen.getByRole("heading", { name: "Witaj ponownie", level: 2 })).toBeInTheDocument();
    expect(screen.getByText("Login form")).toBeInTheDocument();
  });
});
