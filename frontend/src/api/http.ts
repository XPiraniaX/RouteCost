import axios, { AxiosHeaders, type InternalAxiosRequestConfig } from "axios";
import { toApiError } from "./errors";

const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim();

export const api = axios.create({
  baseURL: configuredBaseUrl || "http://localhost:8080",
  headers: { "Content-Type": "application/json" },
  timeout: 15_000,
  withCredentials: true,
});

let accessToken: string | null = null;
let unauthorizedHandler: (() => void) | null = null;
let refreshPromise: Promise<string | null> | null = null;

type RetriableRequest = InternalAxiosRequestConfig & {
  routeCostRetried?: boolean;
};

export function setAccessToken(token: string | null): void {
  accessToken = token;
}

export function setUnauthorizedHandler(handler: (() => void) | null): void {
  unauthorizedHandler = handler;
}

export function refreshAccessToken(): Promise<string | null> {
  if (!refreshPromise) {
    refreshPromise = api
      .post<{ accessToken: string }>("/api/auth/refresh", null, {
        headers: { "X-RouteCost-Auth": "1" },
        withCredentials: true,
      })
      .then((response) => {
        setAccessToken(response.data.accessToken);
        return response.data.accessToken;
      })
      .catch(() => {
        setAccessToken(null);
        unauthorizedHandler?.();
        return null;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

api.interceptors.request.use((config) => {
  if (accessToken) {
    config.headers = AxiosHeaders.from(config.headers);
    config.headers.set("Authorization", `Bearer ${accessToken}`);
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error: unknown) => {
    const apiError = toApiError(error);
    if (
      !axios.isAxiosError(error) ||
      apiError.status !== 401 ||
      !error.config
    ) {
      return Promise.reject(apiError);
    }

    const request = error.config as RetriableRequest;
    const url = request.url ?? "";
    if (
      [
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/refresh",
        "/api/auth/logout",
      ].some((path) => url.includes(path))
    ) {
      return Promise.reject(apiError);
    }

    if (request.routeCostRetried) {
      setAccessToken(null);
      unauthorizedHandler?.();
      return Promise.reject(apiError);
    }
    request.routeCostRetried = true;

    const sentAuthorization = AxiosHeaders.from(request.headers).get(
      "Authorization",
    );
    const token =
      accessToken && sentAuthorization !== `Bearer ${accessToken}`
        ? accessToken
        : await refreshAccessToken();
    if (!token) return Promise.reject(apiError);

    request.headers = AxiosHeaders.from(request.headers);
    request.headers.set("Authorization", `Bearer ${token}`);
    return api.request(request);
  },
);
