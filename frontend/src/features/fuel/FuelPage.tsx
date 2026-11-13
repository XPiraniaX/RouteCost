import { lazy, Suspense, useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { ChevronLeft, ChevronRight, Plus } from "lucide-react";
import {
  ErrorState,
  Loading,
  EmptyState,
  PageHeader,
} from "../../components/ui";
import { ConfirmDialog } from "../../components/ConfirmDialog";
import { dateLabel } from "../../utils/date";
import { number } from "../../utils/format";
import { useVehicle, type Vehicle } from "../vehicles/api";
import {
  useDeleteFuel,
  useFuelEntries,
  useVehicleOptions,
  validId,
  type FuelEntry,
  type SortOrder,
} from "./api";
import { FuelHistory } from "./FuelHistory";
import { FuelStatistics } from "./FuelStatistics";
const FuelCharts = lazy(() =>
  import("./FuelCharts").then((module) => ({ default: module.FuelCharts })),
);

export function FuelPage() {
  const [params, setParams] = useSearchParams();
  const options = useVehicleOptions();
  const vehicles = options.data?.pages.flatMap((page) => page.content) ?? [];
  const rawId = params.get("vehicleId");
  const id = rawId !== null ? Number(rawId) : (vehicles[0]?.id ?? 0);
  const selected = useVehicle(id);
  const changeVehicle = (value: string) => setParams({ vehicleId: value });
  return (
    <>
      <PageHeader
        eyebrow="KOSZTY I ZUŻYCIE"
        title="Tankowania"
        description="Historia paliwa, wydatki na tankowania i rzeczywiste spalanie."
      >
        {selected.data && (
          <Link className="button button-primary" to={`/fuel/${id}/new`}>
            <Plus size={17} />
            Dodaj tankowanie
          </Link>
        )}
      </PageHeader>
      {options.isPending ? (
        <Loading />
      ) : options.isError && !options.data ? (
        <ErrorState
          error={options.error}
          retry={() => void options.refetch()}
        />
      ) : !vehicles.length && !rawId ? (
        <section className="panel">
          <EmptyState />
        </section>
      ) : (
        <>
          <div className="fuel-toolbar">
            <label className="form-field">
              <span>Wybrany pojazd</span>
              <select
                value={id || ""}
                onChange={(event) => changeVehicle(event.target.value)}
              >
                {!id && <option value="">Wybierz pojazd</option>}
                {selected.data && !vehicles.some((v) => v.id === id) && (
                  <option value={id}>
                    {selected.data.make} {selected.data.model} ·{" "}
                    {selected.data.productionYear}
                  </option>
                )}
                {vehicles.map((vehicle) => (
                  <option value={vehicle.id} key={vehicle.id}>
                    {vehicle.make} {vehicle.model} · {vehicle.productionYear}
                  </option>
                ))}
              </select>
            </label>
            {options.hasNextPage && (
              <button
                className="button button-secondary"
                disabled={options.isFetchingNextPage}
                onClick={() => void options.fetchNextPage()}
              >
                {options.isFetchingNextPage
                  ? "Ładowanie…"
                  : "Wczytaj kolejne pojazdy"}
              </button>
            )}
            <p className="muted">
              Statystyki dotyczą całej historii wybranego pojazdu.
            </p>
          </div>
          {options.isFetchNextPageError && (
            <ErrorState
              error={options.error}
              retry={() => void options.fetchNextPage()}
            />
          )}
          {!validId(id) ? (
            <ErrorState
              error={
                new Error(
                  "Nieprawidłowy identyfikator pojazdu. Wybierz pojazd z listy.",
                )
              }
            />
          ) : selected.isPending ? (
            <Loading />
          ) : selected.isError ? (
            <ErrorState
              error={selected.error}
              retry={() => void selected.refetch()}
            />
          ) : (
            <FuelContent key={id} vehicle={selected.data} />
          )}
        </>
      )}
    </>
  );
}
function FuelContent({ vehicle }: { vehicle: Vehicle }) {
  const [params, setParams] = useSearchParams();
  const rawPage = Number(params.get("page") ?? 0);
  const page = Number.isSafeInteger(rawPage) && rawPage >= 0 ? rawPage : 0;
  const order: SortOrder = params.get("order") === "asc" ? "asc" : "desc";
  const query = useFuelEntries(vehicle.id, page, order);
  useEffect(() => {
    if (query.data && page > 0 && page >= query.data.totalPages) {
      setParams(
        {
          vehicleId: String(vehicle.id),
          page: String(Math.max(0, query.data.totalPages - 1)),
          order,
        },
        { replace: true },
      );
    }
  }, [query.data, page, order, setParams, vehicle.id]);
  const [deleting, setDeleting] = useState<FuelEntry | null>(null);
  const deletion = useDeleteFuel(vehicle.id);
  const updateView = (nextPage: number, nextOrder = order, notice?: string) =>
    setParams(
      {
        vehicleId: String(vehicle.id),
        page: String(nextPage),
        order: nextOrder,
      },
      { state: notice ? { notice } : null },
    );
  return (
    <>
      <FuelStatistics id={vehicle.id} />
      <section className="panel">
        <div className="section-heading">
          <div>
            <h2>Historia tankowań</h2>
            <p>
              {query.data
                ? `Liczba wpisów: ${number(query.data.totalElements)} · `
                : ""}
              Koszt i cena za litr bez określonej waluty
            </p>
          </div>
          <label className="filter-label">
            <span className="sr-only">Kolejność tankowań</span>
            <select
              value={order}
              onChange={(e) => updateView(0, e.target.value as SortOrder)}
            >
              <option value="desc">Od najnowszych</option>
              <option value="asc">Od najstarszych</option>
            </select>
          </label>
        </div>
        {query.isPending ? (
          <Loading />
        ) : query.isError ? (
          <ErrorState error={query.error} retry={() => void query.refetch()} />
        ) : (
          <>
            <FuelHistory
              entries={query.data.content}
              onDelete={(entry) => {
                deletion.reset();
                setDeleting(entry);
              }}
            />
            <div className="pagination">
              <span>
                Strona {query.data.totalPages ? page + 1 : 0} z{" "}
                {query.data.totalPages}
              </span>
              <div>
                <button
                  className="button button-secondary"
                  disabled={page === 0}
                  onClick={() => updateView(page - 1)}
                >
                  <ChevronLeft size={16} />
                  Poprzednia
                </button>
                <button
                  className="button button-secondary"
                  disabled={page + 1 >= query.data.totalPages}
                  onClick={() => updateView(page + 1)}
                >
                  Następna
                  <ChevronRight size={16} />
                </button>
              </div>
            </div>
          </>
        )}
      </section>
      <p className="report-note">
        Sortowanie obejmuje całą historię; wpisy z tą samą datą są porządkowane
        według identyfikatora. Filtrowanie po zakresie dat nie jest obecnie
        dostępne.
      </p>
      {query.data && (
        <Suspense fallback={<Loading />}>
          <FuelCharts id={vehicle.id} entries={query.data.content} />
        </Suspense>
      )}
      {deleting && (
        <ConfirmDialog
          title="Usunąć tankowanie?"
          description={
            <>
              Tankowanie z {dateLabel(deleting.entryDate)} (
              {number(deleting.liters)} l) zostanie trwale usunięte. Statystyki
              i spalanie zostaną przeliczone. Tej operacji nie można cofnąć.
            </>
          }
          confirmLabel="Usuń tankowanie"
          pending={deletion.isPending}
          error={deletion.error}
          close={() => setDeleting(null)}
          confirm={() => {
            if (deletion.isPending) return;
            deletion.mutate(deleting.id, {
              onSuccess: () => {
                setDeleting(null);
                updateView(
                  page > 0 && query.data?.content.length === 1
                    ? page - 1
                    : page,
                  order,
                  "Tankowanie zostało usunięte.",
                );
              },
            });
          }}
        />
      )}
    </>
  );
}
