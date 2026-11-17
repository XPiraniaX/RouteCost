import { lazy, Suspense } from "react";
import { CalendarDays, ReceiptText, Tags } from "lucide-react";
import { ErrorState, Loading, Stat } from "../../components/ui";
import { number } from "../../utils/format";
import { useExpenseStatistics } from "./api";
import { categoryLabel } from "./types";
import { polishPlural } from "../../utils/polishPlural";

const MonthlyChart = lazy(() =>
  import("../../components/charts/MonthlyChart").then((module) => ({
    default: module.MonthlyChart,
  })),
);

export function ExpenseStatistics({ id }: { id: number }) {
  const query = useExpenseStatistics(id);
  if (query.isPending) return <Loading rows={2} />;
  if (query.isError)
    return (
      <ErrorState error={query.error} retry={() => void query.refetch()} />
    );

  const data = query.data;
  const currencies = [
    ...new Set([
      ...data.totalsByCurrency.map((row) => row.currency),
      ...data.monthlyTotals.map((row) => row.currency),
    ]),
  ].sort();
  return (
    <>
      <section
        className="stats-grid expense-stats"
        aria-label="Statystyki wydatków"
      >
        <Stat
          label="Wydatki"
          value={number(data.expenseCount)}
          note="Cała historia pojazdu"
          icon={<ReceiptText size={19} />}
        />
        <Stat
          label="Kategorie"
          value={number(
            new Set(data.totalsByCategory.map((row) => row.category)).size,
          )}
          note="Kategorie z zapisanymi wydatkami"
          icon={<Tags size={19} />}
        />
        <Stat
          label="Waluty"
          value={number(data.totalsByCurrency.length)}
          note="Każda waluta podsumowana osobno"
          icon={<CalendarDays size={19} />}
        />
      </section>
      {data.totalsByCurrency.length > 0 && (
        <section className="panel expense-currency-panel">
          <div className="section-heading">
            <div>
              <h2>Łączne wydatki</h2>
              <p>Kwoty z różnych walut nie są sumowane.</p>
            </div>
          </div>
          <div className="expense-currency-grid">
            {data.totalsByCurrency.map((row) => (
              <div className="expense-currency-total" key={row.currency}>
                <span>{row.currency}</span>
                <strong>
                  {number(row.totalAmount)} {row.currency}
                </strong>
                <small>
                  {number(row.count)} {polishPlural(row.count, ["wpis", "wpisy", "wpisów"])}
                </small>
              </div>
            ))}
          </div>
        </section>
      )}
      {data.expenseCount > 0 && (
        <section className="panel">
          <div className="section-heading">
            <div>
              <h2>Wydatki według kategorii</h2>
              <p>Rozbicie kategorii z zachowaniem walut.</p>
            </div>
          </div>
          <div className="table-scroll">
            <table className="data-table expense-table">
              <thead>
                <tr>
                  <th>Kategoria</th>
                  <th>Waluta</th>
                  <th className="numeric">Suma</th>
                  <th className="numeric">Liczba</th>
                </tr>
              </thead>
              <tbody>
                {data.totalsByCategory.map((row) => (
                  <tr key={`${row.category}-${row.currency}`}>
                    <td>{categoryLabel(row.category)}</td>
                    <td>{row.currency}</td>
                    <td className="numeric">
                      {number(row.totalAmount)} {row.currency}
                    </td>
                    <td className="numeric">{number(row.count)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}
      {currencies.length > 0 && (
        <div className="expense-charts">
          {currencies.map((currency) => {
            const rows = data.monthlyTotals.filter(
              (row) => row.currency === currency,
            );
            if (!rows.length) return null;
            return (
              <section className="panel" key={currency}>
                <div className="section-heading">
                  <div>
                    <h2>Koszty miesięczne · {currency}</h2>
                    <p>Cała historia pojazdu · wyłącznie {currency}</p>
                  </div>
                </div>
                <Suspense fallback={<Loading rows={2} />}>
                  <MonthlyChart
                    rows={rows.map(({ year, month, totalAmount }) => ({
                      year,
                      month,
                      amount: totalAmount,
                    }))}
                    label={`Wydatki (${currency})`}
                  />
                </Suspense>
              </section>
            );
          })}
        </div>
      )}
      {data.expenseCount === 0 && (
        <section className="panel expense-stat-empty">
          <p>Statystyki i wykresy pojawią się po dodaniu pierwszego wydatku.</p>
        </section>
      )}
    </>
  );
}
