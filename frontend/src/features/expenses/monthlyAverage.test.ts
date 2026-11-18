import { describe, expect, it } from "vitest";
import { averageMonthlyExpense } from "./monthlyAverage";

describe("averageMonthlyExpense", () => {
  it("includes months without expenses through the current month", () => {
    expect(
      averageMonthlyExpense(
        60,
        [{ year: 2024, month: 11, totalAmount: 60, count: 1 }],
        new Date(2025, 1, 15),
      ),
    ).toBe(15);
  });

  it("returns zero when the vehicle has no expenses", () => {
    expect(averageMonthlyExpense(0, [], new Date(2025, 1, 15))).toBe(0);
  });
});
