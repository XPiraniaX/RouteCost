import { z } from "zod";
import { fuels } from "./api";
import { localToday } from "../../utils/date";
export { localToday } from "../../utils/date";
export const vehicleSchema = (minimumOdometer = 0) =>
  z.object({
    make: z
      .string()
      .trim()
      .min(1, "Podaj markę.")
      .max(100, "Maksymalnie 100 znaków."),
    model: z
      .string()
      .trim()
      .min(1, "Podaj model.")
      .max(100, "Maksymalnie 100 znaków."),
    productionYear: z
      .number({ error: "Podaj rok produkcji." })
      .int("Podaj pełny rok.")
      .min(1886, "Najwcześniejszy rok to 1886.")
      .max(2100, "Najpóźniejszy rok to 2100."),
    fuelType: z.enum(
      Object.keys(fuels) as [keyof typeof fuels, ...(keyof typeof fuels)[]],
    ),
    currentOdometer: z
      .number({ error: "Podaj przebieg." })
      .int("Podaj pełne kilometry.")
      .min(
        minimumOdometer,
        minimumOdometer
          ? `Przebieg nie może być mniejszy niż ${minimumOdometer} km.`
          : "Przebieg nie może być ujemny.",
      )
      .max(2147483647, "Przebieg jest zbyt duży."),
    purchaseDate: z.iso
      .date("Podaj poprawną datę.")
      .refine(
        (value) => value <= localToday(),
        "Data zakupu nie może być w przyszłości.",
      ),
    purchasePrice: z
      .number({ error: "Podaj cenę zakupu." })
      .min(0, "Cena nie może być ujemna.")
      .max(9999999999.99, "Maksymalnie 10 cyfr przed przecinkiem.")
      .refine(
        (value) => Math.abs(value * 100 - Math.round(value * 100)) < 0.0002,
        "Podaj maksymalnie 2 miejsca po przecinku.",
      ),
  });
