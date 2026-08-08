const SATOSHIS_PER_BITCOIN = 100_000_000n;

export function formatSatsAsBitcoin(amountSats: number): string {
  if (!Number.isSafeInteger(amountSats) || amountSats < 0) {
    throw new Error('Amount must be a non-negative safe integer');
  }

  const sats = BigInt(amountSats);
  const whole = sats / SATOSHIS_PER_BITCOIN;
  const fractional = sats % SATOSHIS_PER_BITCOIN;

  if (fractional === 0n) {
    return whole.toString();
  }

  return `${whole}.${fractional.toString().padStart(8, '0').replace(/0+$/, '')}`;
}

export function createBip21Uri(bitcoinAddress: string, amountSats: number): string {
  const address = bitcoinAddress.trim();
  if (!address) {
    throw new Error('Bitcoin address is required');
  }

  return `bitcoin:${address}?amount=${formatSatsAsBitcoin(amountSats)}`;
}
