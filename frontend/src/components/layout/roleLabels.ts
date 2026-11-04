/** Human-friendly role names for display (role values themselves are unchanged). */
export const ROLE_LABELS: Record<string, string> = {
  ADMIN: 'Administrator',
  MANAGING_DIRECTOR: 'Managing Director',
  ACCOUNTANT: 'Accountant',
  HR: 'Human Resources',
  USER: 'Employee',
};

export const roleLabel = (role?: string): string => (role ? ROLE_LABELS[role] ?? role : '');

/** Initials for avatars: "Jean Bosco" -> "JB", "admin" -> "A". */
export const initialsOf = (name?: string): string => {
  const parts = (name ?? '').trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return '?';
  if (parts.length === 1) return parts[0].charAt(0).toUpperCase();
  return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
};
