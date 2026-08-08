import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import QRCode from 'qrcode';
import type { Payment } from '../api/payments';
import { PaymentInvoice } from './PaymentInvoice';

type ToDataURLMock = ((value: string, options?: unknown) => Promise<string>) & {
  mockClear: () => void;
  mockResolvedValue: (value: string) => ToDataURLMock;
  mockRejectedValueOnce: (value: unknown) => ToDataURLMock;
};

const toDataURLMock = QRCode.toDataURL as unknown as ToDataURLMock;

const samplePayment: Payment = {
  id: 'b1b827ea-7f47-4fca-a6ce-6753a4758c64',
  amountSats: 50000,
  bitcoinAddress: 'tb1qexampleaddress0000000000000000000',
  status: 'PENDING',
  createdAt: '2026-07-23T10:00:00Z',
  expiresAt: '2026-07-23T10:15:00Z',
  paidAt: null,
};

beforeEach(() => {
  toDataURLMock.mockClear();
  toDataURLMock.mockResolvedValue('data:image/png;base64,invoice');
});

function mockClipboard(writeText = vi.fn().mockResolvedValue(undefined)) {
  Object.defineProperty(navigator, 'clipboard', {
    configurable: true,
    value: {
      writeText,
    },
  });
  return writeText;
}

describe('PaymentInvoice', () => {
  it('displays the BIP21 URI and renders a QR code for the same URI', async () => {
    render(<PaymentInvoice payment={samplePayment} />);

    const uri = 'bitcoin:tb1qexampleaddress0000000000000000000?amount=0.0005';
    expect(screen.getByTestId('bip21-uri')).toHaveTextContent(uri);

    await waitFor(() => {
      expect(toDataURLMock).toHaveBeenCalledWith(uri, {
        errorCorrectionLevel: 'M',
        margin: 1,
        width: 208,
      });
    });
    expect(await screen.findByTestId('invoice-qr-code')).toHaveAttribute(
      'src',
      'data:image/png;base64,invoice',
    );
  });

  it('displays address and exact BTC amount with separate copy actions', async () => {
    const user = userEvent.setup();
    const writeTextMock = mockClipboard();
    render(<PaymentInvoice payment={samplePayment} />);
    await screen.findByTestId('invoice-qr-code');

    expect(screen.getByTestId('invoice-address')).toHaveTextContent(samplePayment.bitcoinAddress);
    expect(screen.getByTestId('invoice-amount')).toHaveTextContent('0.0005 BTC');

    await user.click(screen.getByRole('button', { name: /copy address/i }));
    expect(writeTextMock).toHaveBeenCalledWith(samplePayment.bitcoinAddress);
    expect(screen.getByRole('status')).toHaveTextContent(/bitcoin address copied/i);

    await user.click(screen.getByRole('button', { name: /copy amount/i }));
    expect(writeTextMock).toHaveBeenCalledWith('0.0005');
    expect(screen.getByRole('status')).toHaveTextContent(/bitcoin amount copied/i);
  });

  it('shows an error if the QR code cannot be generated', async () => {
    toDataURLMock.mockRejectedValueOnce(new Error('QR failed'));

    render(<PaymentInvoice payment={samplePayment} />);

    expect(await screen.findByRole('alert')).toHaveTextContent(/could not generate/i);
    expect(screen.queryByTestId('invoice-qr-code')).not.toBeInTheDocument();
  });

  it('shows an error when clipboard access fails', async () => {
    const user = userEvent.setup();
    mockClipboard(vi.fn().mockRejectedValueOnce(new Error('denied')));
    render(<PaymentInvoice payment={samplePayment} />);
    await screen.findByTestId('invoice-qr-code');

    await user.click(screen.getByRole('button', { name: /copy address/i }));

    expect(await screen.findByRole('alert')).toHaveTextContent(/could not copy bitcoin address/i);
  });
});
