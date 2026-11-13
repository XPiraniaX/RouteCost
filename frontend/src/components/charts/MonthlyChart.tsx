import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { number } from "../../utils/format";
const monthLabel = (year: number, month: number) =>
  `${String(month).padStart(2, "0")}.${year}`;
export function MonthlyChart({
  rows,
  label,
}: {
  rows: { year: number; month: number; amount: number }[];
  label: string;
}) {
  const data = [...rows]
    .sort((a, b) => a.year - b.year || a.month - b.month)
    .map((row) => ({
      month: monthLabel(row.year, row.month),
      amount: row.amount,
    }));
  if (!data.length) return <p>Brak danych do zestawienia miesięcznego.</p>;
  return (
    <>
      <div className="chart" role="img" aria-label={label}>
        <ResponsiveContainer width="100%" height="100%">
          <BarChart
            data={data}
            margin={{ top: 8, right: 8, bottom: 4, left: 8 }}
          >
            <CartesianGrid stroke="var(--color-border)" vertical={false} />
            <XAxis
              dataKey="month"
              tick={{ fill: "var(--color-muted)", fontSize: 11 }}
              axisLine={false}
              tickLine={false}
            />
            <YAxis
              tickFormatter={number}
              width={72}
              tick={{ fill: "var(--color-muted)", fontSize: 11 }}
              axisLine={false}
              tickLine={false}
            />
            <Tooltip
              formatter={(value) => [number(Number(value)), label]}
              contentStyle={{
                background: "var(--color-surface)",
                border: "var(--border)",
                borderRadius: 6,
                color: "var(--color-text)",
              }}
              cursor={{ fill: "#ffffff08" }}
            />
            <Bar
              isAnimationActive={false}
              dataKey="amount"
              fill="var(--color-accent)"
              maxBarSize={40}
              radius={[4, 4, 0, 0]}
            />
          </BarChart>
        </ResponsiveContainer>
      </div>
      <details className="chart-data">
        <summary>Pokaż dane miesięczne</summary>
        <div className="table-scroll">
          <table className="data-table">
            <caption className="sr-only">{label}</caption>
            <thead>
              <tr>
                <th>Miesiąc</th>
                <th className="numeric">{label}</th>
              </tr>
            </thead>
            <tbody>
              {data.map((row) => (
                <tr key={row.month}>
                  <td>{row.month}</td>
                  <td className="numeric">{number(row.amount)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </details>
    </>
  );
}
