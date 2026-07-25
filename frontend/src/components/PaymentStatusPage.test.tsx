import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { PaymentStatusPage } from './PaymentStatusPage';
import { getPayment, type Payment } from '../api/payments';
import { ApiError } from '../api/client';

vi.mock('../api/payments', () => ({
  getPayment: vi.fn(),
}));

const getPaymentMock = vi.mocked(getPayment);

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
});

describe('PaymentStatusPage', () => {
  it('shows payment details once loaded', async () => {
    getPaymentMock.mockResolvedValue(samplePayment);

    renderAtPaymentRoute(samplePayment.id);

    await waitFor(() => {
      expect(screen.getByTestId('payment-id')).toHaveTextContent(samplePayment.id);
    });
    expect(screen.getByTestId('payment-status')).toHaveTextContent('PENDING');
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