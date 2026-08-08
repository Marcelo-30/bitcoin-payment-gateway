import { describe, expect, it } from 'vitest';
import { createBip21Uri, formatSatsAsBitcoin } from './bip21';

describe('formatSatsAsBitcoin', () => {
  it('formats satoshis as an exact BTC decimal', () => {
    expect(formatSatsAsBitcoin(1)).toBe('0.00000001');
    expect(formatSatsAsBitcoin(50000)).toBe('0.0005');
    expect(formatSatsAsBitcoin(100000000)).toBe('1');
    expect(formatSatsAsBitcoin(150000000)).toBe('1.5');
  });

  it('rejects unsafe or negative amounts', () => {
    expect(() => formatSatsAsBitcoin(-1)).toThrow(/non-negative/i);
    expect(() => formatSatsAsBitcoin(1.5)).toThrow(/safe integer/i);
  });
});

describe('createBip21Uri', () => {
  it('generates a BIP21 URI from address and amount', () => {
    expect(createBip21Uri('tb1qexampleaddress0000000000000000000', 50000))
      .toBe('bitcoin:tb1qexampleaddress0000000000000000000?amount=0.0005');
  });

  it('trims the address and rejects empty values', () => {
    expect(createBip21Uri(' tb1qabc ', 1)).toBe('bitcoin:tb1qabc?amount=0.00000001');
    expect(() => createBip21Uri('   ', 1)).toThrow(/address/i);
  });
});
