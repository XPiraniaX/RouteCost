import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { ArrowRight, LoaderCircle, Mail } from "lucide-react";
import { useState } from "react";
import { useForm, type Path } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import { login } from "../../api/auth";
import { toApiError } from "../../api/errors";
import { AuthFeedback } from "../../components/AuthFeedback";
import { FormField } from "../../components/FormField";
import { PasswordField } from "../../components/PasswordField";
import { useAuth } from "./useAuth";
import { loginSchema, type LoginFormValues } from "./authSchemas";

export function LoginPage() {
  const navigate = useNavigate();
  const { establishSession } = useAuth();
  const [serverError, setServerError] = useState("");
  const mutation = useMutation({ mutationFn: login });
  const {
    register: registerField,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({ resolver: zodResolver(loginSchema), defaultValues: { email: "", password: "" } });

  const submit = handleSubmit(async (values) => {
    setServerError("");
    try {
      const response = await mutation.mutateAsync(values);
      await establishSession(response);
      navigate("/", { replace: true });
    } catch (error) {
      const apiError = toApiError(error);
      for (const [field, message] of Object.entries(apiError.fieldErrors)) {
        setError(field as Path<LoginFormValues>, { type: "server", message });
      }
      setServerError(apiError.message);
    }
  });

  const busy = isSubmitting || mutation.isPending;

  return (
    <form className="auth-form" onSubmit={submit} noValidate>
      <FormField
        label="Adres e-mail"
        type="email"
        autoComplete="email"
        placeholder="ty@przyklad.pl"
        leadingIcon={Mail}
        error={errors.email?.message}
        {...registerField("email")}
      />
      <PasswordField label="Hasło" autoComplete="current-password" registration={registerField("password")} error={errors.password?.message} />

      {serverError && <AuthFeedback message={serverError} />}

      <button className="primary-button mt-1" type="submit" disabled={busy}>
        {busy ? <LoaderCircle className="animate-spin" size={18} /> : <span>Zaloguj się</span>}
        {!busy && <ArrowRight size={17} />}
      </button>
      <p className="form-security"><span className="security-dot" /> Token działa tylko w pamięci aktywnej karty.</p>
    </form>
  );
}
