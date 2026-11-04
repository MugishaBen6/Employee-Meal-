/**
 * Returns today's date as YYYY-MM-DD in the Africa/Kigali timezone (UTC+2, no DST).
 *
 * Why not `new Date().toISOString().split('T')[0]`?
 * `toISOString()` always returns UTC. At midnight UTC (02:00 Kigali time),
 * the UTC date is already the next calendar day, so the "today" default would
 * be wrong for 2 hours every night for users in Kigali.
 *
 * This function uses the Intl API to format the current instant in the
 * Africa/Kigali timezone and returns the canonical YYYY-MM-DD string that
 * HTML date inputs and the backend API expect.
 */
export const getKigaliToday = (): string =>
  new Intl.DateTimeFormat('en-CA', { timeZone: 'Africa/Kigali' }).format(new Date());
