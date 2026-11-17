import { useEffect, useState } from "react";
import { ChevronLeft, ChevronRight, SlidersHorizontal } from "lucide-react";
import { useSearchParams } from "react-router-dom";
import { ConfirmDialog } from "../../components/ConfirmDialog";
import { ErrorState, Loading } from "../../components/ui";
import { dateLabel } from "../../utils/date";
import { number } from "../../utils/format";
import type { Vehicle } from "../vehicles/api";
import { ExpenseHistory } from "./ExpenseHistory";
import { ExpenseStatistics } from "./ExpenseStatistics";
import { useDeleteExpense, useExpenses } from "./api";
import {
  categoryLabel,
  expenseCategories,
  type Expense,
  type ExpenseFilters,
  type SortOrder,
} from "./types";
import { expenseFiltersSchema } from "./expenseSchema";
import { polishPlural } from "../../utils/polishPlural";

export function ExpenseContent({ vehicle }: { vehicle: Vehicle }) {
  const [params, setParams] = useSearchParams();
  const rawPage = Number(params.get("page") ?? 0);
  const page = Number.isSafeInteger(rawPage) && rawPage >= 0 ? rawPage : 0;
  const order: SortOrder = params.get("order") === "asc" ? "asc" : "desc";
  const filters: ExpenseFilters = {
    category: params.get("category") ?? "",
    dateFrom: params.get("dateFrom") ?? "",
    dateTo: params.get("dateTo") ?? "",
  };
  const { category, dateFrom, dateTo } = filters;
  const parsedFilters = expenseFiltersSchema.safeParse(filters);
  const query = useExpenses(vehicle.id, page, order, filters);
  const [deleting, setDeleting] = useState<Expense | null>(null);
  const deletion = useDeleteExpense(vehicle.id);
  useEffect(() => {
    if (query.data && page > 0 && page >= query.data.totalPages) {
      setParams(
        {
          vehicleId: String(vehicle.id),
          page: String(Math.max(0, query.data.totalPages - 1)),
          order,
          category,
          dateFrom,
          dateTo,
        },
        { replace: true },
      );
    }
  }, [
    query.data,
    page,
    order,
    category,
    dateFrom,
    dateTo,
    setParams,
    vehicle.id,
  ]);
  const update = (
    next: Partial<ExpenseFilters & { page: number; order: SortOrder }>,
    replace = false,
  ) => {
    const nextFilters = { ...filters, ...next };
    const state: Record<string, string> = {
      vehicleId: String(vehicle.id),
      page: String(next.page ?? 0),
      order: next.order ?? order,
      category: nextFilters.category,
      dateFrom: nextFilters.dateFrom,
      dateTo: nextFilters.dateTo,
    };
    Object.keys(state).forEach((key) => {
      if (!state[key as keyof typeof state])
        delete state[key as keyof typeof state];
    });
    setParams(state, { replace });
  };
  return (
    <>
      <ExpenseStatistics id={vehicle.id} />
      <section className="panel">
        <div className="section-heading">
          <div>
            <h2>Historia wydatków</h2>
            <p>
              {query.data
                ? `${number(query.data.totalElements)} ${polishPlural(query.data.totalElements, ["wpis", "wpisy", "wpisów"])} · `
                : ""}
              kwoty są oznaczone walutą
            </p>
          </div>
          <label className="filter-label">
            <span className="sr-only">Kolejność wydatków</span>
            <select
              value={order}
              onChange={(event) =>
                update({ page: 0, order: event.target.value as SortOrder })
              }
            >
              <option value="desc">Od najnowszych</option>
              <option value="asc">Od najstarszych</option>
            </select>
          </label>
        </div>
        <div className="expense-filters">
          <label className="form-field">
            <span>Kategoria</span>
            <select
              value={filters.category}
              onChange={(event) =>
                update({ category: event.target.value, page: 0 })
              }
            >
              <option value="">Wszystkie kategorie</option>
              {Object.entries(expenseCategories).map(([key, label]) => (
                <option key={key} value={key}>
                  {label}
                </option>
              ))}
            </select>
          </label>
          <label className="form-field">
            <span>Data od</span>
            <input
              type="date"
              value={filters.dateFrom}
              aria-invalid={!parsedFilters.success && !!filters.dateFrom}
              onChange={(event) =>
                update({ dateFrom: event.target.value, page: 0 })
              }
            />
          </label>
          <label className="form-field">
            <span>Data do</span>
            <input
              type="date"
              value={filters.dateTo}
              aria-invalid={!parsedFilters.success && !!filters.dateTo}
              onChange={(event) =>
                update({ dateTo: event.target.value, page: 0 })
              }
            />
          </label>
          <SlidersHorizontal
            className="expense-filter-icon"
            size={17}
            aria-hidden="true"
          />
        </div>
        {!parsedFilters.success && (
          <p className="field-error" role="alert">
            {parsedFilters.error.issues[0]?.message}
          </p>
        )}
        {!parsedFilters.success ? null : query.isPending ? (
          <Loading />
        ) : query.isError ? (
          <ErrorState error={query.error} retry={() => void query.refetch()} />
        ) : (
          <>
            <ExpenseHistory
              expenses={query.data.content}
              vehicleId={vehicle.id}
              onDelete={(expense) => {
                deletion.reset();
                setDeleting(expense);
              }}
            />
            {query.data.content.length > 0 && <div className="pagination">
              <span>
                Strona {query.data.totalPages ? page + 1 : 0} z{" "}
                {query.data.totalPages}
              </span>
              <div>
                <button
                  className="button button-secondary"
                  disabled={page === 0}
                  onClick={() => update({ page: page - 1 })}
                >
                  <ChevronLeft size={16} />
                  Poprzednia
                </button>
                <button
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
          title="Usunąć wydatek?"
          description={
            <>
              Wydatek „{categoryLabel(deleting.category)}” z{" "}
              {dateLabel(deleting.expenseDate)} w wysokości{" "}
              {number(deleting.amount)} {deleting.currency} zostanie trwale
              usunięty. Tej operacji nie można cofnąć.
            </>
          }
          confirmLabel="Usuń wydatek"
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
              },
            });
          }}
        />
      )}
    </>
  );
}
