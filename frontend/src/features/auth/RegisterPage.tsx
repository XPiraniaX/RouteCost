import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { ArrowRight, LoaderCircle, Mail, UserRound } from "lucide-react";
import { useState } from "react";
import { useForm, type Path } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import { register as registerAccount } from "../../api/auth";
import { toApiError } from "../../api/errors";
import { AuthFeedback } from "../../components/AuthFeedback";
import { FormField } from "../../components/FormField";
import { PasswordField } from "../../components/PasswordField";
import { useAuth } from "./useAuth";
import { registerSchema, type RegisterFormValues } from "./authSchemas";

export function RegisterPage() {
  const navigate = useNavigate();
  const { establishSession } = useAuth();
  const [serverError, setServerError] = useState("");
  const mutation = useMutation({ mutationFn: registerAccount });
  const {
    register: registerField,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: { firstName: "", lastName: "", email: "", password: "", confirmPassword: "" },
  });

  const submit = handleSubmit(async (values) => {
    setServerError("");
    try {
      const response = await mutation.mutateAsync({
        firstName: values.firstName,
        lastName: values.lastName,
        email: values.email,
        password: values.password,
      });
      await establishSession(response);
      navigate("/", { replace: true });
    } catch (error) {
      const apiError = toApiError(error);
      for (const [field, message] of Object.entries(apiError.fieldErrors)) {
        setError(field as Path<RegisterFormValues>, { type: "server", message });
      }
      setServerError(apiError.message);
    }
  });

  const busy = isSubmitting || mutation.isPending;

  return (
    <form className="auth-form auth-form-register" onSubmit={submit} noValidate>
      <div className="grid grid-cols-2 gap-3">
        <FormField label="Imię" autoComplete="given-name" placeholder="Anna" leadingIcon={UserRound} error={errors.firstName?.message} {...registerField("firstName")} />
        <FormField label="Nazwisko" autoComplete="family-name" placeholder="Kowalska" error={errors.lastName?.message} {...registerField("lastName")} />
      </div>
      <FormField label="Adres e-mail" type="email" autoComplete="email" placeholder="ty@przyklad.pl" leadingIcon={Mail} error={errors.email?.message} {...registerField("email")} />
      <PasswordField label="Hasło" autoComplete="new-password" registration={registerField("password")} error={errors.password?.message} />
      <PasswordField label="Powtórz hasło" autoComplete="new-password" registration={registerField("confirmPassword")} error={errors.confirmPassword?.message} />

      {serverError && <AuthFeedback message={serverError} />}

      <button className="primary-button mt-1" type="submit" disabled={busy}>
        {busy ? <LoaderCircle className="animate-spin" size={18} /> : <span>Utwórz konto</span>}
        {!busy && <ArrowRight size={17} />}
      </button>
      <p className="form-security"><span className="security-dot" /> Hasło musi zawierać co najmniej 8 znaków.</p>
    </form>
  );
}
