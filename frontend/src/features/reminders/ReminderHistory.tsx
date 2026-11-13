import {
  BellRing,
  Check,
  CircleOff,
  Pencil,
  RotateCcw,
  Trash2,
} from "lucide-react";
import { Link } from "react-router-dom";
import { dateLabel, localToday } from "../../utils/date";
import { number } from "../../utils/format";
import { reminderStatusLabel } from "./reminderSchema";
import { reminderTypeLabel, type Reminder, type ReminderFinalStatus } from "./types";

export function ReminderHistory({
  reminders,
  vehicleId,
  busy,
  onStatusChange,
  onReactivate,
  onDelete,
  filtered,
}: {
  reminders: Reminder[];
  vehicleId: number;
  busy: boolean;
  onStatusChange: (reminder: Reminder, status: ReminderFinalStatus) => void;
  onReactivate: (reminder: Reminder) => void;
  onDelete: (reminder: Reminder) => void;
  filtered: boolean;
}) {
  if (!reminders.length) {
    return (
      <div className="empty-state">
        <span className="empty-icon">
          <BellRing size={28} />
        </span>
        <h3>
          {filtered
            ? "Brak przypomnień dla wybranych filtrów"
            : "Nie masz jeszcze przypomnień"}
        </h3>
        <p>
          {filtered
            ? "Zmień status lub zakres terminów, aby zobaczyć inne wpisy."
            : "Dodaj przypomnienie o serwisie, ubezpieczeniu lub przeglądzie."}
        </p>
      </div>
    );
  }

  return (
    <div className="table-scroll">
      <table className="data-table reminder-table">
        <caption className="sr-only">
          Przypomnienia wybranego pojazdu wraz z terminami, statusami i akcjami.
        </caption>
        <thead>
          <tr>
            <th>Przypomnienie</th>
            <th>Typ</th>
            <th>Termin</th>
            <th className="numeric">Przebieg</th>
            <th>Status</th>
            <th>
              <span className="sr-only">Akcje</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {reminders.map((reminder) => {
            const overdue =
              reminder.status === "ACTIVE" &&
              reminder.dueDate < localToday();
            const dueLabel = dateLabel(reminder.dueDate);
            return (
              <tr key={reminder.id}>
                <td data-label="Przypomnienie" className="reminder-title-cell">
                  <strong>{reminder.title}</strong>
                  {reminder.description && (
                    <span className="muted">{reminder.description}</span>
                  )}
                </td>
                <td data-label="Typ">{reminderTypeLabel(reminder.type)}</td>
                <td data-label="Termin">
                  <strong>{dueLabel}</strong>
                  {overdue && (
                    <span className="reminder-overdue-label">
                      Termin minął
                    </span>
                  )}
                </td>
                <td data-label="Przebieg" className="numeric">
                  {reminder.odometer === null ? (
                    <span className="muted">Nie określono</span>
                  ) : (
                    `${number(reminder.odometer)} km`
                  )}
                </td>
                <td data-label="Status">
                  <span
                    className={`badge reminder-status reminder-status-${reminder.status.toLowerCase()}${overdue ? " reminder-status-overdue" : ""}`}
                    aria-label={
                      overdue
                        ? `Aktywne, przeterminowane od ${dueLabel}`
                        : reminderStatusLabel(reminder.status)
                    }
                  >
                    {overdue ? "Przeterminowane" : reminderStatusLabel(reminder.status)}
                  </span>
                </td>
                <td className="reminder-row-actions">
                  <div className="table-actions">
                    <Link
                      className="icon-button"
                      aria-label={`Edytuj przypomnienie: ${reminder.title}`}
                      title="Edytuj"
                      to={`/reminders/${vehicleId}/${reminder.id}/edit`}
                    >
                      <Pencil size={16} />
                    </Link>
                    {reminder.status === "ACTIVE" ? (
                      <>
                        <button
                          type="button"
                          className="icon-button reminder-complete"
                          aria-label={`Oznacz jako wykonane: ${reminder.title}`}
                          title="Oznacz jako wykonane"
                          disabled={busy}
                          onClick={() => onStatusChange(reminder, "COMPLETED")}
                        >
                          <Check size={16} />
                        </button>
                        <button
                          type="button"
                          className="icon-button"
                          aria-label={`Anuluj przypomnienie: ${reminder.title}`}
                          title="Anuluj"
                          disabled={busy}
                          onClick={() => onStatusChange(reminder, "CANCELLED")}
                        >
                          <CircleOff size={16} />
                        </button>
                      </>
                    ) : (
                      <button
                        type="button"
                        className="icon-button reminder-reactivate"
                        aria-label={`Aktywuj ponownie: ${reminder.title}`}
                        title="Aktywuj ponownie"
                        disabled={busy}
                        onClick={() => onReactivate(reminder)}
                      >
                        <RotateCcw size={16} />
                      </button>
                    )}
                    <button
                      type="button"
                      className="icon-button danger-icon"
                      aria-label={`Usuń przypomnienie: ${reminder.title}`}
                      title="Usuń"
                      disabled={busy}
                      onClick={() => onDelete(reminder)}
                    >
                      <Trash2 size={16} />
                    </button>
                  </div>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
