import { describe, expect, it } from "vitest";
import { expenseFiltersSchema, expenseSchema } from "./expenseSchema";

const validExpense = {
  expenseDate: "2025-06-15",
  category: "REPAIR",
  amount: 120.5,
  currency: "pln",
  description: null,
  odometer: null,
};

describe("expenseSchema", () => {
  const schema = expenseSchema("2020-01-01");

  it("accepts valid expense data and normalizes the currency", () => {
    expect(schema.parse(validExpense)).toMatchObject({
      ...validExpense,
      currency: "PLN",
    });
  });

  it("rejects dates before purchase and future dates", () => {
    expect(
      schema.safeParse({ ...validExpense, expenseDate: "2019-12-31" }).success,
    ).toBe(false);
    expect(
      schema.safeParse({ ...validExpense, expenseDate: "2999-01-01" }).success,
    ).toBe(false);
  });

  it("enforces positive two-decimal amounts and a three-letter currency code", () => {
    expect(schema.safeParse({ ...validExpense, amount: 0 }).success).toBe(
      false,
    );
    expect(schema.safeParse({ ...validExpense, amount: 1.234 }).success).toBe(
      false,
    );
    expect(schema.safeParse({ ...validExpense, currency: "PL" }).success).toBe(
      false,
    );
  });

  it("validates optional odometer and description limits", () => {
    expect(schema.safeParse({ ...validExpense, odometer: -1 }).success).toBe(
      false,
    );
    expect(
      schema.safeParse({ ...validExpense, description: "x".repeat(1001) })
        .success,
    ).toBe(false);
  });
});

describe("expenseFiltersSchema", () => {
  it("accepts empty or valid filters", () => {
    expect(
      expenseFiltersSchema.safeParse({ category: "", dateFrom: "", dateTo: "" })
        .success,
    ).toBe(true);
    expect(
      expenseFiltersSchema.safeParse({
        category: "TIRES",
        dateFrom: "2025-01-01",
        dateTo: "2025-12-31",
      }).success,
    ).toBe(true);
  });

  it("rejects invalid categories and reversed date ranges", () => {
    expect(
      expenseFiltersSchema.safeParse({
        category: "UNKNOWN",
        dateFrom: "",
        dateTo: "",
      }).success,
    ).toBe(false);
    expect(
      expenseFiltersSchema.safeParse({
        category: "",
        dateFrom: "2025-02-01",
        dateTo: "2025-01-01",
      }).success,
    ).toBe(false);
  });
});
