import { useEffect, useRef } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { Trash2 } from "lucide-react";
import { ErrorState } from "../../components/ui";
import { vehicleApi, type Vehicle } from "./api";
export function DeleteVehicleDialog({
  vehicle,
  close,
}: {
  vehicle: Vehicle;
  close: () => void;
}) {
  const ref = useRef<HTMLDialogElement>(null);
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
  useEffect(() => {
    const dialog = ref.current;
    dialog?.showModal();
    return () => dialog?.close();
  }, []);
  return (
    <dialog
      ref={ref}
      className="confirm-dialog"
      aria-labelledby="delete-title"
      aria-describedby="delete-description"
      onCancel={(e) => {
        if (mutation.isPending) e.preventDefault();
        else close();
      }}
      onClose={close}
    >
      <Trash2 size={26} />
      <h2 id="delete-title">Usunąć pojazd?</h2>
      <p id="delete-description">
        Pojazd{" "}
        <strong>
          {vehicle.make} {vehicle.model}
        </strong>{" "}
        oraz powiązane z nim tankowania, wydatki i przypomnienia zostaną trwale
        usunięte. Tej operacji nie można cofnąć.
      </p>
      {mutation.isError && <ErrorState error={mutation.error} />}
      <div className="form-actions">
        <button
          autoFocus
          className="button button-secondary"
          disabled={mutation.isPending}
          onClick={close}
        >
          Anuluj
        </button>
        <button
          className="button button-danger"
          disabled={mutation.isPending}
          onClick={() => mutation.mutate()}
        >
          {mutation.isPending ? "Usuwanie…" : "Usuń pojazd"}
        </button>
      </div>
    </dialog>
  );
}
