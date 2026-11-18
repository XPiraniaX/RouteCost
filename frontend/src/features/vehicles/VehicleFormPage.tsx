import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ArrowLeft, Check, LoaderCircle } from "lucide-react";
import { ApiError } from "../../api/errors";
import { ErrorState, Loading, PageHeader } from "../../components/ui";
import {
  fuels,
  useVehicle,
  vehicleApi,
  type Vehicle,
  type VehicleRequest,
} from "./api";
import { localToday, vehicleSchema } from "./vehicleSchema";

export function VehicleFormPage() {
  const { vehicleId } = useParams();
  const id = Number(vehicleId);
  const query = useVehicle(id);
  if (vehicleId && (!Number.isSafeInteger(id) || id <= 0))
    return (
      <ErrorState error={new Error("Nieprawidłowy identyfikator pojazdu.")} />
    );
  if (vehicleId && query.isPending) return <Loading />;
  if (vehicleId && query.isError)
    return (
      <ErrorState error={query.error} retry={() => void query.refetch()} />
    );
  return (
    <VehicleForm
      key={vehicleId || "new"}
      vehicle={vehicleId ? query.data : undefined}
    />
  );
}
function VehicleForm({ vehicle }: { vehicle?: Vehicle }) {
  const navigate = useNavigate();
  const client = useQueryClient();
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<VehicleRequest>({
    resolver: zodResolver(vehicleSchema(vehicle?.currentOdometer)),
    defaultValues: vehicle
      ? {
          make: vehicle.make,
          model: vehicle.model,
          productionYear: vehicle.productionYear,
          fuelType: vehicle.fuelType,
          currentOdometer: vehicle.currentOdometer,
          purchaseDate: vehicle.purchaseDate,
          purchasePrice: vehicle.purchasePrice || undefined,
        }
      : { make: "", model: "", fuelType: "PETROL", currentOdometer: 0 },
  });
  const mutation = useMutation({
    mutationFn: (data: VehicleRequest) => vehicleApi.save(data, vehicle?.id),
    onSuccess: async (saved) => {
      await Promise.all([
        client.invalidateQueries({ queryKey: ["vehicles"] }),
        client.invalidateQueries({ queryKey: ["dashboard"] }),
      ]);
      navigate(`/vehicles/${saved.id}`, {
        replace: true,
        state: {
          notice: vehicle
            ? "Zmiany zostały zapisane."
            : "Pojazd został dodany.",
        },
      });
    },
    onError: (error) => {
      if (error instanceof ApiError)
        Object.entries(error.fieldErrors).forEach(([key, message]) => {
          if (key in vehicleSchema().shape)
            setError(key as keyof VehicleRequest, { message });
        });
    },
  });
  const back = vehicle ? `/vehicles/${vehicle.id}` : "/vehicles";
  const error = (key: keyof VehicleRequest) =>
    errors[key] ? (
      <span id={`${key}-error`} className="field-error" role="alert">
        {errors[key]?.message}
      </span>
    ) : null;
  const attrs = (key: keyof VehicleRequest) => ({
    "aria-invalid": !!errors[key],
    "aria-describedby": errors[key] ? `${key}-error` : undefined,
  });
  return (
    <div className="vehicle-form-page">
      <Link className="text-link back-link" to={back}>
        <ArrowLeft size={16} />
        Wróć do {vehicle ? "pojazdu" : "pojazdów"}
      </Link>
      <PageHeader
        eyebrow="TWÓJ GARAŻ"
        title={vehicle ? "Edytuj pojazd" : "Dodaj pojazd"}
        description="Zacznij od podstaw. Dane możesz zaktualizować w dowolnej chwili."
      />
      <section className="panel form-panel">
        <div className="section-heading">
          <div>
            <h2>Dane pojazdu</h2>
            <p>Uzupełnij dane pojazdu. Cena zakupu jest opcjonalna.</p>
          </div>
          <span className="badge">{vehicle ? "Edycja" : "Nowy pojazd"}</span>
        </div>
        <form
          className="vehicle-form"
          noValidate
          onSubmit={handleSubmit((data) => mutation.mutate(data))}
        >
          {mutation.isError && <ErrorState error={mutation.error} />}
          <fieldset
            disabled={mutation.isPending}
            style={{ border: 0, padding: 0, margin: 0, minWidth: 0 }}
          >
            <div className="form-grid">
              <label className="form-field">
                <span>Marka</span>
                <input
                  autoFocus
                  maxLength={100}
                  autoComplete="off"
                  placeholder="np. Toyota"
                  {...register("make")}
                  {...attrs("make")}
                />
                {error("make")}
              </label>
              <label className="form-field">
                <span>Model</span>
                <input
                  maxLength={100}
                  placeholder="np. Corolla"
                  {...register("model")}
                  {...attrs("model")}
                />
                {error("model")}
              </label>
              <label className="form-field">
                <span>Rok produkcji</span>
                <input
                  type="number"
                  min={1886}
                  max={2100}
                  placeholder="np. 2020"
                  {...register("productionYear", { valueAsNumber: true })}
                  {...attrs("productionYear")}
                />
                {error("productionYear")}
              </label>
              <label className="form-field">
                <span>Rodzaj paliwa</span>
                <select {...register("fuelType")} {...attrs("fuelType")}>
                  {Object.entries(fuels).map(([v, label]) => (
                    <option key={v} value={v}>
                      {label}
                    </option>
                  ))}
                </select>
                {error("fuelType")}
              </label>
              <label className="form-field">
                <span>Aktualny przebieg (km)</span>
                <input
                  type="number"
                  min={vehicle?.currentOdometer || 0}
                  max={2147483647}
                  step={1}
                  {...register("currentOdometer", { valueAsNumber: true })}
                  {...attrs("currentOdometer")}
                />
                <small>
                  {vehicle
                    ? "Przebieg nie może być niższy od zapisanego."
                    : "Podaj aktualny stan licznika."}
                </small>
                {error("currentOdometer")}
              </label>
              <label className="form-field">
                <span>Data zakupu</span>
                <input
                  type="date"
                  max={localToday()}
                  {...register("purchaseDate")}
                  {...attrs("purchaseDate")}
                />
                {error("purchaseDate")}
              </label>
              <label className="form-field">
                <span>Cena zakupu (opcjonalna)</span>
                <input
                  aria-label="Cena zakupu"
                  type="number"
                  min={0}
                  max={9999999999.99}
                  step="0.01"
                  placeholder="0,00"
                  {...register("purchasePrice", { valueAsNumber: true })}
                  {...attrs("purchasePrice")}
                />
                <small>
                  Cena zakupu jest zapisywana bez przypisanej waluty.
                </small>
                {error("purchasePrice")}
              </label>
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
                  : vehicle
                    ? "Zapisz zmiany"
                    : "Dodaj pojazd"}
              </button>
            </div>
          </fieldset>
        </form>
      </section>
    </div>
  );
}
