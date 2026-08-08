import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { PaymentStatusPage } from './PaymentStatusPage';
import { getPayment, simulatePayment, type Payment } from '../api/payments';
import { ApiError } from '../api/client';

vi.mock('../api/payments', () => ({
  getPayment: vi.fn(),
  simulatePayment: vi.fn(),
}));

const getPaymentMock = vi.mocked(getPayment);
const simulatePaymentMock = vi.mocked(simulatePayment);

const samplePayment: Payment = {
  id: 'b1b827ea-7f47-4fca-a6ce-6753a4758c64',
  amountSats: 50000,
  bitcoinAddress: 'tb1qexampleaddress0000000000000000000',
  status: 'PENDING',
  createdAt: '2026-07-23T10:00:00Z',
  expiresAt: '2026-07-23T10:15:00Z',
  paidAt: null,
};

function renderAtPaymentRoute(paymentId: string) {
  return render(
    <MemoryRouter initialEntries={[`/payments/${paymentId}`]}>
      <Routes>
        <Route path="/payments/:paymentId" element={<PaymentStatusPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

beforeEach(() => {
  getPaymentMock.mockReset();
  simulatePaymentMock.mockReset();
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

describe('PaymentStatusPage', () => {
  it('shows payment details once loaded', async () => {
    getPaymentMock.mockResolvedValue(samplePayment);

    renderAtPaymentRoute(samplePayment.id);

    await waitFor(() => {
      expect(screen.getByTestId('payment-id')).toHaveTextContent(samplePayment.id);
    });
    expect(screen.getByTestId('payment-status')).toHaveTextContent('PENDING');
    expect(screen.getByTestId('bip21-uri')).toHaveTextContent(
      'bitcoin:tb1qexampleaddress0000000000000000000?amount=0.0005',
    );
    expect(await screen.findByTestId('invoice-qr-code')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /create payment/i })).toHaveAttribute('href', '/');
    expect(screen.getByRole('link', { name: /payment history/i })).toHaveAttribute(
      'href',
      '/#payment-history',
    );
    expect(screen.getByRole('link', { name: /open payment status page/i })).toHaveAttribute(
      'href',
      `/payments/${samplePayment.id}`,
    );
    expect(screen.getByRole('button', { name: 'Simulate payment' })).toBeEnabled();
  });

  it('copies the complete payment link', async () => {
    const user = userEvent.setup();
    const writeTextMock = mockClipboard();
    getPaymentMock.mockResolvedValue(samplePayment);

    renderAtPaymentRoute(samplePayment.id);

    await screen.findByTestId('payment-id');
    await user.click(screen.getByRole('button', { name: /copy payment link/i }));

    expect(writeTextMock).toHaveBeenCalledWith(`${window.location.origin}/payments/${samplePayment.id}`);
    expect(screen.getByRole('status')).toHaveTextContent(/payment link copied/i);
  });

  it('shows an error when the payment link cannot be copied', async () => {
    const user = userEvent.setup();
    mockClipboard(vi.fn().mockRejectedValueOnce(new Error('denied')));
    getPaymentMock.mockResolvedValue(samplePayment);

    renderAtPaymentRoute(samplePayment.id);

    await screen.findByTestId('payment-id');
    await user.click(screen.getByRole('button', { name: /copy payment link/i }));

    expect(await screen.findByRole('alert')).toHaveTextContent(/could not copy payment link/i);
  });

  it('simulates a pending payment and displays the paid result', async () => {
    const user = userEvent.setup();
    let resolveSimulation!: (payment: Payment) => void;
    simulatePaymentMock.mockReturnValue(new Promise((resolve) => {
      resolveSimulation = resolve;
    }));
    getPaymentMock.mockResolvedValue(samplePayment);
    renderAtPaymentRoute(samplePayment.id);

    const button = await screen.findByRole('button', { name: 'Simulate payment' });
    await user.click(button);
    expect(simulatePaymentMock).toHaveBeenCalledWith(samplePayment.id);
    expect(button).toBeDisabled();
    expect(button).toHaveTextContent('Simulating payment');

    resolveSimulation({
      ...samplePayment,
      status: 'PAID',
      paidAt: '2026-07-23T10:01:00Z',
    });
    await waitFor(() => expect(screen.getByTestId('payment-status')).toHaveTextContent('PAID'));
    expect(screen.getByTestId('payment-paid-at')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /simulate payment/i })).not.toBeInTheDocument();
  });

  it.each(['PAID', 'EXPIRED'] as const)('does not show simulation for %s payments', async (status) => {
    getPaymentMock.mockResolvedValue({ ...samplePayment, status });
    renderAtPaymentRoute(samplePayment.id);
    await screen.findByTestId('payment-status');
    expect(screen.queryByRole('button', { name: /simulate payment/i })).not.toBeInTheDocument();
  });

  it('shows an error when simulation fails and allows retry', async () => {
    const user = userEvent.setup();
    getPaymentMock.mockResolvedValue(samplePayment);
    simulatePaymentMock.mockRejectedValue(new ApiError(409, 'Payment expired'));
    renderAtPaymentRoute(samplePayment.id);

    await user.click(await screen.findByRole('button', { name: 'Simulate payment' }));
    expect(await screen.findByRole('alert')).toHaveTextContent(/payment expired/i);
    expect(screen.getByRole('button', { name: 'Simulate payment' })).toBeEnabled();
  });

  it('shows a not-found message for a 404', async () => {
    getPaymentMock.mockRejectedValue(new ApiError(404, 'Payment not found'));

    renderAtPaymentRoute('does-not-exist');

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(/not found/i);
    });
  });

  it('shows a connection error message when the backend is unreachable', async () => {
    getPaymentMock.mockRejectedValue(new Error('Failed to fetch'));

    renderAtPaymentRoute(samplePayment.id);

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(/cannot reach backend/i);
    });
  });
});
