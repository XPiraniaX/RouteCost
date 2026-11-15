import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "../../api/errors";
import { api } from "../../api/http";
import { AdminPage } from "./AdminPage";

vi.mock("../../api/http", () => ({
  api: { get: vi.fn(), patch: vi.fn() },
}));
vi.mock("../auth/useAuth", () => ({
  useAuth: () => ({ user: { id: 1, role: "ADMIN" } }),
}));

const users = {
  content: [
    {
      id: 2,
      email: "user@example.test",
      firstName: "Test",
      lastName: "User",
      role: "USER" as const,
      enabled: true,
      createdAt: "2026-01-01T10:00:00Z",
    },
  ],
  page: 0,
  size: 10,
  totalElements: 1,
  totalPages: 1,
};
const dashboard = {
  totalUsers: 12,
  activeUsers: 10,
  inactiveUsers: 2,
  adminUsers: 1,
  totalVehicles: 20,
  totalFuelEntries: 30,
  totalExpenses: 40,
  totalReminders: 50,
};

function mountAdmin() {
  HTMLDialogElement.prototype.showModal = vi.fn(function (
    this: HTMLDialogElement,
  ) {
    this.setAttribute("open", "");
  });
  HTMLDialogElement.prototype.close = vi.fn();
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <AdminPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
  return client;
}

beforeEach(() => {
  vi.resetAllMocks();
  vi.mocked(api.get).mockImplementation(async (path) => ({
    data: path === "/api/admin/dashboard" ? dashboard : users,
  }));
});

describe("admin user management", () => {
  it("loads global statistics and the user table", async () => {
    mountAdmin();
    expect(await screen.findByText("user@example.test")).toBeInTheDocument();
    expect(screen.getByText("Aktywne konta")).toBeInTheDocument();
    expect(api.get).toHaveBeenCalledWith("/api/admin/dashboard");
    expect(api.get).toHaveBeenCalledWith("/api/admin/users", {
      params: { page: 0, size: 10, sort: "createdAt,desc" },
    });
  });

  it("confirms role changes and sends the admin API request", async () => {
    vi.mocked(api.patch).mockResolvedValue({ data: { ...users.content[0], role: "ADMIN" } });
    mountAdmin();
    await screen.findByText("user@example.test");

    fireEvent.click(screen.getByRole("button", { name: "Nadaj ADMIN" }));
    fireEvent.click(screen.getByRole("button", { name: "Zmień rolę" }));

    await waitFor(() =>
      expect(api.patch).toHaveBeenCalledWith("/api/admin/users/2/role", {
        role: "ADMIN",
      }),
    );
  });

  it("shows HTTP 409 as a readable confirmation error", async () => {
    vi.mocked(api.patch).mockRejectedValue(
      new ApiError("System musi zachować aktywnego administratora.", 409),
    );
    mountAdmin();
    await screen.findByText("user@example.test");

    fireEvent.click(screen.getByRole("button", { name: "Nadaj ADMIN" }));
    fireEvent.click(screen.getByRole("button", { name: "Zmień rolę" }));

    expect(
      await screen.findByText("System musi zachować aktywnego administratora."),
    ).toBeInTheDocument();
  });

  it("filters and paginates the user list through the API", async () => {
    vi.mocked(api.get).mockImplementation(async (path, config) => {
      if (path === "/api/admin/dashboard") return { data: dashboard };
      const params = config?.params as Record<string, unknown> | undefined;
      return {
        data: {
          ...users,
          page: params?.page ?? 0,
          totalElements: 21,
          totalPages: 3,
        },
      };
    });
    mountAdmin();
    await screen.findByText("user@example.test");

    fireEvent.change(screen.getByRole("searchbox", { name: "Szukaj użytkowników" }), {
      target: { value: "lookup" },
    });
    fireEvent.change(screen.getByLabelText("Rola"), { target: { value: "ADMIN" } });
    fireEvent.change(screen.getByLabelText("Status konta"), { target: { value: "false" } });
    await waitFor(() =>
      expect(api.get).toHaveBeenLastCalledWith("/api/admin/users", {
        params: {
          page: 0,
          size: 10,
          sort: "createdAt,desc",
          search: "lookup",
          role: "ADMIN",
          enabled: false,
        },
      }),
    );
    expect(await screen.findByText("21 kont · strona 1 z 3")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Następna" }));
    await waitFor(() =>
      expect(api.get).toHaveBeenLastCalledWith("/api/admin/users", {
        params: {
          page: 1,
          size: 10,
          sort: "createdAt,desc",
          search: "lookup",
          role: "ADMIN",
          enabled: false,
        },
      }),
    );
  });

  it("confirms account activation and sends the status change", async () => {
    vi.mocked(api.get).mockImplementation(async (path) => ({
      data: path === "/api/admin/dashboard"
        ? dashboard
        : { ...users, content: [{ ...users.content[0], enabled: false }] },
    }));
    vi.mocked(api.patch).mockResolvedValue({ data: { ...users.content[0], enabled: true } });
    mountAdmin();
    await screen.findByText("user@example.test");

    fireEvent.click(screen.getByRole("button", { name: "Aktywuj" }));
    fireEvent.click(screen.getByRole("button", { name: "Aktywuj konto" }));

    await waitFor(() =>
      expect(api.patch).toHaveBeenCalledWith("/api/admin/users/2/status", {
        enabled: true,
      }),
    );
  });
});
