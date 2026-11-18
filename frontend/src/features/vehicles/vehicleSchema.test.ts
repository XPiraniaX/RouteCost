import { describe, expect, it } from "vitest";
import { vehicleSchema } from "./vehicleSchema";
const valid = {
  make: "Toyota",
  model: "Corolla",
  productionYear: 2020,
  fuelType: "HYBRID",
  currentOdometer: 50000,
  purchaseDate: "2020-06-01",
  purchasePrice: 12345.67,
};
describe("vehicle backend constraints", () => {
  it("defaults an empty purchase price to zero", () => {
    expect(vehicleSchema().parse({ ...valid, purchasePrice: Number.NaN }).purchasePrice).toBe(0);
  });
  it("accepts all supported fuels and trims names", () => {
    for (const fuelType of [
      "PETROL",
      "DIESEL",
      "LPG",
      "HYBRID",
      "ELECTRIC",
      "OTHER",
    ])
      expect(
        vehicleSchema().parse({ ...valid, fuelType, make: " Toyota " }).make,
      ).toBe("Toyota");
  });
  it("rejects odometer rollback and values outside Java integer", () => {
    expect(vehicleSchema(50001).safeParse(valid).success).toBe(false);
    expect(
      vehicleSchema().safeParse({ ...valid, currentOdometer: 2147483648 })
        .success,
    ).toBe(false);
  });
  it.each([
    { make: " " },
    { model: "a".repeat(101) },
    { productionYear: 1885 },
    { productionYear: 2101 },
    { productionYear: 2020.5 },
    { currentOdometer: -1 },
    { purchaseDate: "2999-01-01" },
    { purchaseDate: "2020-02-31" },
    { purchasePrice: -1 },
    { purchasePrice: 12.345 },
    { purchasePrice: 10000000000 },
    { fuelType: "CNG" },
  ])("rejects invalid input %j", (change) => {
    expect(vehicleSchema().safeParse({ ...valid, ...change }).success).toBe(
      false,
    );
  });
});
