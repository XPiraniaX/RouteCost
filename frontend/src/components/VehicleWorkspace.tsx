import type { ReactNode } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { Plus } from "lucide-react";
import { EmptyState, ErrorState, Loading, PageHeader } from "./ui";
import { useVehicle, useVehicleOptions, type Vehicle } from "../features/vehicles/api";
import { validId } from "../utils/ids";

export function VehicleWorkspace({ title, description, basePath, actionLabel, children }: {
  title: string; description: string; basePath: string; actionLabel: string; children: (vehicle: Vehicle) => ReactNode;
}) {
  const [params, setParams] = useSearchParams();
  const options = useVehicleOptions();
  const vehicles = options.data?.pages.flatMap(page => page.content) ?? [];
  const rawId = params.get("vehicleId");
  const id = rawId !== null ? Number(rawId) : vehicles[0]?.id ?? 0;
  const selected = useVehicle(id);
  return <><PageHeader eyebrow="KOSZTY I ZUŻYCIE" title={title} description={description}>{selected.data && <Link className="button button-primary" to={`${basePath}/${id}/new`}><Plus size={17} />{actionLabel}</Link>}</PageHeader>
    {options.isPending ? <Loading /> : options.isError && !options.data ? <ErrorState error={options.error} retry={() => void options.refetch()} /> : !vehicles.length && !rawId ? <section className="panel"><EmptyState /></section> : <>
      <div className="vehicle-toolbar"><label className="form-field"><span>Wybrany pojazd</span><select value={id || ""} onChange={event => setParams({ vehicleId: event.target.value })}>{!id && <option value="">Wybierz pojazd</option>}{selected.data && !vehicles.some(v => v.id === id) && <option value={id}>{selected.data.make} {selected.data.model} · {selected.data.productionYear}</option>}{vehicles.map(vehicle => <option value={vehicle.id} key={vehicle.id}>{vehicle.make} {vehicle.model} · {vehicle.productionYear}</option>)}</select></label>{options.hasNextPage && <button className="button button-secondary" disabled={options.isFetchingNextPage} onClick={() => void options.fetchNextPage()}>{options.isFetchingNextPage ? "Ładowanie…" : "Wczytaj kolejne pojazdy"}</button>}<p className="muted">Statystyki dotyczą całej historii wybranego pojazdu.</p></div>
      {options.isFetchNextPageError && <ErrorState error={options.error} retry={() => void options.fetchNextPage()} />}
      {!validId(id) ? <ErrorState error={new Error("Nieprawidłowy identyfikator pojazdu. Wybierz pojazd z listy.")} /> : selected.isPending ? <Loading /> : selected.isError ? <ErrorState error={selected.error} retry={() => void selected.refetch()} /> : children(selected.data)}
    </>}
  </>;
}
