import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowLeft, Check, LoaderCircle } from "lucide-react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ApiError } from "../../api/errors";
import { ErrorState, Loading, PageHeader } from "../../components/ui";
import { dateLabel, localToday } from "../../utils/date";
import { validId } from "../../utils/ids";
import { useVehicle, type Vehicle } from "../vehicles/api";
import { useReminder, useSaveReminder } from "./api";
import { reminderSchema } from "./reminderSchema";
import { reminderTypes, type Reminder, type ReminderRequest } from "./types";

export function ReminderFormPage() {
  const { vehicleId, reminderId } = useParams();
  const id = Number(vehicleId);
  const reminderNumber = Number(reminderId);
  const vehicle = useVehicle(id);
  const reminder = useReminder(id, reminderNumber);

  if (
    !validId(id) ||
    (reminderId !== undefined && !validId(reminderNumber))
  ) {
    return (
      <ErrorState
        error={new Error("Nieprawidłowy identyfikator pojazdu lub przypomnienia.")}
      />
    );
  }
  if (vehicle.isPending || (reminderId !== undefined && reminder.isPending)) {
    return <Loading />;
  }
  if (vehicle.isError) {
    return <ErrorState error={vehicle.error} retry={() => void vehicle.refetch()} />;
  }
  if (reminderId !== undefined && reminder.isError) {
    return (
      <ErrorState error={reminder.error} retry={() => void reminder.refetch()} />
    );
  }

  return (
    <ReminderForm
      key={`${id}-${reminderId ?? "new"}`}
      vehicle={vehicle.data}
      reminder={reminderId ? reminder.data : undefined}
    />
  );
}

export function ReminderForm({
  vehicle,
  reminder,
}: {
  vehicle: Vehicle;
  reminder?: Reminder;
}) {
  const navigate = useNavigate();
  const mutation = useSaveReminder(vehicle.id, reminder?.id);
  const schema = reminderSchema(vehicle.purchaseDate);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<ReminderRequest>({
    resolver: zodResolver(schema),
    defaultValues: reminder
      ? {
          type: reminder.type,
          title: reminder.title,
          description: reminder.description ?? "",
          dueDate: reminder.dueDate,
          odometer: reminder.odometer,
        }
      : {
          type: "OTHER",
          title: "",
          description: "",
          dueDate: localToday(),
          odometer: null,
        },
  });
  const back = `/reminders?vehicleId=${vehicle.id}`;
  const error = (key: keyof ReminderRequest) =>
    errors[key] && (
      <span id={`${key}-error`} role="alert" className="field-error">
        {errors[key]?.message}
      </span>
    );
  const attrs = (key: keyof ReminderRequest) => ({
    "aria-invalid": !!errors[key],
    "aria-describedby": errors[key] ? `${key}-error` : undefined,
  });

  const submit = (data: ReminderRequest) => {
    if (mutation.isPending) return;
    mutation.mutate(
      { ...data, description: data.description?.trim() || null },
      {
        onSuccess: () =>
          navigate(back, {
            replace: true,
            state: {
              notice: reminder
                ? "Przypomnienie zostało zaktualizowane."
                : "Przypomnienie zostało dodane.",
            },
          }),
        onError: (failure) => {
          if (failure instanceof ApiError) {
            Object.entries(failure.fieldErrors).forEach(([field, message]) => {
              if (Object.hasOwn(schema.shape, field)) {
                setError(field as keyof ReminderRequest, { message });
              }
            });
          }
        },
      },
    );
  };

  return (
    <div className="vehicle-form-page">
      <Link className="text-link back-link" to={back}>
        <ArrowLeft size={16} />
        Wróć do przypomnień
      </Link>
      <PageHeader
        eyebrow={`${vehicle.make} ${vehicle.model}`}
        title={reminder ? "Edytuj przypomnienie" : "Dodaj przypomnienie"}
        description="Ustaw termin serwisu, przeglądu lub innego ważnego zdarzenia."
      />
      <section className="panel form-panel">
        <div className="section-heading">
          <div>
            <h2>Dane przypomnienia</h2>
            <p>
              Termin nie może poprzedzać zakupu pojazdu. Przebieg jest opcjonalny.
            </p>
          </div>
          <span className="badge">{reminder ? "Edycja" : "Nowy wpis"}</span>
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
                <span>Tytuł</span>
                <input
                  autoFocus
                  type="text"
                  maxLength={150}
                  placeholder="np. Przegląd techniczny"
                  {...register("title")}
                  {...attrs("title")}
                />
                {error("title")}
              </label>
              <label className="form-field">
                <span>Typ przypomnienia</span>
                <select {...register("type")} {...attrs("type")}>
                  {Object.entries(reminderTypes).map(([value, label]) => (
                    <option value={value} key={value}>
                      {label}
                    </option>
                  ))}
                </select>
                {error("type")}
              </label>
              <label className="form-field">
                <span>Termin</span>
                <input
                  type="date"
                  min={vehicle.purchaseDate}
                  {...register("dueDate")}
                  {...attrs("dueDate")}
                />
                {error("dueDate")}
              </label>
              <label className="form-field">
                <span>Przebieg (opcjonalnie, km)</span>
                <input
                  type="number"
                  min={0}
                  max={2147483647}
                  step={1}
                  placeholder="np. 80000"
                  {...register("odometer", {
                    setValueAs: (value) =>
                      value === "" || value == null ? null : Number(value),
                  })}
                  {...attrs("odometer")}
                />
                {error("odometer")}
              </label>
              <label className="form-field reminder-description">
                <span>Opis (opcjonalnie)</span>
                <textarea
                  rows={4}
                  maxLength={1000}
                  placeholder="Dodatkowe informacje"
                  {...register("description")}
                  {...attrs("description")}
                />
                {error("description")}
              </label>
            </div>
            <div className="fuel-form-note">
              <strong>Termin przypomnienia</strong>
              <p>
                Możesz ustawić także termin z przeszłości, aby zapisać zaległą
                czynność. Data musi przypadać w dniu zakupu pojazdu (
                {dateLabel(vehicle.purchaseDate)}) lub później.
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
                  : reminder
                    ? "Zapisz zmiany"
                    : "Dodaj przypomnienie"}
              </button>
            </div>
          </fieldset>
        </form>
      </section>
    </div>
  );
}
