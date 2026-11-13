import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { ConfirmDialog } from "../../components/ConfirmDialog";
import { vehicleApi, type Vehicle } from "./api";
export function DeleteVehicleDialog({
  vehicle,
  close,
}: {
  vehicle: Vehicle;
  close: () => void;
}) {
  const navigate = useNavigate();
  const client = useQueryClient();
  const mutation = useMutation({
    mutationFn: () => vehicleApi.remove(vehicle.id),
    onSuccess: async () => {
      await Promise.all([
        client.invalidateQueries({ queryKey: ["vehicles"] }),
        client.invalidateQueries({ queryKey: ["dashboard"] }),
      ]);
      navigate("/vehicles", { state: { notice: "Pojazd został usunięty." } });
    },
  });
  return (
    <ConfirmDialog
      title="Usunąć pojazd?"
      description={
        <>
          Pojazd{" "}
          <strong>
            {vehicle.make} {vehicle.model}
          </strong>{" "}
          oraz powiązane z nim tankowania, wydatki i przypomnienia zostaną
          trwale usunięte. Tej operacji nie można cofnąć.
        </>
      }
      confirmLabel="Usuń pojazd"
      pending={mutation.isPending}
      error={mutation.error}
      close={close}
      confirm={() => mutation.mutate()}
    />
  );
}
