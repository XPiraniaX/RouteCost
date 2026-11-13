import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  AxiosHeaders,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from "axios";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it } from "vitest";
import { api, setAccessToken, setUnauthorizedHandler } from "../../api/http";
import { AuthProvider } from "./AuthProvider";
import { useAuth } from "./useAuth";

const originalAdapter = api.defaults.adapter;
const profile = {
  id: 7,
  email: "session@example.test",
  firstName: "Session",
  lastName: "User",
  role: "USER" as const,
};

function SessionState() {
  const auth = useAuth();
  if (auth.isInitializing) return <p>Checking session</p>;
  return (
    <>
      <p>{auth.user?.email ?? "Signed out"}</p>
      <button onClick={() => void auth.signOut()}>Sign out</button>
    </>
  );
}

function response<T>(
  config: InternalAxiosRequestConfig,
  data: T,
  status = 200,
): AxiosResponse<T> {
  return {
    config,
    data,
    status,
    statusText: "OK",
    headers: new AxiosHeaders(),
  };
}

afterEach(() => {
  api.defaults.adapter = originalAdapter;
  setAccessToken(null);
  setUnauthorizedHandler(null);
});

describe("AuthProvider persistent session", () => {
  it("restores the user with the refresh cookie on application startup and logs out on the server", async () => {
    const requests: string[] = [];
    api.defaults.adapter = async (config) => {
      requests.push(config.url ?? "");
      if (config.url?.includes("/api/auth/refresh")) {
        expect(config.withCredentials).toBe(true);
        expect(AxiosHeaders.from(config.headers).get("X-RouteCost-Auth")).toBe(
          "1",
        );
        return response(config, { accessToken: "restored-access-token" });
      }
      if (config.url?.includes("/api/auth/me")) {
        expect(AxiosHeaders.from(config.headers).get("Authorization")).toBe(
          "Bearer restored-access-token",
        );
        return response(config, profile);
      }
      if (config.url?.includes("/api/auth/logout"))
        return response(config, undefined, 204);
      throw new Error(`Unexpected request ${config.url}`);
    };
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
    const user = userEvent.setup();
    render(
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <SessionState />
        </AuthProvider>
      </QueryClientProvider>,
    );

    expect(await screen.findByText(profile.email)).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Sign out" }));
    await waitFor(() =>
      expect(screen.getByText("Signed out")).toBeInTheDocument(),
    );
    expect(requests).toEqual([
      "/api/auth/refresh",
      "/api/auth/me",
      "/api/auth/logout",
    ]);
    queryClient.clear();
  });
});
