import { useState, type InputHTMLAttributes } from "react";
import { Eye, EyeOff, LockKeyhole } from "lucide-react";
import type { UseFormRegisterReturn } from "react-hook-form";
import { FormField } from "./FormField";

interface PasswordFieldProps extends Omit<InputHTMLAttributes<HTMLInputElement>, "type"> {
  label: string;
  error?: string;
  registration: UseFormRegisterReturn;
}

export function PasswordField({ label, error, registration, ...inputProps }: PasswordFieldProps) {
  const [visible, setVisible] = useState(false);
  return (
    <FormField
      {...inputProps}
      {...registration}
      label={label}
      error={error}
      type={visible ? "text" : "password"}
      leadingIcon={LockKeyhole}
      trailing={(
        <button
          type="button"
          className="icon-button"
          aria-label={visible ? "Ukryj hasło" : "Pokaż hasło"}
          onClick={() => setVisible((current) => !current)}
        >
          {visible ? <EyeOff size={17} /> : <Eye size={17} />}
        </button>
      )}
    />
  );
}
