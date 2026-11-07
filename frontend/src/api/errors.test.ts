import { describe, expect, it } from "vitest";
import { apiErrorFromResponse } from "./errors";

describe("API problem responses", () => {
  it("preserves validation details and field errors from HTTP 400", () => {
    const error = apiErrorFromResponse(400, {
      title: "Validation failed",
      detail: "One or more request fields are invalid.",
      errors: { email: "must be a well-formed email address" },
    });

    expect(error.status).toBe(400);
    expect(error.message).toBe("One or more request fields are invalid.");
    expect(error.fieldErrors.email).toBe("must be a well-formed email address");
  });

  it("shows the backend message for HTTP 401", () => {
    const error = apiErrorFromResponse(401, { detail: "Invalid email or password." });
    expect(error.status).toBe(401);
    expect(error.message).toBe("Invalid email or password.");
  });

  it("provides a useful fallback for HTTP 403", () => {
    const error = apiErrorFromResponse(403, {});
    expect(error.status).toBe(403);
    expect(error.message).toBe("Nie masz uprawnień do wykonania tej czynności.");
  });
});
