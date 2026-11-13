import { useState } from "react";
import { ChevronLeft, ChevronRight, SlidersHorizontal } from "lucide-react";
import {
  AddVehicle,
  ErrorState,
  Loading,
  PageHeader,
} from "../../components/ui";
import { fuels, useVehicles } from "./api";
import { VehicleTable } from "./VehicleTable";
export function VehiclesPage() {
  const [page, setPage] = useState(0);
  const [fuel, setFuel] = useState("");
  const query = useVehicles(page, fuel);
  return (
    <>
      <PageHeader
        eyebrow="TWÓJ GARAŻ"
        title="Pojazdy"
        description="Zarządzaj pojazdami i poznaj koszty ich użytkowania."
      >
        <AddVehicle />
      </PageHeader>
      <section className="panel">
        <div className="section-heading">
          <div>
            <h2>Lista pojazdów</h2>
            <p>
              {query.data
                ? `${query.data.totalElements} pojazdów w wybranym widoku`
                : "Dane Twojego konta"}
            </p>
          </div>
          <label className="filter-label">
            <SlidersHorizontal size={16} />
            <span className="sr-only">Filtruj według paliwa</span>
            <select
              value={fuel}
              onChange={(e) => {
                setFuel(e.target.value);
                setPage(0);
              }}
            >
              <option value="">Wszystkie paliwa</option>
              {Object.entries(fuels).map(([v, text]) => (
                <option value={v} key={v}>
                  {text}
                </option>
              ))}
            </select>
          </label>
        </div>
        {query.isPending ? (
          <Loading />
        ) : query.isError ? (
          <ErrorState error={query.error} retry={() => void query.refetch()} />
        ) : (
          <>
            <VehicleTable vehicles={query.data.content} filtered={!!fuel} />
            <div className="pagination">
              <span>
                Strona {query.data.totalPages ? page + 1 : 0} z{" "}
                {query.data.totalPages}
              </span>
              <div>
                <button
                  className="button button-secondary"
                  disabled={page === 0}
                  onClick={() => setPage(page - 1)}
                >
                  <ChevronLeft size={16} />
                  Poprzednia
                </button>
                <button
                  className="button button-secondary"
                  disabled={page + 1 >= query.data.totalPages}
                  onClick={() => setPage(page + 1)}
                >
                  Następna
                  <ChevronRight size={16} />
                </button>
              </div>
            </div>
          </>
        )}
      </section>
    </>
  );
}
