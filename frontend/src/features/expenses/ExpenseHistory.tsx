import { Pencil, ReceiptText, Trash2 } from "lucide-react";
import { Link } from "react-router-dom";
import { dateLabel } from "../../utils/date";
import { number } from "../../utils/format";
import { categoryLabel, type Expense } from "./types";

export function ExpenseHistory({
  expenses,
  vehicleId,
  onDelete,
}: {
  expenses: Expense[];
  vehicleId: number;
  onDelete: (expense: Expense) => void;
}) {
  if (!expenses.length)
    return (
      <div className="empty-state">
        <span className="empty-icon">
          <ReceiptText size={28} />
        </span>
        <h3>Brak wydatków w tym widoku</h3>
        <p>Dodaj wydatek lub zmień filtry, aby zobaczyć wpisy.</p>
      </div>
    );
  return (
    <div className="table-scroll">
      <table className="data-table expense-table expense-history-table">
        <caption className="sr-only">
          Wydatki pojazdu. Wszystkie kwoty są podane w PLN.
        </caption>
        <thead>
          <tr>
            <th>Data</th>
            <th>Kategoria</th>
            <th className="numeric">Kwota</th>
            <th>Opis</th>
            <th className="numeric">Przebieg</th>
            <th>
              <span className="sr-only">Akcje</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {expenses.map((expense) => (
            <tr key={expense.id}>
              <td data-label="Data">
                <strong>{dateLabel(expense.expenseDate)}</strong>
              </td>
              <td data-label="Kategoria">{categoryLabel(expense.category)}</td>
              <td data-label="Kwota" className="numeric">
                <strong>
                  {number(expense.amount)} PLN
                </strong>
              </td>
              <td data-label="Opis">
                {expense.description || <span className="muted">—</span>}
              </td>
              <td data-label="Przebieg" className="numeric">
                {expense.odometer === null ? (
                  <span className="muted">—</span>
                ) : (
                  `${number(expense.odometer)} km`
                )}
              </td>
              <td className="expense-row-actions">
                <div className="table-actions">
                  <Link
                    className="icon-button"
                    aria-label={`Edytuj wydatek z ${dateLabel(expense.expenseDate)}`}
                    title="Edytuj"
                    to={`/expenses/${vehicleId}/${expense.id}/edit`}
                  >
                    <Pencil size={16} />
                  </Link>
                  <button
                    className="icon-button danger-icon"
                    aria-label={`Usuń wydatek z ${dateLabel(expense.expenseDate)}`}
                    title="Usuń"
                    onClick={() => onDelete(expense)}
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
