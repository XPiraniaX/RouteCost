import { lazy, Suspense } from "react";
import { CalendarDays, ReceiptText, Tags, TrendingUp } from "lucide-react";
import { ErrorState, Loading, Stat } from "../../components/ui";
import { number } from "../../utils/format";
import { useExpenseStatistics } from "./api";
import { categoryLabel } from "./types";
import { averageMonthlyExpense } from "./monthlyAverage";

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
  return (
    <>
      <section className="stats-grid expense-stats" aria-label="Statystyki wydatków">
        <Stat
          label="Wydatki"
          value={number(data.expenseCount)}
          note="Cała historia pojazdu"
          icon={<ReceiptText size={19} />}
        />
        <Stat
          label="Kategorie"
          value={number(new Set(data.totalsByCategory.map((row) => row.category)).size)}
          note="Kategorie z zapisanymi wydatkami"
          icon={<Tags size={19} />}
        />
        <Stat
          label="Łączna kwota"
          value={`${number(data.totalAmount)} PLN`}
          note="Cała historia wydatków"
          icon={<CalendarDays size={19} />}
        />
        <Stat
          label="Średni miesięczny koszt"
          value={`${number(averageMonthlyExpense(data.totalAmount, data.monthlyTotals))} PLN`}
          note="Od pierwszego wydatku do dziś"
          icon={<TrendingUp size={19} />}
        />
      </section>
      {data.expenseCount > 0 && (
        <section className="panel">
          <div className="section-heading">
            <div>
              <h2>Wydatki według kategorii</h2>
              <p>Wszystkie kwoty w PLN.</p>
            </div>
          </div>
          <div className="table-scroll">
            <table className="data-table expense-table">
              <thead>
                <tr>
                  <th>Kategoria</th>
                  <th className="numeric">Suma (PLN)</th>
                  <th className="numeric">Liczba</th>
                </tr>
              </thead>
              <tbody>
                {data.totalsByCategory.map((row) => (
                  <tr key={row.category}>
                    <td>{categoryLabel(row.category)}</td>
                    <td className="numeric">{number(row.totalAmount)} PLN</td>
                    <td className="numeric">{number(row.count)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}
      {data.monthlyTotals.length > 0 && (
        <section className="panel">
          <div className="section-heading">
            <div>
              <h2>Koszty miesięczne · PLN</h2>
              <p>Cała historia pojazdu.</p>
            </div>
          </div>
          <Suspense fallback={<Loading rows={2} />}>
            <MonthlyChart
              rows={data.monthlyTotals.map(({ year, month, totalAmount }) => ({
                year,
                month,
                amount: totalAmount,
              }))}
              label="Wydatki (PLN)"
            />
          </Suspense>
        </section>
      )}
      {data.expenseCount === 0 && (
        <section className="panel expense-stat-empty">
          <p>Statystyki i wykresy pojawią się po dodaniu pierwszego wydatku.</p>
        </section>
      )}
    </>
  );
}
