import { useState } from "react";
import { ArrowLeft, Check, CircleOff, Pencil, RotateCcw, Trash2 } from "lucide-react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ConfirmDialog } from "../../components/ConfirmDialog";
import { ErrorState, Loading, PageHeader } from "../../components/ui";
import { dateLabel } from "../../utils/date";
import { validId } from "../../utils/ids";
import { useVehicle } from "../vehicles/api";
import {
  useChangeReminderStatus,
  useDeleteReminder,
  useReactivateReminder,
  useReminder,
} from "./api";
import { reminderStatuses, reminderTypeLabel, type ReminderFinalStatus } from "./types";

export function ReminderDetailPage() {
  const { vehicleId: rawVehicleId, reminderId: rawReminderId } = useParams();
  const vehicleId = Number(rawVehicleId);
  const reminderId = Number(rawReminderId);
  const vehicle = useVehicle(vehicleId);
  const reminder = useReminder(vehicleId, reminderId);
  const navigate = useNavigate();
  const statusChange = useChangeReminderStatus(vehicleId);
  const reactivation = useReactivateReminder(vehicleId);
  const deletion = useDeleteReminder(vehicleId);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [notice, setNotice] = useState("");

  if (!validId(vehicleId) || !validId(reminderId)) {
    return <ErrorState error={new Error("Nieprawidłowy identyfikator przypomnienia.")} />;
  }
  if (vehicle.isPending || reminder.isPending) return <Loading />;
  if (vehicle.isError) return <ErrorState error={vehicle.error} retry={() => void vehicle.refetch()} />;
  if (reminder.isError) return <ErrorState error={reminder.error} retry={() => void reminder.refetch()} />;

  const item = reminder.data;
  const busy = statusChange.isPending || reactivation.isPending || deletion.isPending;
  const operationError = statusChange.error ?? reactivation.error;

  const changeStatus = (status: ReminderFinalStatus) => {
    statusChange.reset();
    reactivation.reset();
    setNotice("");
    statusChange.mutate({ reminderId, status }, {
      onSuccess: () => setNotice(status === "COMPLETED"
        ? "Przypomnienie oznaczono jako wykonane."
        : "Przypomnienie zostało anulowane."),
    });
  };

  const reactivate = () => {
    statusChange.reset();
    reactivation.reset();
    setNotice("");
    reactivation.mutate(reminderId, {
      onSuccess: () => setNotice("Przypomnienie zostało ponownie aktywowane."),
    });
  };

  return <>
    <Link className="text-link back-link" to={`/reminders?vehicleId=${vehicleId}`}>
      <ArrowLeft size={16} /> Wróć do przypomnień
    </Link>
    <PageHeader
      eyebrow={`${vehicle.data.make} ${vehicle.data.model}`}
      title={item.title}
      description="Szczegóły przypomnienia dla wybranego pojazdu."
    >
      <div className="reminder-detail-actions">
        <Link className="button button-primary" to={`/reminders/${vehicleId}/${reminderId}/edit`}>
          <Pencil size={16} /> Edytuj
        </Link>
        <div className="reminder-status-actions" aria-label="Akcje statusu przypomnienia">
          {item.status === "ACTIVE" ? <>
            <button className="button button-secondary reminder-complete" type="button" disabled={busy}
              onClick={() => changeStatus("COMPLETED")}><Check size={16} /> Oznacz jako wykonane</button>
            <button className="button button-secondary" type="button" disabled={busy}
              onClick={() => changeStatus("CANCELLED")}><CircleOff size={16} /> Anuluj</button>
          </> : <button className="button button-secondary reminder-reactivate" type="button" disabled={busy}
            onClick={reactivate}><RotateCcw size={16} /> Aktywuj ponownie</button>}
        </div>
        <button className="button button-danger" type="button" disabled={busy}
          onClick={() => { deletion.reset(); setConfirmDelete(true); }}><Trash2 size={16} /> Usuń</button>
      </div>
    </PageHeader>
    {notice && <div className="feedback feedback-success" role="status">{notice}</div>}
    {operationError && <ErrorState error={operationError} />}
    <section className="panel notification-reminder-detail">
      <dl>
        <div><dt>Typ</dt><dd>{reminderTypeLabel(item.type)}</dd></div>
        <div><dt>Termin</dt><dd>{dateLabel(item.dueDate)}</dd></div>
        <div><dt>Status</dt><dd>{reminderStatuses[item.status]}</dd></div>
        <div><dt>Pojazd</dt><dd>{vehicle.data.make} {vehicle.data.model}</dd></div>
        {item.odometer !== null && <div><dt>Przebieg</dt><dd>{item.odometer.toLocaleString("pl-PL")} km</dd></div>}
        {item.description && <div><dt>Opis</dt><dd>{item.description}</dd></div>}
      </dl>
    </section>
    {confirmDelete && <ConfirmDialog
      title="Usunąć przypomnienie?"
      description={<>„{item.title}” zostanie trwale usunięte. Tej operacji nie można cofnąć.</>}
      confirmLabel="Usuń przypomnienie"
      pending={deletion.isPending}
      error={deletion.error}
      close={() => { setConfirmDelete(false); deletion.reset(); }}
      confirm={() => deletion.mutate(reminderId, {
        onSuccess: () => navigate(`/reminders?vehicleId=${vehicleId}`, {
          replace: true,
          state: { notice: "Przypomnienie zostało usunięte." },
        }),
      })}
    />}
  </>;
}
