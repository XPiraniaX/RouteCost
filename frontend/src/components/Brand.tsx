import { Route } from "lucide-react";

export function Brand({ compact = false }: { compact?: boolean }) {
  return (
    <span className="inline-flex items-center gap-3">
      <span className="brand-mark" aria-hidden="true"><Route size={19} strokeWidth={2.2} /></span>
      {!compact && <span className="text-[17px] font-semibold tracking-[-0.04em] text-white">route<span className="text-accent">cost</span></span>}
    </span>
  );
}
