import { useEffect, useState } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { useSearchParams } from "react-router-dom";
import { ConfirmDialog } from "../../components/ConfirmDialog";
import { ErrorState, Loading } from "../../components/ui";
import { dateLabel } from "../../utils/date";
import { number } from "../../utils/format";
import { polishPlural } from "../../utils/polishPlural";
import type { Vehicle } from "../vehicles/api";
import {
  useChangeReminderStatus,
  useDeleteReminder,
  useReactivateReminder,
  useReminders,
} from "./api";
import { ReminderHistory } from "./ReminderHistory";
import { reminderFiltersSchema } from "./reminderSchema";
import {
  reminderStatuses,
  type Reminder,
  type ReminderFilters,
  type ReminderFinalStatus,
  type ReminderStatus,
} from "./types";

export function RemindersContent({ vehicle }: { vehicle: Vehicle }) {
  const [params, setParams] = useSearchParams();
  const rawPage = Number(params.get("page") ?? 0);
  const page = Number.isSafeInteger(rawPage) && rawPage >= 0 ? rawPage : 0;
  const filters: ReminderFilters = {
    status: (params.get("status") ?? "") as ReminderStatus | "",
    dueFrom: params.get("dueFrom") ?? "",
    dueTo: params.get("dueTo") ?? "",
  };
  const { status, dueFrom, dueTo } = filters;
  const parsedFilters = reminderFiltersSchema.safeParse(filters);
  const query = useReminders(vehicle.id, page, filters);
  const statusChange = useChangeReminderStatus(vehicle.id);
  const reactivation = useReactivateReminder(vehicle.id);
  const deletion = useDeleteReminder(vehicle.id);
  const [deleting, setDeleting] = useState<Reminder | null>(null);
  const [notice, setNotice] = useState("");

  useEffect(() => {
    if (query.data && page > 0 && page >= query.data.totalPages) {
      setParams(
        {
          vehicleId: String(vehicle.id),
          page: String(Math.max(0, query.data.totalPages - 1)),
          status,
          dueFrom,
          dueTo,
        },
        { replace: true },
      );
    }
  }, [query.data, page, status, dueFrom, dueTo, setParams, vehicle.id]);

  const update = (
    next: Partial<ReminderFilters> & { page?: number },
    replace = false,
  ) => {
    const nextFilters = { ...filters, ...next };
    const search: Record<string, string> = {
      vehicleId: String(vehicle.id),
      page: String(next.page ?? 0),
      status: nextFilters.status,
      dueFrom: nextFilters.dueFrom,
      dueTo: nextFilters.dueTo,
    };
    Object.keys(search).forEach((key) => {
      if (!search[key]) delete search[key];
    });
    setParams(search, { replace });
    setNotice("");
  };

  const changeStatus = (reminder: Reminder, nextStatus: ReminderFinalStatus) => {
    statusChange.mutate(
      { reminderId: reminder.id, status: nextStatus },
      {
        onSuccess: () =>
          setNotice(
            nextStatus === "COMPLETED"
              ? "Przypomnienie oznaczono jako wykonane."
              : "Przypomnienie zostało anulowane.",
          ),
      },
    );
  };

  const reactivate = (reminder: Reminder) => {
    reactivation.mutate(reminder.id, {
      onSuccess: () => setNotice("Przypomnienie zostało ponownie aktywowane."),
    });
  };

  const busy = statusChange.isPending || reactivation.isPending || deletion.isPending;
  const operationError = statusChange.error ?? reactivation.error;

  return (
    <>
      <section className="panel reminder-list-panel">
        <div className="section-heading">
          <div>
            <h2>Lista przypomnień</h2>
            <p>
              {query.data
                ? `${number(query.data.totalElements)} ${polishPlural(query.data.totalElements, ["wpis", "wpisy", "wpisów"])} · `
                : ""}
              sortowanie według najbliższego terminu
            </p>
          </div>
        </div>

        <div className="reminder-filters">
          <label className="form-field">
            <span>Status</span>
            <select
              value={filters.status}
              onChange={(event) =>
                update({ status: event.target.value as ReminderStatus | "" })
              }
            >
              <option value="">Wszystkie statusy</option>
              {Object.entries(reminderStatuses).map(([value, label]) => (
                <option value={value} key={value}>
                  {label}
                </option>
              ))}
            </select>
          </label>
          <label className="form-field">
            <span>Termin od</span>
            <input
              type="date"
              value={filters.dueFrom}
              aria-invalid={!parsedFilters.success && !!filters.dueFrom}
              onChange={(event) => update({ dueFrom: event.target.value })}
            />
          </label>
          <label className="form-field">
            <span>Termin do</span>
            <input
              type="date"
              value={filters.dueTo}
              aria-invalid={!parsedFilters.success && !!filters.dueTo}
              onChange={(event) => update({ dueTo: event.target.value })}
            />
          </label>
        </div>

        {!parsedFilters.success && (
          <p className="field-error" role="alert">
            {parsedFilters.error.issues[0]?.message}
          </p>
        )}
        {notice && (
          <div className="feedback feedback-success" role="status">
            {notice}
          </div>
        )}
        {operationError && <ErrorState error={operationError} />}

        {!parsedFilters.success ? null : query.isPending ? (
          <Loading />
        ) : query.isError ? (
          <ErrorState error={query.error} retry={() => void query.refetch()} />
        ) : (
          <>
            <ReminderHistory
              reminders={query.data.content}
              vehicleId={vehicle.id}
              busy={busy}
              filtered={!!filters.status || !!filters.dueFrom || !!filters.dueTo}
              onStatusChange={changeStatus}
              onReactivate={reactivate}
              onDelete={(reminder) => {
                deletion.reset();
                setDeleting(reminder);
              }}
            />
            {query.data.content.length > 0 && <div className="pagination">
              <span>
                Strona {query.data.totalPages ? page + 1 : 0} z {query.data.totalPages}
              </span>
              <div>
                <button
                  type="button"
                  className="button button-secondary"
                  disabled={page === 0}
                  onClick={() => update({ page: page - 1 })}
                >
                  <ChevronLeft size={16} />
                  Poprzednia
                </button>
                <button
                  type="button"
                  className="button button-secondary"
                  disabled={page + 1 >= query.data.totalPages}
                  onClick={() => update({ page: page + 1 })}
                >
                  Następna
                  <ChevronRight size={16} />
                </button>
              </div>
            </div>}
          </>
        )}
      </section>

      {deleting && (
        <ConfirmDialog
          title="Usunąć przypomnienie?"
          description={
            <>
              „{deleting.title}” z terminem {dateLabel(deleting.dueDate)} zostanie
              trwale usunięte. Tej operacji nie można cofnąć.
            </>
          }
          confirmLabel="Usuń przypomnienie"
          pending={deletion.isPending}
          error={deletion.error}
          close={() => setDeleting(null)}
          confirm={() => {
            if (deletion.isPending) return;
            deletion.mutate(deleting.id, {
              onSuccess: () => {
                setDeleting(null);
                update({
                  page:
                    page > 0 && query.data?.content.length === 1
                      ? page - 1
                      : page,
                });
                setNotice("Przypomnienie zostało usunięte.");
              },
            });
          }}
        />
      )}
    </>
  );
}
