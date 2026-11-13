import { MonthlyChart } from "../../components/charts/MonthlyChart";
import { number } from "../../utils/format";
import { Fuel, Gauge } from "lucide-react";
import { ErrorState, Loading, Stat } from "../../components/ui";
import { useCosts } from "./api";

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
        Koszty paliwa nie mają przypisanej waluty. Pozostałe wydatki są
        prezentowane osobno dla każdej waluty. Kwoty nie są łączone ani
        przeliczane.
      </p>
      <div className="cost-grid">
        <Stat
          label="Łączny koszt tankowań"
          icon={<Fuel size={19} />}
          value={number(fuel.totalCost)}
          note={`Bez określonej waluty · ${number(fuel.fuelEntryCount)} tankowań`}
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
      </div>
      <section className="panel">
        <div className="section-heading">
          <div>
            <h2>Paliwo — koszty miesięczne</h2>
            <p>Kwoty bez określonej waluty</p>
          </div>
        </div>
        <div className="report-body">
          {fuel.hasFuelData ? (
            <MonthlyChart
              rows={fuel.monthlyTotals.map((row) => ({
                ...row,
                amount: row.totalCost,
              }))}
              label="Koszt paliwa (bez waluty)"
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
              {number(otherExpenses.expenseCount)} wpisów · według walut i
              kategorii
            </p>
          </div>
        </div>
        <div className="report-body">
          {!otherExpenses.totalsByCurrency.length ? (
            <p>Ten pojazd nie ma jeszcze zarejestrowanych wydatków.</p>
          ) : (
            otherExpenses.totalsByCurrency.map((total) => (
              <section key={total.currency}>
                <div className="currency-total">
                  <strong>{total.currency}</strong>
                  <strong>
                    {number(total.totalAmount)} {total.currency}
                  </strong>
                </div>
                <h3 className="chart-title">
                  Wydatki według kategorii · {total.currency}
                </h3>
                <ul className="category-list">
                  {otherExpenses.totalsByCategory
                    .filter((row) => row.currency === total.currency)
                    .map((row) => (
                      <li key={row.category}>
                        <span>
                          {categories[row.category] || row.category}{" "}
                          <span className="muted">({row.count})</span>
                        </span>
                        <strong>
                          {number(row.totalAmount)} {row.currency}
                        </strong>
                      </li>
                    ))}
                </ul>
                <h3 className="chart-title">
                  Koszty miesięczne · {total.currency}
                </h3>
                <MonthlyChart
                  rows={otherExpenses.monthlyTotals
                    .filter((row) => row.currency === total.currency)
                    .map((row) => ({ ...row, amount: row.totalAmount }))}
                  label={`Wydatki (${total.currency})`}
                />
              </section>
            ))
          )}
        </div>
      </section>
    </>
  );
}
