import { z } from "zod";
import { localToday } from "../../utils/date";
import { expenseCategories } from "./types";

export const categorySchema = z.enum(
  Object.keys(expenseCategories) as [
    keyof typeof expenseCategories,
    ...(keyof typeof expenseCategories)[],
  ],
);
export const expenseSchema = (purchaseDate: string) =>
  z.object({
    expenseDate: z.iso
      .date("Podaj poprawną datę.")
      .refine(
        (value) => value >= purchaseDate,
        "Data nie może poprzedzać zakupu pojazdu.",
      )
      .refine(
        (value) => value <= localToday(),
        "Data nie może być w przyszłości.",
      ),
    category: categorySchema,
    amount: z
      .number({ error: "Podaj kwotę." })
      .positive("Kwota musi być większa od zera.")
      .max(9999999999.99, "Kwota jest zbyt duża.")
      .refine(
        (value) => Number(value.toFixed(2)) === value,
        "Podaj maksymalnie 2 miejsca po przecinku.",
      ),
    description: z
      .string()
      .max(1000, "Opis może mieć maksymalnie 1000 znaków.")
      .nullable(),
    odometer: z
      .number({ error: "Podaj poprawny przebieg lub pozostaw pole puste." })
      .int("Podaj pełne kilometry.")
      .min(0, "Przebieg nie może być ujemny.")
      .max(2147483647, "Przebieg jest zbyt duży.")
      .nullable(),
  });
export const expenseFiltersSchema = z
  .object({
    category: categorySchema.or(z.literal("")),
    dateFrom: z.iso.date("Podaj poprawną datę początkową.").or(z.literal("")),
    dateTo: z.iso.date("Podaj poprawną datę końcową.").or(z.literal("")),
  })
  .refine(
    (value) =>
      !value.dateFrom || !value.dateTo || value.dateFrom <= value.dateTo,
    {
      path: ["dateTo"],
      message: "Data końcowa nie może być wcześniejsza niż początkowa.",
    },
  );
