import { api } from "./http";
import type {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  UserProfile,
} from "../types/auth";

export async function login(request: LoginRequest): Promise<AuthResponse> {
  const response = await api.post<AuthResponse>("/api/auth/login", request);
  return response.data;
}

export async function register(
  request: RegisterRequest,
): Promise<AuthResponse> {
  const response = await api.post<AuthResponse>("/api/auth/register", request);
  return response.data;
}

export async function getCurrentUser(): Promise<UserProfile> {
  const response = await api.get<UserProfile>("/api/auth/me");
  return response.data;
}

export async function logoutSession(): Promise<void> {
  await api.post("/api/auth/logout", null, {
    headers: { "X-RouteCost-Auth": "1" },
    withCredentials: true,
  });
}
