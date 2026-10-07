const pad = (value: number) => String(value).padStart(2, '0');

/** A calendar day as the API writes it, in the browser's own time zone. */
export function isoDay(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

/** An instant as a `datetime-local` field shows it: the browser's own zone, to the minute. */
export function toLocalInput(iso: string): string {
  const date = new Date(iso);
  return `${isoDay(date)}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

/** A `datetime-local` value, read in the browser's own zone, as the instant the API stores. */
export function fromLocalInput(local: string): string {
  return new Date(local).toISOString();
}
