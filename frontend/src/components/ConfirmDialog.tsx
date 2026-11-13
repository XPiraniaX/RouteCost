import { useEffect, useId, useRef, type ReactNode } from "react";
import { Trash2 } from "lucide-react";
import { ErrorState } from "./ui";

export function ConfirmDialog({
  title,
  description,
  confirmLabel,
  pending,
  error,
  close,
  confirm,
}: {
  title: string;
  description: ReactNode;
  confirmLabel: string;
  pending: boolean;
  error: Error | null;
  close: () => void;
  confirm: () => void;
}) {
  const ref = useRef<HTMLDialogElement>(null);
  const closeRef = useRef(close);
  const ignoreProgrammaticClose = useRef(false);
  const titleId = useId();
  const descriptionId = useId();
  closeRef.current = close;
  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    const handleClose = () => {
      if (ignoreProgrammaticClose.current) {
        ignoreProgrammaticClose.current = false;
        return;
      }
      closeRef.current();
    };
    dialog.addEventListener("close", handleClose);
    dialog.showModal();
    return () => {
      if (dialog.open) {
        ignoreProgrammaticClose.current = true;
        dialog.close();
      }
      dialog.removeEventListener("close", handleClose);
    };
  }, []);
  return (
    <dialog
      ref={ref}
      className="confirm-dialog"
      aria-labelledby={titleId}
      aria-describedby={descriptionId}
      onCancel={(event) => {
        if (pending) event.preventDefault();
        else close();
      }}
    >
      <Trash2 size={26} />
      <h2 id={titleId}>{title}</h2>
      <p id={descriptionId}>{description}</p>
      {error && <ErrorState error={error} />}
      <div className="form-actions">
        <button
          type="button"
          autoFocus
          className="button button-secondary"
          disabled={pending}
          onClick={close}
        >
          Anuluj
        </button>
        <button
          type="button"
          className="button button-danger"
          disabled={pending}
          onClick={confirm}
        >
          {pending ? "Usuwanie…" : confirmLabel}
        </button>
      </div>
    </dialog>
  );
}
