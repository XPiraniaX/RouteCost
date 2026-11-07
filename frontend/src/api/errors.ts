import axios from "axios";

export type FieldErrors = Record<string, string>;

export class ApiError extends Error {
  readonly status?: number;
  readonly fieldErrors: FieldErrors;

  constructor(message: string, status?: number, fieldErrors: FieldErrors = {}) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

function readFieldErrors(value: unknown): FieldErrors {
  if (!value || typeof value !== "object" || Array.isArray(value)) return {};

  return Object.fromEntries(
    Object.entries(value).flatMap(([field, message]) =>
      typeof message === "string" ? [[field, message]] : [],
    ),
  );
}

export function apiErrorFromResponse(status: number, data: unknown): ApiError {
  const problem = data && typeof data === "object" ? data as Record<string, unknown> : {};
  const detail = typeof problem.detail === "string" ? problem.detail : undefined;
  const title = typeof problem.title === "string" ? problem.title : undefined;
  const fallback = status === 401
    ? "Sesja wygasła lub dane logowania są nieprawidłowe."
    : status === 403
      ? "Nie masz uprawnień do wykonania tej czynności."
      : status === 400
        ? "Sprawdź poprawność wprowadzonych danych."
        : "Wystąpił błąd podczas komunikacji z serwerem.";

  return new ApiError(detail ?? title ?? fallback, status, readFieldErrors(problem.errors));
}

export function toApiError(error: unknown): ApiError {
  if (axios.isAxiosError(error)) {
    if (error.response) return apiErrorFromResponse(error.response.status, error.response.data);
    return new ApiError("Nie udało się połączyć z serwerem. Sprawdź połączenie i spróbuj ponownie.");
  }
  if (error instanceof ApiError) return error;
  return new ApiError("Wystąpił nieoczekiwany błąd. Spróbuj ponownie.");
}
