import { VehicleWorkspace } from "../../components/VehicleWorkspace";
import { RemindersContent } from "./RemindersContent";

export function RemindersPage() {
  return (
    <VehicleWorkspace
      title="Przypomnienia"
      description="Pilnuj terminów serwisu, ubezpieczeń i przeglądów pojazdów."
      basePath="/reminders"
      actionLabel="Dodaj przypomnienie"
    >
      {(vehicle) => <RemindersContent key={vehicle.id} vehicle={vehicle} />}
    </VehicleWorkspace>
  );
}
