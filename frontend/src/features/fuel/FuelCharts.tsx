import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { MonthlyChart } from "../../components/charts/MonthlyChart";
import { ErrorState, Loading } from "../../components/ui";
import { dateLabel } from "../../utils/date";
import { number } from "../../utils/format";
import { useCosts } from "../vehicles/api";
import type { FuelEntry } from "./api";

export function FuelCharts({
  id,
  entries,
}: {
  id: number;
  entries: FuelEntry[];
}) {
  const costs = useCosts(id);
  const data = [...entries].sort(
    (a, b) => a.entryDate.localeCompare(b.entryDate) || a.id - b.id,
  );
  const hasConsumption = data.some(
    (entry) => entry.consumptionFromPreviousFullTank !== null,
  );
  return (
    <div className="fuel-charts">
      <section className="panel">
        <div className="section-heading">
          <div>
            <h2>Spalanie na odcinkach</h2>
            <p>
              Średnie l/100 km na odcinkach widocznych na bieżącej stronie
              historii
            </p>
          </div>
        </div>
        <div className="report-body">
          {!hasConsumption ? (
            <p>
              Brak danych spalania dla wpisów na tej stronie. Potrzebne są co
              najmniej dwa tankowania do pełna z rosnącym przebiegiem.
            </p>
          ) : (
            <>
              <div
                className="chart"
                role="img"
                aria-label="Średnie spalanie na odcinkach z bieżącej strony"
              >
                <ResponsiveContainer width="100%" height="100%">
                  <LineChart
                    data={data}
                    margin={{ top: 12, right: 16, bottom: 8, left: 0 }}
                  >
                    <CartesianGrid
                      stroke="var(--color-border)"
                      vertical={false}
                    />
                    <XAxis
                      dataKey="id"
                      type="category"
                      tickFormatter={(id) => {
                        const entry = data.find((row) => row.id === id);
                        return entry ? dateLabel(entry.entryDate) : "";
                      }}
                      tick={{ fill: "var(--color-muted)", fontSize: 10 }}
                      tickLine={false}
                    />
                    <YAxis
                      label={{
                        value: "l/100 km",
                        angle: -90,
                        position: "insideLeft",
                        fill: "var(--color-muted)",
                        fontSize: 10,
                      }}
                      width={60}
                      tick={{ fill: "var(--color-muted)", fontSize: 11 }}
                      tickLine={false}
                    />
                    <Tooltip
                      labelFormatter={(id) => {
                        const entry = data.find((row) => row.id === Number(id));
                        return entry
                          ? `${dateLabel(entry.entryDate)} · ${number(entry.odometer)} km`
                          : "";
                      }}
                      formatter={(value) => [
                        `${number(Number(value))} l/100 km`,
                        "Spalanie odcinka",
                      ]}
                      contentStyle={{
                        background: "var(--color-surface)",
                        border: "var(--border)",
                        color: "var(--color-text)",
                        borderRadius: 6,
                      }}
                    />
                    <Line
                      type="linear"
                      dataKey="consumptionFromPreviousFullTank"
                      stroke="var(--color-accent)"
                      strokeWidth={2}
                      dot={{ r: 4 }}
                      connectNulls={false}
                      isAnimationActive={false}
                    />
                  </LineChart>
                </ResponsiveContainer>
              </div>
              <p>
                Każdy punkt pochodzi z obliczeń backendu. Brakujące wartości
                pozostają przerwami. Dane liczbowe są dostępne w historii
                powyżej.
              </p>
            </>
          )}
        </div>
      </section>
      <section className="panel">
        <div className="section-heading">
          <div>
            <h2>Koszty miesięczne</h2>
            <p>Cała historia pojazdu · kwoty w PLN</p>
          </div>
        </div>
        <div className="report-body">
          {costs.isPending ? (
            <Loading rows={2} />
          ) : costs.isError ? (
            <ErrorState
              error={costs.error}
              retry={() => void costs.refetch()}
            />
          ) : (
            <MonthlyChart
              rows={costs.data.fuel.monthlyTotals.map((row) => ({
                ...row,
                amount: row.totalCost,
              }))}
              label="Koszt paliwa (PLN)"
            />
          )}
        </div>
      </section>
    </div>
  );
}
