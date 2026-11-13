import { AlertCircle, CarFront, Plus } from "lucide-react";
import { Link } from "react-router-dom";
import type { ReactNode } from "react";

export function PageHeader({
  eyebrow,
  title,
  description,
  children,
}: {
  eyebrow?: string;
  title: string;
  description: string;
  children?: ReactNode;
}) {
  return (
    <header className="page-heading">
      <div>
        {eyebrow && <p className="eyebrow">{eyebrow}</p>}
        <h1>{title}</h1>
        <p>{description}</p>
      </div>
      <div className="heading-actions">{children}</div>
    </header>
  );
}
export function AddVehicle() {
  return (
    <Link className="button button-primary" to="/vehicles/new">
      <Plus size={17} />
      Dodaj pojazd
    </Link>
  );
}
export function ErrorState({
  error,
  retry,
}: {
  error: Error | null;
  retry?: () => void;
}) {
  return (
    <div className="feedback feedback-error" role="alert">
      <AlertCircle size={20} />
      <div>
        <strong>Nie udało się wykonać operacji</strong>
        <p>{error?.message || "Spróbuj ponownie za chwilę."}</p>
        {retry && (
          <button className="button button-secondary" onClick={retry}>
            Spróbuj ponownie
          </button>
        )}
      </div>
    </div>
  );
}
export function Loading({ rows = 3 }: { rows?: number }) {
  return (
    <div className="loading" role="status" aria-label="Ładowanie danych">
      {Array.from({ length: rows }, (_, i) => (
        <div className="skeleton" key={i} />
      ))}
      <span className="sr-only">Ładowanie danych…</span>
    </div>
  );
}
export function EmptyState({ filtered = false }: { filtered?: boolean }) {
  return (
    <div className="empty-state">
      <span className="empty-icon">
        <CarFront size={28} />
      </span>
      <h3>
        {filtered
          ? "Brak pojazdów z tym paliwem"
          : "Tutaj zaczyna się Twoja historia"}
      </h3>
      <p>
        {filtered
          ? "Wybierz inny rodzaj paliwa lub pokaż wszystkie pojazdy."
          : "Dodaj pierwszy pojazd, aby mieć jego dane i koszty w jednym miejscu."}
      </p>
      {!filtered && <AddVehicle />}
    </div>
  );
}
export function Stat({
  label,
  value,
  note,
  icon,
  warning = false,
}: {
  label: string;
  value: ReactNode;
  note?: string;
  icon: ReactNode;
  warning?: boolean;
}) {
  return (
    <div className={`stat ${warning ? "stat-warning" : ""}`}>
      <div className="stat-label">
        {label}
        {icon}
      </div>
      <strong>{value}</strong>
      {note && <span className="stat-note">{note}</span>}
    </div>
  );
}
