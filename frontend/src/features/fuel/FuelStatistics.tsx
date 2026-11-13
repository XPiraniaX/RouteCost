import { Fuel, Gauge, ReceiptText, Droplets, Activity } from "lucide-react";
import { ErrorState, Loading, Stat } from "../../components/ui";
import { number } from "../../utils/format";
import { useFuelStatistics } from "./api";

export function FuelStatistics({ id }: { id: number }) {
  const query = useFuelStatistics(id);
  if (query.isPending) return <Loading rows={2} />;
  if (query.isError)
    return (
      <ErrorState error={query.error} retry={() => void query.refetch()} />
    );
  const data = query.data;
  return (
    <>
      <section
        className="stats-grid fuel-stats"
        aria-label="Statystyki tankowań"
      >
        <Stat
          label="Średnie spalanie"
          icon={<Gauge size={19} />}
          value={
            data.averageConsumptionLitersPer100Km === null
              ? "Brak danych"
              : `${number(data.averageConsumptionLitersPer100Km)} l/100 km`
          }
          note={
            data.averageConsumptionLitersPer100Km === null
              ? "Potrzebne są poprawne odcinki między tankowaniami do pełna."
              : "Średnia ważona dystansem · cała historia"
          }
        />
        <Stat
          label="Tankowania"
          icon={<Fuel size={19} />}
          value={number(data.fuelEntryCount)}
          note="Wszystkie wpisy pojazdu"
        />
        <Stat
          label="Łącznie paliwa"
          icon={<Droplets size={19} />}
          value={`${new Intl.NumberFormat("pl-PL", { maximumFractionDigits: 3 }).format(data.totalLiters)} l`}
          note="Suma wszystkich tankowań"
        />
        <Stat
          label="Łączny koszt"
          icon={<ReceiptText size={19} />}
          value={number(data.totalCost)}
          note="Bez określonej waluty"
        />
      </section>
      <div className="reminder-strip">
        <Activity size={19} />
        <div>
          <strong>
            Poprawne interwały spalania:{" "}
            {number(data.validConsumptionIntervals)}
          </strong>
          <span>
            Tankowanie otwierające odcinek nie wlicza się do jego zużycia.
            Tankowania częściowe pomiędzy pełnymi są uwzględniane.
          </span>
        </div>
      </div>
    </>
  );
}
