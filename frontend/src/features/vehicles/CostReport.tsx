import { MonthlyChart } from "../../components/charts/MonthlyChart";
import { number } from "../../utils/format";
import { Fuel, Gauge, ReceiptText, CalendarRange } from "lucide-react";
import { ErrorState, Loading, Stat } from "../../components/ui";
import { useCosts } from "./api";
import { polishPlural } from "../../utils/polishPlural";

const categories: Record<string, string> = {
  REPAIR: "Naprawy",
  PARTS: "Części",
  SERVICE: "Serwis",
  INSURANCE: "Ubezpieczenie",
  TECHNICAL_INSPECTION: "Przegląd techniczny",
  TIRES: "Opony",
  CAR_WASH: "Myjnia",
  OTHER: "Inne",
};
export function CostReport({ id }: { id: number }) {
  const query = useCosts(id);
  if (query.isPending) return <Loading />;
  if (query.isError)
    return (
      <ErrorState error={query.error} retry={() => void query.refetch()} />
    );
  const { fuel, otherExpenses } = query.data;
  return (
    <>
      <div className="section-heading report-heading">
        <div>
          <h2>Raport kosztów</h2>
          <p>Cała historia pojazdu. Dane zarejestrowane na Twoim koncie.</p>
        </div>
      </div>
      <p className="report-note">
        Koszty tankowań i wydatków oraz średnie są prezentowane w PLN. Cena
        zakupu pojazdu nie jest wliczana do kosztów utrzymania.
      </p>
      <div className="cost-grid cost-summary-grid">
        <Stat
          label="Łączny koszt utrzymania pojazdu"
          icon={<ReceiptText size={19} />}
          value={`${number(query.data.totalMaintenanceCost)} PLN`}
          note="Tankowania i wydatki · cała historia, bez ceny zakupu"
        />
        <Stat
          label="Średnie spalanie"
          icon={<Gauge size={19} />}
          value={
            fuel.averageConsumptionAvailable &&
            fuel.averageConsumptionLitersPer100Km !== null
              ? `${number(fuel.averageConsumptionLitersPer100Km)} l/100 km`
              : "Brak danych"
          }
          note={
            fuel.averageConsumptionAvailable
              ? `Obliczono na podstawie ${fuel.validConsumptionIntervals} poprawnych odcinków`
              : "Brak wystarczających danych do obliczenia spalania."
          }
        />
        <Stat
          label="Średni miesięczny koszt utrzymania"
          icon={<CalendarRange size={19} />}
          value={
            query.data.hasMaintenanceHistory &&
            query.data.averageMonthlyMaintenanceCost !== null
              ? `${number(query.data.averageMonthlyMaintenanceCost)} PLN`
              : "Brak danych"
          }
          note={
            query.data.hasMaintenanceHistory
              ? `Średnia za ${number(query.data.maintenanceHistoryMonthCount)} ${polishPlural(query.data.maintenanceHistoryMonthCount, ["miesiąc kalendarzowy", "miesiące kalendarzowe", "miesięcy kalendarzowych"])}`
              : "Dodaj tankowanie lub wydatek, aby obliczyć średnią."
          }
        />
        <Stat
          label="Łączny koszt tankowań"
          icon={<Fuel size={19} />}
          value={`${number(fuel.totalCost)} PLN`}
          note={`${number(fuel.fuelEntryCount)} ${polishPlural(fuel.fuelEntryCount, ["tankowanie", "tankowania", "tankowań"])}`}
        />
      </div>
      <section className="panel">
        <div className="section-heading">
          <div>
            <h2>Paliwo — koszty miesięczne</h2>
            <p>Kwoty w PLN</p>
          </div>
        </div>
        <div className="report-body">
          {fuel.hasFuelData ? (
            <MonthlyChart
              rows={fuel.monthlyTotals.map((row) => ({
                ...row,
                amount: row.totalCost,
              }))}
              label="Koszt paliwa (PLN)"
            />
          ) : (
            <p>Ten pojazd nie ma jeszcze zarejestrowanych tankowań.</p>
          )}
        </div>
      </section>
      <section className="panel">
        <div className="section-heading">
          <div>
            <h2>Pozostałe wydatki</h2>
            <p>
              {number(otherExpenses.expenseCount)} {polishPlural(otherExpenses.expenseCount, ["wpis", "wpisy", "wpisów"])} · wszystkie kwoty w PLN
            </p>
          </div>
        </div>
        <div className="report-body">
          {!otherExpenses.expenseCount ? (
            <p>Ten pojazd nie ma jeszcze zarejestrowanych wydatków.</p>
          ) : (
            <section>
                <div className="cost-total">
                  <strong>
                    {number(otherExpenses.totalAmount)} PLN
                  </strong>
                </div>
                <h3 className="chart-title">
                  Wydatki według kategorii
                </h3>
                <ul className="category-list">
                  {otherExpenses.totalsByCategory.map((row) => (
                      <li key={row.category}>
                        <span>
                          {categories[row.category] || row.category}{" "}
                          <span className="muted">({row.count})</span>
                        </span>
                        <strong>
                          {number(row.totalAmount)} PLN
                        </strong>
                      </li>
                    ))}
                </ul>
                <h3 className="chart-title">
                  Koszty miesięczne · PLN
                </h3>
                <MonthlyChart
                  rows={otherExpenses.monthlyTotals.map((row) => ({ ...row, amount: row.totalAmount }))}
                  label="Wydatki (PLN)"
                />
            </section>
          )}
        </div>
      </section>
    </>
  );
}
