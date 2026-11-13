export const reminderTypes = {
  INSURANCE: "Ubezpieczenie",
  TECHNICAL_INSPECTION: "Przegląd techniczny",
  SERVICE: "Serwis",
  OIL_CHANGE: "Wymiana oleju",
  TIRES: "Opony",
  OTHER: "Inne",
} as const;

export type ReminderType = keyof typeof reminderTypes;
export type ReminderStatus = "ACTIVE" | "COMPLETED" | "CANCELLED";
export type ReminderFinalStatus = Exclude<ReminderStatus, "ACTIVE">;

export const reminderStatuses: Record<ReminderStatus, string> = {
  ACTIVE: "Aktywne",
  COMPLETED: "Wykonane",
  CANCELLED: "Anulowane",
};

export interface ReminderRequest {
  type: ReminderType;
  title: string;
  description: string | null;
  dueDate: string;
  odometer: number | null;
}

export interface Reminder extends ReminderRequest {
  id: number;
  vehicleId: number;
  status: ReminderStatus;
  activationVersion: number;
  createdAt: string;
}

export interface ReminderPage {
  content: Reminder[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ReminderFilters {
  status: ReminderStatus | "";
  dueFrom: string;
  dueTo: string;
}

export const reminderTypeLabel = (type: ReminderType) => reminderTypes[type];
