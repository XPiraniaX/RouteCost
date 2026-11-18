import axios from "axios";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { api } from "../../api/http";
import { expenseApi } from "./api";

vi.mock("../../api/http", () => ({ api: { get: vi.fn() } }));

describe("expense list sorting", () => {
  beforeEach(() => vi.clearAllMocks());

  it.each([
    ["desc", "sort=expenseDate,desc&sort=id,desc"],
    ["asc", "sort=expenseDate,asc&sort=id,asc"],
  ] as const)("sends %s sorting through Spring Pageable", async (order, expectedSort) => {
    vi.mocked(api.get).mockResolvedValue({ data: { content: [] } });
    await expenseApi.list(7, 0, order, {
      category: "REPAIR",
      dateFrom: "2025-01-01",
      dateTo: "2025-12-31",
    });

    const [url, config] = vi.mocked(api.get).mock.calls[0]!;
    const requestUrl = axios.getUri({ url, ...config });
    expect(requestUrl).toContain(expectedSort);
    expect(requestUrl).toContain("category=REPAIR");
    expect(requestUrl).toContain("dateFrom=2025-01-01");
    expect(requestUrl).toContain("dateTo=2025-12-31");
    expect(requestUrl).not.toContain("sort%5B%5D");
  });
});
