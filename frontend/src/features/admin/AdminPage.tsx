import { useState } from "react";
import {
  Activity,
  CarFront,
  Fuel,
  ReceiptText,
  ShieldCheck,
  Users,
  Wrench,
} from "lucide-react";
import { useQueryClient } from "@tanstack/react-query";
import { ConfirmDialog } from "../../components/ConfirmDialog";
import { ErrorState, Loading, PageHeader, Stat } from "../../components/ui";
import { toApiError } from "../../api/errors";
import { useAuth } from "../auth/useAuth";
import {
  useAdminDashboard,
  useAdminUsers,
  useChangeAdminRole,
  useChangeAdminStatus,
} from "./api";
import type { AdminUser, AdminUserFilters } from "./types";
import { number } from "../../utils/format";

type Action =
  | { kind: "role"; user: AdminUser }
  | { kind: "status"; user: AdminUser; enabled: boolean };

const emptyFilters: AdminUserFilters = { search: "", role: "", enabled: undefined };

function AdminDashboard() {
  const query = useAdminDashboard();
  if (query.isPending) return <Loading rows={4} />;
  if (query.isError)
    return <ErrorState error={query.error} retry={() => void query.refetch()} />;

  const data = query.data;
  return (
    <section aria-label="Statystyki systemu" className="admin-stats-grid">
      <Stat label="Użytkownicy" value={number(data.totalUsers)} icon={<Users size={18} />} />
      <Stat label="Aktywne konta" value={number(data.activeUsers)} icon={<Activity size={18} />} />
      <Stat label="Nieaktywne konta" value={number(data.inactiveUsers)} icon={<Users size={18} />} />
      <Stat label="Administratorzy" value={number(data.adminUsers)} icon={<ShieldCheck size={18} />} />
      <Stat label="Pojazdy" value={number(data.totalVehicles)} icon={<CarFront size={18} />} />
      <Stat label="Tankowania" value={number(data.totalFuelEntries)} icon={<Fuel size={18} />} />
      <Stat label="Wydatki" value={number(data.totalExpenses)} icon={<ReceiptText size={18} />} />
      <Stat label="Przypomnienia" value={number(data.totalReminders)} icon={<Wrench size={18} />} />
    </section>
  );
}

export function AdminPage() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [filters, setFilters] = useState<AdminUserFilters>(emptyFilters);
  const [action, setAction] = useState<Action | null>(null);
  const [actionError, setActionError] = useState<Error | null>(null);
  const usersQuery = useAdminUsers(page, filters);
  const roleMutation = useChangeAdminRole();
  const statusMutation = useChangeAdminStatus();
  const pending = roleMutation.isPending || statusMutation.isPending;

  const updateFilter = <K extends keyof AdminUserFilters>(
    key: K,
    value: AdminUserFilters[K],
  ) => {
    setPage(0);
    setFilters((current) => ({ ...current, [key]: value }));
  };

  const confirmAction = async () => {
    if (!action) return;
    setActionError(null);
    try {
      if (action.kind === "role") {
        await roleMutation.mutateAsync({
          userId: action.user.id,
          role: action.user.role === "ADMIN" ? "USER" : "ADMIN",
        });
      } else {
        await statusMutation.mutateAsync({
          userId: action.user.id,
          enabled: action.enabled,
        });
      }
      setAction(null);
      await queryClient.invalidateQueries({ queryKey: ["auth", "me"] });
    } catch (error) {
      setActionError(toApiError(error));
    }
  };

  const openAction = (next: Action) => {
    roleMutation.reset();
    statusMutation.reset();
    setActionError(null);
    setAction(next);
  };

  const actionTitle = action?.kind === "role" ? "Zmienić rolę użytkownika?" :
    action?.enabled ? "Aktywować konto?" : "Dezaktywować konto?";
  const actionDescription = action?.kind === "role"
    ? `Rola konta ${action.user.email} zostanie zmieniona na ${action.user.role === "ADMIN" ? "USER" : "ADMIN"}.`
    : `${action?.user.email} ${action?.enabled ? "odzyska dostęp do konta" : "utraci dostęp do API, a jego sesje zostaną unieważnione"}.`;
  const confirmLabel = action?.kind === "role"
    ? "Zmień rolę"
    : action?.enabled ? "Aktywuj konto" : "Dezaktywuj konto";

  return (
    <div className="admin-page">
      <PageHeader
        eyebrow="ADMINISTRACJA"
        title="Administracja"
        description="Zarządzaj kontami i przeglądaj zagregowane statystyki RouteCost."
      />

      <AdminDashboard />

      <section className="panel admin-users-panel" aria-labelledby="admin-users-title">
        <div className="section-heading">
          <div>
            <h2 id="admin-users-title">Użytkownicy</h2>
            <p>Zmiany uprawnień i statusu kont wymagają potwierdzenia.</p>
          </div>
        </div>

        <div className="admin-filters">
          <label className="form-field">
            <span>Szukaj użytkowników</span>
            <input
              type="search"
              value={filters.search}
              placeholder="E-mail, imię lub nazwisko"
              onChange={(event) => updateFilter("search", event.target.value)}
            />
          </label>
          <label className="form-field">
            <span>Rola</span>
            <select
              value={filters.role}
              onChange={(event) => updateFilter("role", event.target.value as AdminUserFilters["role"])}
            >
              <option value="">Wszystkie role</option>
              <option value="USER">USER</option>
              <option value="ADMIN">ADMIN</option>
            </select>
          </label>
          <label className="form-field">
            <span>Status konta</span>
            <select
              value={filters.enabled === undefined ? "" : String(filters.enabled)}
              onChange={(event) => updateFilter(
                "enabled",
                event.target.value === "" ? undefined : event.target.value === "true",
              )}
            >
              <option value="">Wszystkie statusy</option>
              <option value="true">Aktywne</option>
              <option value="false">Nieaktywne</option>
            </select>
          </label>
        </div>

        {usersQuery.isPending ? (
          <Loading rows={5} />
        ) : usersQuery.isError ? (
          <ErrorState error={usersQuery.error} retry={() => void usersQuery.refetch()} />
        ) : usersQuery.data.content.length === 0 ? (
          <div className="empty-state admin-empty-state">
            <span className="empty-icon"><Users size={26} /></span>
            <h3>Brak użytkowników</h3>
            <p>Zmień wyszukiwanie lub filtry, aby zobaczyć inne konta.</p>
          </div>
        ) : (
          <>
            <div className="table-scroll">
              <table className="data-table admin-users-table">
                <caption className="sr-only">Lista kont użytkowników RouteCost</caption>
                <thead>
                  <tr>
                    <th>Użytkownik</th>
                    <th>Rola</th>
                    <th>Status</th>
                    <th>Utworzono</th>
                    <th><span className="sr-only">Operacje</span></th>
                  </tr>
                </thead>
                <tbody>
                  {usersQuery.data.content.map((row) => {
                    const isSelf = row.id === user?.id;
                    return (
                      <tr key={row.id}>
                        <td data-label="Użytkownik">
                          <strong>{row.firstName} {row.lastName}</strong>
                          <span className="admin-user-email">{row.email}</span>
                        </td>
                        <td data-label="Rola"><span className="badge">{row.role}</span></td>
                        <td data-label="Status">
                          <span className={`badge ${row.enabled ? "admin-status-active" : "admin-status-inactive"}`}>
                            {row.enabled ? "Aktywne" : "Nieaktywne"}
                          </span>
                        </td>
                        <td data-label="Utworzono">{new Intl.DateTimeFormat("pl-PL").format(new Date(row.createdAt))}</td>
                        <td data-label="Operacje">
                          <div className="admin-row-actions">
                            <button
                              className="button button-secondary"
                              disabled={isSelf && row.role === "ADMIN"}
                              title={isSelf ? "Nie możesz zmienić własnej roli administratora" : undefined}
                              onClick={() => openAction({ kind: "role", user: row })}
                            >
                              {row.role === "ADMIN" ? "Zmień na USER" : "Nadaj ADMIN"}
                            </button>
                            <button
                              className={`button ${row.enabled ? "button-danger" : "button-secondary"}`}
                              disabled={isSelf && row.enabled}
                              title={isSelf && row.enabled ? "Nie możesz dezaktywować własnego konta" : undefined}
                              onClick={() => openAction({ kind: "status", user: row, enabled: !row.enabled })}
                            >
                              {row.enabled ? "Dezaktywuj" : "Aktywuj"}
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
            <div className="pagination admin-pagination">
              <span>
                {number(usersQuery.data.totalElements)} kont · strona {usersQuery.data.page + 1}
                {usersQuery.data.totalPages > 0 ? ` z ${usersQuery.data.totalPages}` : ""}
              </span>
              <div>
                <button
                  className="button button-secondary"
                  disabled={page <= 0}
                  onClick={() => setPage((current) => Math.max(0, current - 1))}
                >
                  Poprzednia
                </button>
                <button
                  className="button button-secondary"
                  disabled={page + 1 >= usersQuery.data.totalPages}
                  onClick={() => setPage((current) => current + 1)}
                >
                  Następna
                </button>
              </div>
            </div>
          </>
        )}
      </section>

      {action && (
        <ConfirmDialog
          title={actionTitle ?? "Potwierdź zmianę"}
          description={actionDescription}
          confirmLabel={confirmLabel ?? "Potwierdź"}
          pending={pending}
          error={actionError}
          close={() => { if (!pending) setAction(null); }}
          confirm={() => void confirmAction()}
        />
      )}
    </div>
  );
}
