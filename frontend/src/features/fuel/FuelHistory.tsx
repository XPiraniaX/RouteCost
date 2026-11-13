import { Fuel, Pencil, Trash2 } from "lucide-react";
import { Link } from "react-router-dom";
import { number } from "../../utils/format";
import { dateLabel } from "../../utils/date";
import type { FuelEntry } from "./api";

export function FuelHistory({
  entries,
  onDelete,
}: {
  entries: FuelEntry[];
  onDelete: (entry: FuelEntry) => void;
}) {
  if (!entries.length)
    return (
      <div className="empty-state">
        <span className="empty-icon">
          <Fuel size={28} />
        </span>
        <h3>Historia czeka na pierwszy wpis</h3>
        <p>
          Dodaj tankowanie. Kolejne wpisy do pełna pozwolą obliczyć spalanie.
        </p>
      </div>
    );
  return (
    <div className="table-scroll">
      <table className="data-table fuel-table">
        <caption className="sr-only">
          Historia tankowań. Kwoty bez określonej waluty.
        </caption>
        <thead>
          <tr>
            <th>Data</th>
            <th className="numeric">Przebieg</th>
            <th className="numeric">Paliwo (l)</th>
            <th className="numeric">Koszt</th>
            <th className="numeric">Cena / l</th>
            <th>Do pełna</th>
            <th className="numeric">l/100 km</th>
            <th>
              <span className="sr-only">Akcje</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {entries.map((entry) => (
            <tr key={entry.id}>
              <td data-label="Data">
                <strong>{dateLabel(entry.entryDate)}</strong>
              </td>
              <td data-label="Przebieg" className="numeric">
                {number(entry.odometer)} km
              </td>
              <td data-label="Paliwo (l)" className="numeric">
                {new Intl.NumberFormat("pl-PL", {
                  maximumFractionDigits: 3,
                }).format(entry.liters)}
              </td>
              <td data-label="Koszt (bez waluty)" className="numeric">
                {number(entry.totalCost)}
              </td>
              <td data-label="Cena / l (bez waluty)" className="numeric">
                {new Intl.NumberFormat("pl-PL", {
                  maximumFractionDigits: 4,
                }).format(entry.pricePerLiter)}
              </td>
              <td data-label="Do pełna">
                <span
                  className={`badge ${entry.fullTank ? "badge-positive" : ""}`}
                >
                  {entry.fullTank ? "Tak" : "Nie"}
                </span>
              </td>
              <td data-label="Spalanie (l/100 km)" className="numeric">
                {entry.consumptionFromPreviousFullTank === null ? (
                  <span
                    className="muted"
                    title="Brak poprawnego odcinka między tankowaniami do pełna"
                  >
                    Brak danych
                  </span>
                ) : (
                  number(entry.consumptionFromPreviousFullTank)
                )}
              </td>
              <td className="fuel-row-actions">
                <div className="table-actions">
                  <Link
                    className="icon-button"
                    aria-label={`Edytuj tankowanie z ${dateLabel(entry.entryDate)}`}
                    to={`/fuel/${entry.vehicleId}/${entry.id}/edit`}
                  >
                    <Pencil size={16} />
                  </Link>
                  <button
                    className="icon-button"
                    aria-label={`Usuń tankowanie z ${dateLabel(entry.entryDate)}`}
                    onClick={() => onDelete(entry)}
                  >
                    <Trash2 size={16} />
                  </button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
