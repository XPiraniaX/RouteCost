import { describe, expect, it } from "vitest";
import { reminderFiltersSchema, reminderSchema } from "./reminderSchema";

const valid = {
  type: "SERVICE",
  title: "Regular service",
  description: null,
  dueDate: "2026-04-02",
  odometer: 120000,
};

describe("reminder request validation", () => {
  const schema = reminderSchema("2020-01-01");

  it("accepts past-due reminders and optional odometer", () => {
    expect(schema.safeParse({ ...valid, dueDate: "2021-01-01", odometer: null }).success).toBe(true);
  });

  it("requires a nonblank bounded title and a date after purchase", () => {
    expect(schema.safeParse({ ...valid, title: "   " }).success).toBe(false);
    expect(schema.safeParse({ ...valid, title: "x".repeat(151) }).success).toBe(false);
    expect(schema.safeParse({ ...valid, dueDate: "2019-12-31" }).success).toBe(false);
  });

  it("validates description, integer odometer, and supported type", () => {
    expect(schema.safeParse({ ...valid, description: "x".repeat(1001) }).success).toBe(false);
    expect(schema.safeParse({ ...valid, odometer: -1 }).success).toBe(false);
    expect(schema.safeParse({ ...valid, odometer: 1.5 }).success).toBe(false);
    expect(schema.safeParse({ ...valid, type: "UNKNOWN" }).success).toBe(false);
  });
});

describe("reminder filters", () => {
  it("accepts supported filters and rejects invalid status or reversed dates", () => {
    expect(reminderFiltersSchema.safeParse({ status: "ACTIVE", dueFrom: "2026-01-01", dueTo: "2026-12-31" }).success).toBe(true);
    expect(reminderFiltersSchema.safeParse({ status: "DONE", dueFrom: "", dueTo: "" }).success).toBe(false);
    expect(reminderFiltersSchema.safeParse({ status: "", dueFrom: "2026-02-01", dueTo: "2026-01-01" }).success).toBe(false);
  });
});
