import { beforeEach, describe, expect, it, vi } from "vitest";
import { api } from "../../api/http";
import { reminderApi } from "./api";
import type { ReminderRequest } from "./types";

vi.mock("../../api/http", () => ({
  api: { get: vi.fn(), post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn() },
}));

const request: ReminderRequest = {
  type: "SERVICE",
  title: "Oil service",
  description: null,
  dueDate: "2026-05-10",
  odometer: 90000,
};

beforeEach(() => vi.resetAllMocks());

describe("reminder API contract", () => {
  it("requests the backend page with status/date filters and stable size-10 sorting", async () => {
    vi.mocked(api.get).mockResolvedValue({ data: { content: [] } });
    await reminderApi.list(7, 1, { status: "ACTIVE", dueFrom: "2026-01-01", dueTo: "2026-12-31" });
    expect(api.get).toHaveBeenCalledWith("/api/vehicles/7/reminders", {
      params: { page: 1, size: 10, sort: ["dueDate,asc", "id,asc"], status: "ACTIVE", dueFrom: "2026-01-01", dueTo: "2026-12-31" },
      paramsSerializer: { indexes: null },
    });
  });

  it("creates and updates reminders through their existing REST endpoints", async () => {
    vi.mocked(api.post).mockResolvedValue({ data: {} });
    vi.mocked(api.put).mockResolvedValue({ data: {} });
    await reminderApi.save(7, request);
    await reminderApi.save(7, request, 11);
    expect(api.post).toHaveBeenCalledWith("/api/vehicles/7/reminders", request);
    expect(api.put).toHaveBeenCalledWith("/api/vehicles/7/reminders/11", request);
  });

  it("changes status, uses the dedicated reactivation route, deletes, and propagates API errors", async () => {
    vi.mocked(api.patch).mockResolvedValue({ data: {} });
    vi.mocked(api.post).mockResolvedValue({ data: {} });
    vi.mocked(api.delete).mockResolvedValue({});
    await reminderApi.changeStatus(7, 11, "COMPLETED");
    await reminderApi.reactivate(7, 11);
    await reminderApi.remove(7, 11);
    expect(api.patch).toHaveBeenCalledWith("/api/vehicles/7/reminders/11/status", { status: "COMPLETED" });
    expect(api.post).toHaveBeenCalledWith("/api/vehicles/7/reminders/11/reactivate");
    expect(api.delete).toHaveBeenCalledWith("/api/vehicles/7/reminders/11");
    const failure = new Error("API unavailable");
    vi.mocked(api.get).mockRejectedValue(failure);
    await expect(reminderApi.get(7, 11)).rejects.toBe(failure);
  });
});
