import { z } from "zod";
import { reminderStatuses, reminderTypes } from "./types";

export const reminderTypeSchema = z.enum(
  Object.keys(reminderTypes) as [
    keyof typeof reminderTypes,
    ...(keyof typeof reminderTypes)[],
  ],
);

export const reminderStatusSchema = z.enum([
  "ACTIVE",
  "COMPLETED",
  "CANCELLED",
]);

export const reminderSchema = (purchaseDate: string) =>
  z.object({
    type: reminderTypeSchema,
    title: z
      .string({ error: "Podaj tytuł przypomnienia." })
      .trim()
      .min(1, "Podaj tytuł przypomnienia.")
      .max(150, "Tytuł może mieć maksymalnie 150 znaków."),
    description: z
      .string()
      .max(1000, "Opis może mieć maksymalnie 1000 znaków.")
      .nullable(),
    dueDate: z
      .iso.date("Podaj poprawny termin.")
      .refine(
        (value) => value >= purchaseDate,
        "Termin nie może poprzedzać zakupu pojazdu.",
      ),
    odometer: z
      .number({ error: "Podaj poprawny przebieg lub pozostaw pole puste." })
      .int("Podaj pełne kilometry.")
      .min(0, "Przebieg nie może być ujemny.")
      .max(2147483647, "Przebieg jest zbyt duży.")
      .nullable(),
  });

export const reminderFiltersSchema = z
  .object({
    status: reminderStatusSchema.or(z.literal("")),
    dueFrom: z.iso.date("Podaj poprawną datę początkową.").or(z.literal("")),
    dueTo: z.iso.date("Podaj poprawną datę końcową.").or(z.literal("")),
  })
  .refine(
    (value) => !value.dueFrom || !value.dueTo || value.dueFrom <= value.dueTo,
    {
      path: ["dueTo"],
      message: "Data końcowa nie może być wcześniejsza niż początkowa.",
    },
  );

export const reminderStatusLabel = (status: string) =>
  Object.hasOwn(reminderStatuses, status)
    ? reminderStatuses[status as keyof typeof reminderStatuses]
    : status;
