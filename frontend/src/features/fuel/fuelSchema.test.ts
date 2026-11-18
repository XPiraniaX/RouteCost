import { describe, expect, it } from "vitest";
import { fuelSchema } from "./fuelSchema";

const valid = {
  entryDate: "2026-01-02",
  odometer: 1000,
  liters: 42.123,
  totalCost: 250.12,
  fullTank: true,
};
describe("fuel DTO validation", () => {
  it("accepts DTO fields and strips response-only fields", () => {
    expect(
      fuelSchema("2020-01-01").parse({
        ...valid,
        pricePerLiter: 6,
      }),
    ).toEqual(valid);
  });
  it("accepts historical odometer below vehicle current odometer", () => {
    expect(fuelSchema("2020-01-01").safeParse(valid).success).toBe(true);
  });
  it.each([
    { entryDate: "2999-01-01" },
    { entryDate: "2019-12-31" },
    { entryDate: "2026-02-30" },
    { odometer: -1 },
    { odometer: 10.1 },
    { odometer: 2147483648 },
    { liters: 0 },
    { liters: 1.2345 },
    { liters: 0.0000001 },
    { liters: 10000000 },
    { totalCost: 0 },
    { totalCost: -1 },
    { totalCost: 1.001 },
    { totalCost: 10000000000 },
    { fullTank: undefined },
  ])("rejects %j", (change) => {
    expect(
      fuelSchema("2020-01-01").safeParse({ ...valid, ...change }).success,
    ).toBe(false);
  });
  it("accepts maximum precision and smallest positive amounts", () => {
    expect(
      fuelSchema("2020-01-01").safeParse({
        ...valid,
        liters: 9999999.999,
        totalCost: 9999999999.99,
      }).success,
    ).toBe(true);
    expect(
      fuelSchema("2020-01-01").safeParse({
        ...valid,
        liters: 0.001,
        totalCost: 0.01,
        fullTank: false,
      }).success,
    ).toBe(true);
  });
});
