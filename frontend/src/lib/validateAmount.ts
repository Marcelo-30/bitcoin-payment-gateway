/** Validates the satoshi amount field. Returns an error message, or null when valid. */
export function validateAmount(raw: string): string | null {
  const trimmed = raw.trim();
  if (trimmed === '') {
    return 'Amount is required';
  }
  if (!/^\d+$/.test(trimmed)) {
    return 'Amount must be a whole number of satoshis';
  }
  if (Number(trimmed) <= 0) {
    return 'Amount must be greater than zero';
  }
  return null;
}
