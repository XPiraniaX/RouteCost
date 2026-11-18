import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowLeft, Check, LoaderCircle } from "lucide-react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ApiError } from "../../api/errors";
import { ErrorState, Loading, PageHeader } from "../../components/ui";
import { localToday, dateLabel } from "../../utils/date";
import { useVehicle, type Vehicle } from "../vehicles/api";
import { validId } from "../../utils/ids";
import { expenseApi, useExpense, useSaveExpense } from "./api";
import { expenseSchema } from "./expenseSchema";
import { expenseCategories, type ExpenseRequest } from "./types";

export function ExpenseFormPage() {
  const { vehicleId, expenseId } = useParams();
  const id = Number(vehicleId);
  const expenseNumber = Number(expenseId);
  const vehicle = useVehicle(id);
  const expense = useExpense(id, expenseNumber);
  if (!validId(id) || (expenseId !== undefined && !validId(expenseNumber)))
    return (
      <ErrorState
        error={new Error("Nieprawidłowy identyfikator pojazdu lub wydatku.")}
      />
    );
  if (vehicle.isPending || (expenseId && expense.isPending)) return <Loading />;
  if (vehicle.isError)
    return (
      <ErrorState error={vehicle.error} retry={() => void vehicle.refetch()} />
    );
  if (expenseId && expense.isError)
    return (
      <ErrorState error={expense.error} retry={() => void expense.refetch()} />
    );
  return (
    <ExpenseForm
      key={`${id}-${expenseId ?? "new"}`}
      vehicle={vehicle.data}
      expense={expenseId ? expense.data : undefined}
    />
  );
}

function ExpenseForm({
  vehicle,
  expense,
}: {
  vehicle: Vehicle;
  expense?: Awaited<ReturnType<typeof expenseApi.get>>;
}) {
  const navigate = useNavigate();
  const mutation = useSaveExpense(vehicle.id, expense?.id);
  const schema = expenseSchema(vehicle.purchaseDate);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<ExpenseRequest>({
    resolver: zodResolver(schema),
    defaultValues: expense
      ? {
          expenseDate: expense.expenseDate,
          category: expense.category,
          amount: expense.amount,
          description: expense.description,
          odometer: expense.odometer,
        }
      : {
          expenseDate: localToday(),
          category: "OTHER",
          description: "",
          odometer: null,
        },
  });
  const back = `/expenses?vehicleId=${vehicle.id}`;
  const error = (key: keyof ExpenseRequest) =>
    errors[key] && (
      <span id={`${key}-error`} role="alert" className="field-error">
        {errors[key]?.message}
      </span>
    );
  const attrs = (key: keyof ExpenseRequest) => ({
    "aria-invalid": !!errors[key],
    "aria-describedby": errors[key] ? `${key}-error` : undefined,
  });
  const submit = (data: ExpenseRequest) => {
    if (mutation.isPending) return;
    mutation.mutate(
      { ...data, description: data.description?.trim() || null },
      {
        onSuccess: () =>
          navigate(back, {
            replace: true,
            state: {
              notice: expense
                ? "Wydatek został zaktualizowany."
                : "Wydatek został dodany.",
            },
          }),
        onError: (failure) => {
          if (failure instanceof ApiError)
            Object.entries(failure.fieldErrors).forEach(([key, message]) => {
              if (Object.hasOwn(schema.shape, key))
                setError(key as keyof ExpenseRequest, { message });
            });
        },
      },
    );
  };
  return (
    <div className="vehicle-form-page">
      <Link className="text-link back-link" to={back}>
        <ArrowLeft size={16} />
        Wróć do wydatków
      </Link>
      <PageHeader
        eyebrow={`${vehicle.make} ${vehicle.model}`}
        title={expense ? "Edytuj wydatek" : "Dodaj wydatek"}
        description="Zapisz koszt utrzymania pojazdu. Kwoty są zapisywane w PLN."
      />
      <section className="panel form-panel">
        <div className="section-heading">
          <div>
            <h2>Dane wydatku</h2>
            <p>Wszystkie kwoty są zapisywane i prezentowane w PLN.</p>
          </div>
          <span className="badge">{expense ? "Edycja" : "Nowy wpis"}</span>
        </div>
        <form
          className="vehicle-form"
          noValidate
          onSubmit={handleSubmit(submit)}
        >
          {mutation.isError && <ErrorState error={mutation.error} />}
          <fieldset className="form-fieldset" disabled={mutation.isPending}>
            <div className="form-grid">
              <label className="form-field">
                <span>Data wydatku</span>
                <input
                  autoFocus
                  type="date"
                  min={vehicle.purchaseDate}
                  max={localToday()}
                  {...register("expenseDate")}
                  {...attrs("expenseDate")}
                />
                {error("expenseDate")}
              </label>
              <label className="form-field">
                <span>Kategoria</span>
                <select {...register("category")} {...attrs("category")}>
                  {Object.entries(expenseCategories).map(([value, label]) => (
                    <option value={value} key={value}>
                      {label}
                    </option>
                  ))}
                </select>
                {error("category")}
              </label>
              <label className="form-field">
                <span>Kwota (PLN)</span>
                <input
                  type="number"
                  min="0.01"
                  max="9999999999.99"
                  step="0.01"
                  placeholder="np. 250,00"
                  {...register("amount", { valueAsNumber: true })}
                  {...attrs("amount")}
                />
                {error("amount")}
              </label>
              <label className="form-field">
                <span>Przebieg (opcjonalnie, km)</span>
                <input
                  type="number"
                  min={0}
                  max={2147483647}
                  step={1}
                  placeholder="np. 50000"
                  {...register("odometer", {
                    setValueAs: (value) =>
                      value === "" ? null : Number(value),
                  })}
                  {...attrs("odometer")}
                />
                {error("odometer")}
              </label>
              <label className="form-field expense-description">
                <span>Opis (opcjonalnie)</span>
                <textarea
                  rows={3}
                  maxLength={1000}
                  placeholder="Szczegóły wydatku"
                  {...register("description", {
                    setValueAs: (value) => (value === "" ? null : value),
                  })}
                  {...attrs("description")}
                />
                {error("description")}
              </label>
            </div>
            <div className="fuel-form-note">
              <strong>Przebieg jest sprawdzany z historią pojazdu</strong>
              <p>
                Data wydatku nie może poprzedzać zakupu pojazdu (
                {dateLabel(vehicle.purchaseDate)}). Jeśli podasz przebieg,
                backend sprawdzi jego chronologię względem tankowań i innych
                wydatków.
              </p>
            </div>
            <div className="form-actions">
              <Link className="button button-secondary" to={back}>
                Anuluj
              </Link>
              <button
                className="button button-primary"
                type="submit"
                disabled={mutation.isPending}
              >
                {mutation.isPending ? (
                  <LoaderCircle size={16} />
                ) : (
                  <Check size={16} />
                )}
                {mutation.isPending
                  ? "Zapisywanie…"
                  : expense
                    ? "Zapisz zmiany"
                    : "Dodaj wydatek"}
              </button>
            </div>
          </fieldset>
        </form>
      </section>
    </div>
  );
}
