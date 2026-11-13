import {
  AxiosError,
  AxiosHeaders,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from "axios";
import { afterEach, describe, expect, it, vi } from "vitest";
import { api, setAccessToken, setUnauthorizedHandler } from "./http";

const originalAdapter = api.defaults.adapter;

function unauthorized(config: InternalAxiosRequestConfig): Promise<never> {
  const response: AxiosResponse = {
    data: { detail: "Unauthorized" },
    status: 401,
    statusText: "Unauthorized",
    headers: new AxiosHeaders(),
    config,
  };
  return Promise.reject(
    new AxiosError(
      "Unauthorized",
      "ERR_BAD_REQUEST",
      config,
      undefined,
      response,
    ),
  );
}

afterEach(() => {
  api.defaults.adapter = originalAdapter;
  setAccessToken(null);
  setUnauthorizedHandler(null);
});

describe("authenticated API client", () => {
  it("shares one refresh operation for concurrent 401 responses and retries both requests", async () => {
    const refresh = vi.fn();
    const calls = new Map<string, number>();
    setAccessToken("expired-access-token");
    api.defaults.adapter = async (config) => {
      const path = config.url ?? "";
      if (path.includes("/api/auth/refresh")) {
        refresh();
        await new Promise((resolve) => setTimeout(resolve, 15));
        return {
          data: { accessToken: "fresh-access-token" },
          status: 200,
          statusText: "OK",
          headers: new AxiosHeaders(),
          config,
        };
      }
      calls.set(path, (calls.get(path) ?? 0) + 1);
      if (
        AxiosHeaders.from(config.headers).get("Authorization") !==
        "Bearer fresh-access-token"
      ) {
        return unauthorized(config);
      }
      return {
        data: { path },
        status: 200,
        statusText: "OK",
        headers: new AxiosHeaders(),
        config,
      };
    };

    const [first, second] = await Promise.all([
      api.get("/api/dashboard/summary"),
      api.get("/api/vehicles"),
    ]);

    expect(refresh).toHaveBeenCalledTimes(1);
    expect(calls.get("/api/dashboard/summary")).toBe(2);
    expect(calls.get("/api/vehicles")).toBe(2);
    expect(first.data.path).toBe("/api/dashboard/summary");
    expect(second.data.path).toBe("/api/vehicles");
  });

  it("does not retry the refresh endpoint when its own request returns 401", async () => {
    const refresh = vi.fn();
    const unauthorizedHandler = vi.fn();
    setUnauthorizedHandler(unauthorizedHandler);
    setAccessToken(null);
    api.defaults.adapter = async (config) => {
      if (config.url?.includes("/api/auth/refresh")) refresh();
      return unauthorized(config);
    };

    await expect(api.get("/api/dashboard/summary")).rejects.toMatchObject({
      status: 401,
    });
    expect(refresh).toHaveBeenCalledTimes(1);
    expect(unauthorizedHandler).toHaveBeenCalledTimes(1);
  });
});
