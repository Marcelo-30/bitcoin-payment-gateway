import '@testing-library/jest-dom/vitest';
import { vi } from 'vitest';

vi.mock('qrcode', () => ({
  default: {
    toDataURL: vi.fn((value: string) => Promise.resolve(`data:image/png;base64,${btoa(value)}`)),
  },
}));
