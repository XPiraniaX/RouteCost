import { useId, type InputHTMLAttributes, type ReactNode } from "react";
import type { LucideIcon } from "lucide-react";

interface FormFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
  leadingIcon?: LucideIcon;
  trailing?: ReactNode;
}

export function FormField({ label, error, leadingIcon: Icon, trailing, className = "", ...inputProps }: FormFieldProps) {
  const generatedId = useId();
  const id = inputProps.id ?? generatedId;
  const errorId = `${id}-error`;

  return (
    <div className="space-y-2">
      <label htmlFor={id} className="block text-[13px] font-medium text-slate-200">{label}</label>
      <div className={`field-wrap ${error ? "field-wrap-error" : ""}`}>
        {Icon && <Icon className="field-icon" size={17} aria-hidden="true" />}
        <input
          id={id}
          className={`field-input ${Icon ? "pl-11" : "pl-4"} ${trailing ? "pr-12" : "pr-4"} ${className}`}
          aria-invalid={Boolean(error)}
          aria-describedby={error ? errorId : undefined}
          {...inputProps}
        />
        {trailing && <span className="field-trailing">{trailing}</span>}
      </div>
      {error && <p id={errorId} className="text-xs leading-5 text-rose-300">{error}</p>}
    </div>
  );
}
