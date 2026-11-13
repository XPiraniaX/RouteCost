import { VehicleWorkspace } from "../../components/VehicleWorkspace";
import { ExpenseContent } from "./ExpensePage";

export function ExpensePage() {
  return (
    <VehicleWorkspace
      title="Wydatki"
      description="Kontroluj koszty utrzymania każdego pojazdu."
      basePath="/expenses"
      actionLabel="Dodaj wydatek"
    >
      {(vehicle) => <ExpenseContent key={vehicle.id} vehicle={vehicle} />}
    </VehicleWorkspace>
  );
}
