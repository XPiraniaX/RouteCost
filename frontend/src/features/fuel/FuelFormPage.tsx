import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ArrowLeft, Check, LoaderCircle } from "lucide-react";
import { ApiError } from "../../api/errors";
import { ErrorState, Loading, PageHeader } from "../../components/ui";
import { localToday, dateLabel } from "../../utils/date";
import { useVehicle, type Vehicle } from "../vehicles/api";
import { fuelSchema } from "./fuelSchema";
import {
  useFuelEntry,
  useSaveFuel,
  validId,
  type FuelEntry,
  type FuelRequest,
} from "./api";

export function FuelFormPage() {
  const { vehicleId, entryId } = useParams();
  const id = Number(vehicleId);
  const fuelId = Number(entryId);
  const vehicle = useVehicle(id);
  const entry = useFuelEntry(id, fuelId);
  if (!validId(id) || (entryId !== undefined && !validId(fuelId)))
    return (
      <ErrorState
        error={new Error("Nieprawidłowy identyfikator pojazdu lub tankowania.")}
      />
    );
  if (vehicle.isPending || (entryId && entry.isPending)) return <Loading />;
  if (vehicle.isError)
    return (
      <ErrorState error={vehicle.error} retry={() => void vehicle.refetch()} />
    );
  if (entryId && entry.isError)
    return (
      <ErrorState error={entry.error} retry={() => void entry.refetch()} />
    );
  return (
    <FuelForm
      key={`${id}-${entryId ?? "new"}`}
      vehicle={vehicle.data}
      entry={entryId ? entry.data : undefined}
    />
  );
}
function FuelForm({ vehicle, entry }: { vehicle: Vehicle; entry?: FuelEntry }) {
  const navigate = useNavigate();
  const mutation = useSaveFuel(vehicle.id, entry?.id);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<FuelRequest>({
    resolver: zodResolver(fuelSchema(vehicle.purchaseDate)),
    defaultValues: entry
      ? {
          entryDate: entry.entryDate,
          odometer: entry.odometer,
          liters: entry.liters,
          totalCost: entry.totalCost,
          fullTank: entry.fullTank,
        }
      : { entryDate: localToday(), fullTank: false },
  });
  const back = `/fuel?vehicleId=${vehicle.id}`;
  const error = (key: keyof FuelRequest) =>
    errors[key] && (
      <span id={`${key}-error`} role="alert" className="field-error">
        {errors[key]?.message}
      </span>
    );
  const attrs = (key: keyof FuelRequest) => ({
    "aria-invalid": !!errors[key],
    "aria-describedby": errors[key] ? `${key}-error` : undefined,
  });
  const submit = (data: FuelRequest) => {
    if (mutation.isPending) return;
    mutation.mutate(data, {
      onSuccess: () =>
        navigate(back, {
          replace: true,
          state: {
            notice: entry
              ? "Tankowanie zostało zaktualizowane."
              : "Tankowanie zostało dodane.",
          },
        }),
      onError: (error) => {
        if (error instanceof ApiError)
          Object.entries(error.fieldErrors).forEach(([key, message]) => {
            if (Object.hasOwn(fuelSchema(vehicle.purchaseDate).shape, key))
              setError(key as keyof FuelRequest, { message });
          });
      },
    });
  };
  return (
    <>
      <Link className="text-link back-link" to={back}>
        <ArrowLeft size={16} />
        Wróć do historii tankowań
      </Link>
      <PageHeader
        eyebrow={`${vehicle.make} ${vehicle.model}`}
        title={entry ? "Edytuj tankowanie" : "Dodaj tankowanie"}
        description="Zapisz dane z paragonu i aktualny stan licznika."
      />
      <section className="panel form-panel">
        <div className="section-heading">
          <div>
            <h2>Dane tankowania</h2>
            <p>
              Wszystkie wartości są wymagane. Koszt jest zapisywany bez
              przypisanej waluty.
            </p>
          </div>
          <span className="badge">{entry ? "Edycja" : "Nowy wpis"}</span>
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
                <span>Data tankowania</span>
                <input
                  autoFocus
                  type="date"
                  min={vehicle.purchaseDate}
                  max={localToday()}
                  {...register("entryDate")}
                  {...attrs("entryDate")}
                />
                {error("entryDate")}
              </label>
              <label className="form-field">
                <span>Przebieg (km)</span>
                <input
                  type="number"
                  min={0}
                  max={2147483647}
                  step={1}
                  placeholder="np. 50000"
                  {...register("odometer", { valueAsNumber: true })}
                  {...attrs("odometer")}
                />
                {error("odometer")}
              </label>
              <label className="form-field">
                <span>Ilość paliwa (l)</span>
                <input
                  type="number"
                  min="0.001"
                  max="9999999.999"
                  step="0.001"
                  placeholder="np. 42,500"
                  {...register("liters", { valueAsNumber: true })}
                  {...attrs("liters")}
                />
                {error("liters")}
              </label>
              <label className="form-field">
                <span>Całkowity koszt (bez waluty)</span>
                <input
                  type="number"
                  min="0.01"
                  max="9999999999.99"
                  step="0.01"
                  placeholder="np. 250,00"
                  {...register("totalCost", { valueAsNumber: true })}
                  {...attrs("totalCost")}
                />
                {error("totalCost")}
              </label>
              <label className="fuel-checkbox">
                <input
                  type="checkbox"
                  {...register("fullTank")}
                  {...attrs("fullTank")}
                />
                <span>
                  <strong>Tankowanie do pełna</strong>
                  <small>
                    Zaznacz, jeśli zbiornik został całkowicie napełniony.
                  </small>
                </span>
              </label>
              {error("fullTank")}
            </div>
            <div className="fuel-form-note">
              <strong>Chronologia ma znaczenie</strong>
              <p>
                Data nie może poprzedzać zakupu pojazdu (
                {dateLabel(vehicle.purchaseDate)}). Przebieg musi zachować
                kolejność względem wcześniejszych i późniejszych tankowań, także
                z tego samego dnia. Zapis zostanie sprawdzony z całą historią
                pojazdu.
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
                  : entry
                    ? "Zapisz zmiany"
                    : "Dodaj tankowanie"}
              </button>
            </div>
          </fieldset>
        </form>
      </section>
    </>
  );
}
