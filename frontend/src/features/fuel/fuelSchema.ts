import { z } from "zod";
import { localToday } from "../../utils/date";

const decimal = (max: number, scale: number) =>
  z
    .number({ error: "Podaj poprawną liczbę." })
    .positive("Wartość musi być większa od zera.")
    .max(max, "Wartość przekracza dopuszczalny zakres.")
    .refine(
      (value) => Number(value.toFixed(scale)) === value,
      `Dopuszczalna liczba miejsc po przecinku: ${scale}.`,
    );
export const fuelSchema = (purchaseDate: string) =>
  z.object({
    entryDate: z.iso
      .date("Podaj poprawną datę.")
      .refine(
        (value) => value <= localToday(),
        "Data tankowania nie może być w przyszłości.",
      )
      .refine(
        (value) => value >= purchaseDate,
        "Data tankowania nie może być wcześniejsza niż data zakupu pojazdu.",
      ),
    odometer: z
      .number({ error: "Podaj przebieg." })
      .int("Podaj pełne kilometry.")
      .min(0, "Przebieg nie może być ujemny.")
      .max(2147483647, "Przebieg jest zbyt duży."),
    liters: decimal(9999999.999, 3),
    totalCost: decimal(9999999999.99, 2),
    fullTank: z.boolean(),
  });
