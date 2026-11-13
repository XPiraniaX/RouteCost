import { number } from "../../utils/format";
import { ArrowUpRight, CarFront, Pencil } from "lucide-react";
import { Link } from "react-router-dom";
import { EmptyState } from "../../components/ui";
import { fuels, type Vehicle } from "./api";
export function VehicleTable({
  vehicles,
  filtered = false,
}: {
  vehicles: Vehicle[];
  filtered?: boolean;
}) {
  if (!vehicles.length) return <EmptyState filtered={filtered} />;
  return (
    <div className="table-scroll">
      <table className="data-table">
        <thead>
          <tr>
            <th>Pojazd</th>
            <th>Rocznik</th>
            <th>Paliwo</th>
            <th className="numeric">Przebieg</th>
            <th>
              <span className="sr-only">Akcje</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {vehicles.map((v) => (
            <tr key={v.id}>
              <td>
                <Link className="vehicle-name" to={`/vehicles/${v.id}`}>
                  <span className="vehicle-icon">
                    <CarFront size={21} />
                  </span>
                  <span>
                    <strong>
                      {v.make} {v.model}
                    </strong>
                    <small>
                      Dodano {new Date(v.createdAt).toLocaleDateString("pl-PL")}
                    </small>
                  </span>
                </Link>
              </td>
              <td>{v.productionYear}</td>
              <td>
                <span className="badge">{fuels[v.fuelType]}</span>
              </td>
              <td className="numeric">
                {number(v.currentOdometer)} <span className="muted">km</span>
              </td>
              <td>
                <div className="table-actions">
                  <Link
                    className="icon-button"
                    aria-label={`Edytuj ${v.make} ${v.model}`}
                    to={`/vehicles/${v.id}/edit`}
                  >
                    <Pencil size={16} />
                  </Link>
                  <Link
                    className="icon-button"
                    aria-label={`Szczegóły ${v.make} ${v.model}`}
                    to={`/vehicles/${v.id}`}
                  >
                    <ArrowUpRight size={19} />
                  </Link>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
