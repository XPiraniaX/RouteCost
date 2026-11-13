export const localToday = () => {
  const date = new Date();
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
};
export const dateLabel = (value: string) =>
  new Date(`${value}T12:00:00`).toLocaleDateString("pl-PL");
