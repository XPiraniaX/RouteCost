import { z } from "zod";

const emailSchema = z.string()
  .trim()
  .min(1, "Podaj adres e-mail.")
  .email("Wpisz poprawny adres e-mail.")
  .max(255, "Adres e-mail może mieć maksymalnie 255 znaków.");

const passwordSchema = z.string()
  .min(1, "Podaj hasło.")
  .max(72, "Hasło może mieć maksymalnie 72 znaki.")
  .refine((value) => new TextEncoder().encode(value).length <= 72, {
    message: "Hasło może mieć maksymalnie 72 bajty w kodowaniu UTF-8.",
  });

const registrationPasswordSchema = z.string()
  .min(8, "Hasło musi mieć co najmniej 8 znaków.")
  .max(72, "Hasło może mieć maksymalnie 72 znaki.")
  .refine((value) => new TextEncoder().encode(value).length <= 72, {
    message: "Hasło może mieć maksymalnie 72 bajty w kodowaniu UTF-8.",
  });

export const loginSchema = z.object({
  email: emailSchema,
  password: passwordSchema,
});

export const registerSchema = z.object({
  firstName: z.string().trim().min(1, "Podaj imię.").max(100, "Imię może mieć maksymalnie 100 znaków."),
  lastName: z.string().trim().min(1, "Podaj nazwisko.").max(100, "Nazwisko może mieć maksymalnie 100 znaków."),
  email: emailSchema,
  password: registrationPasswordSchema,
  confirmPassword: z.string().min(1, "Powtórz hasło."),
}).refine((values) => values.password === values.confirmPassword, {
  message: "Hasła muszą być takie same.",
  path: ["confirmPassword"],
});

export type LoginFormValues = z.infer<typeof loginSchema>;
export type RegisterFormValues = z.infer<typeof registerSchema>;
