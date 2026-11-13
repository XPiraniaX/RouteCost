import { number } from "../../utils/format";
import { lazy, Suspense, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ArrowLeft, Pencil, Trash2 } from "lucide-react";
import { ErrorState, Loading, PageHeader } from "../../components/ui";
import { fuels, useVehicle } from "./api";
import { DeleteVehicleDialog } from "./DeleteVehicleDialog";
const CostReport = lazy(() =>
  import("./CostReport").then((module) => ({ default: module.CostReport })),
);
export function VehicleDetailPage() {
  const { vehicleId } = useParams();
  const id = Number(vehicleId);
  const query = useVehicle(id);
  const [deleting, setDeleting] = useState(false);
  if (!Number.isSafeInteger(id) || id <= 0)
    return (
      <ErrorState error={new Error("Nieprawidłowy identyfikator pojazdu.")} />
    );
  if (query.isPending) return <Loading />;
  if (query.isError)
    return (
      <ErrorState error={query.error} retry={() => void query.refetch()} />
    );
  const v = query.data;
  return (
    <>
      <Link className="text-link back-link" to="/vehicles">
        <ArrowLeft size={16} />
        Wszystkie pojazdy
      </Link>
      <PageHeader
        eyebrow="SZCZEGÓŁY POJAZDU"
        title={`${v.make} ${v.model}`}
        description={`${v.productionYear} · ${fuels[v.fuelType]} · ${number(v.currentOdometer)} km`}
      >
        <Link className="button button-secondary" to={`/vehicles/${id}/edit`}>
          <Pencil size={15} />
          Edytuj
        </Link>
        <button
          className="button button-secondary"
          onClick={() => setDeleting(true)}
        >
          <Trash2 size={15} />
          Usuń
        </button>
      </PageHeader>
      <section className="panel">
        <div className="section-heading">
          <h2>Informacje o pojeździe</h2>
          <span className="badge">{fuels[v.fuelType]}</span>
        </div>
        <dl className="detail-info">
          <div>
            <dt>Rok produkcji</dt>
            <dd>{v.productionYear}</dd>
          </div>
          <div>
            <dt>Aktualny przebieg</dt>
            <dd>{number(v.currentOdometer)} km</dd>
          </div>
          <div>
            <dt>Data zakupu</dt>
            <dd>
              {new Date(`${v.purchaseDate}T12:00:00`).toLocaleDateString(
                "pl-PL",
              )}
            </dd>
          </div>
          <div>
            <dt>Cena zakupu (bez waluty)</dt>
            <dd>{number(v.purchasePrice)}</dd>
          </div>
        </dl>
      </section>
      <Suspense fallback={<Loading />}>
        <CostReport id={id} />
      </Suspense>
      {deleting && (
        <DeleteVehicleDialog vehicle={v} close={() => setDeleting(false)} />
      )}
    </>
  );
}
