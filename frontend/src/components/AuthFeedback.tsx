import { CircleAlert } from "lucide-react";

export function AuthFeedback({ message }: { message: string }) {
  return (
    <div role="alert" className="flex items-start gap-3 rounded-xl border border-rose-400/20 bg-rose-400/[0.08] px-4 py-3 text-sm leading-6 text-rose-200">
      <CircleAlert className="mt-0.5 shrink-0 text-rose-300" size={17} aria-hidden="true" />
      <span>{message}</span>
    </div>
  );
}
