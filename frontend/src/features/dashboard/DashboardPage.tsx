import { number } from "../../utils/format";
import {
  Bell,
  CarFront,
  Fuel,
  ReceiptText,
  ArrowRight,
  Clock3,
} from "lucide-react";
import { Link } from "react-router-dom";
import { useAuth } from "../auth/useAuth";
import { useVehicles } from "../vehicles/api";
import { useSummary } from "./api";
import { VehicleTable } from "../vehicles/VehicleTable";
import {
  AddVehicle,
  ErrorState,
  Loading,
  PageHeader,
  Stat,
} from "../../components/ui";
export function DashboardPage() {
  const { user } = useAuth();
  const summary = useSummary();
  const vehicles = useVehicles(0, "", 5);
  return (
    <>
      <PageHeader
        eyebrow="PRZEGLĄD KONTA"
        title={`Witaj, ${user?.firstName}.`}
        description="Twoje pojazdy i najważniejsze liczby. Wszystko pod kontrolą."
      >
        <AddVehicle />
      </PageHeader>
      {summary.isPending ? (
        <Loading rows={2} />
      ) : summary.isError ? (
        <ErrorState
          error={summary.error}
          retry={() => void summary.refetch()}
        />
      ) : (
        <>
          <section className="stats-grid" aria-label="Podsumowanie konta">
            <Stat
              label="Pojazdy"
              value={number(summary.data.vehicleCount)}
              note="W Twoim garażu"
              icon={<CarFront size={19} />}
            />
            <Stat
              label="Tankowania"
              value={number(summary.data.fuelEntryCount)}
              note="Wszystkie wpisy"
              icon={<Fuel size={19} />}
            />
            <Stat
              label="Wydatki"
              value={number(summary.data.expenseCount)}
              note="Zarejestrowane operacje"
              icon={<ReceiptText size={19} />}
            />
            <Stat
              label="Aktywne przypomnienia"
              value={number(summary.data.activeReminderCount)}
              note="Do zrealizowania"
              icon={<Bell size={19} />}
            />
          </section>
          <div
            className={`reminder-strip ${summary.data.overdueReminderCount > 0 ? "is-warning" : ""}`}
          >
            <Clock3 size={19} />
            <div>
              <strong>
                Zaległe przypomnienia:{" "}
                {number(summary.data.overdueReminderCount)}
              </strong>
              <span>
                {summary.data.overdueReminderCount
                  ? "Na Twoim koncie są terminy, które już minęły."
                  : "Żaden zaległy termin nie wymaga Twojej uwagi."}
              </span>
            </div>
            <span className="badge">Podgląd</span>
          </div>
        </>
      )}
      <section className="panel">
        <div className="section-heading">
          <div>
            <h2>Twoje pojazdy</h2>
            <p>Ostatnio dodane. Wybierz pojazd, aby sprawdzić jego koszty.</p>
          </div>
          <Link className="text-link" to="/vehicles">
            Wszystkie pojazdy <ArrowRight size={16} />
          </Link>
        </div>
        {vehicles.isPending ? (
          <Loading />
        ) : vehicles.isError ? (
          <ErrorState
            error={vehicles.error}
            retry={() => void vehicles.refetch()}
          />
        ) : (
          <VehicleTable vehicles={vehicles.data.content} />
        )}
      </section>
      <div className="dashboard-note">
        <ReceiptText size={21} />
        <div>
          <h3>Koszty z właściwej perspektywy</h3>
          <p>
            Raport każdego pojazdu pokazuje paliwo i pozostałe wydatki osobno, z
            zachowaniem podziału na waluty.
          </p>
        </div>
        <span className="badge">W szczegółach pojazdu</span>
      </div>
    </>
  );
}
