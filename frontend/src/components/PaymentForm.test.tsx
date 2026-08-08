import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes, useParams } from 'react-router-dom';
import { PaymentForm } from './PaymentForm';
import { validateAmount } from '../lib/validateAmount';
import { createPayment, type Payment } from '../api/payments';
import { ApiError } from '../api/client';

vi.mock('../api/payments', () => ({
  createPayment: vi.fn(),
}));

const createPaymentMock = vi.mocked(createPayment);

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
  createPaymentMock.mockReset();
});

function PaymentRouteProbe() {
  const { paymentId } = useParams<{ paymentId: string }>();
  return <p data-testid="payment-route">Payment route: {paymentId}</p>;
}

function renderPaymentForm() {
  return render(
    <MemoryRouter initialEntries={['/']}>
      <Routes>
        <Route path="/" element={<PaymentForm />} />
        <Route path="/payments/:paymentId" element={<PaymentRouteProbe />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('validateAmount', () => {
  it('rejects empty, non-numeric and non-positive values', () => {
    expect(validateAmount('')).toMatch(/required/i);
    expect(validateAmount('abc')).toMatch(/whole number/i);
    expect(validateAmount('12.5')).toMatch(/whole number/i);
    expect(validateAmount('0')).toMatch(/greater than zero/i);
  });

  it('accepts a positive integer', () => {
    expect(validateAmount('50000')).toBeNull();
  });
});

describe('PaymentForm', () => {
  it('renders the amount input', () => {
    renderPaymentForm();
    expect(screen.getByLabelText(/amount \(satoshis\)/i)).toBeInTheDocument();
  });

  it('shows a validation error and does not call the API for invalid input', async () => {
    const user = userEvent.setup();
    renderPaymentForm();

    await user.click(screen.getByRole('button', { name: /create payment/i }));

    expect(screen.getByRole('alert')).toHaveTextContent(/required/i);
    expect(createPaymentMock).not.toHaveBeenCalled();
    expect(screen.queryByTestId('payment-route')).not.toBeInTheDocument();
  });

  it('submits and navigates to the created payment status route', async () => {
    createPaymentMock.mockResolvedValue(samplePayment);
    const user = userEvent.setup();
    renderPaymentForm();

    await user.type(screen.getByLabelText(/amount \(satoshis\)/i), '50000');
    await user.click(screen.getByRole('button', { name: /create payment/i }));

    await waitFor(() => {
      expect(screen.getByTestId('payment-route')).toHaveTextContent(samplePayment.id);
    });
    expect(createPaymentMock).toHaveBeenCalledWith({ amountSats: 50000 });
  });

  it('shows the backend validation error message on failure without navigating', async () => {
    createPaymentMock.mockRejectedValue(new ApiError(400, 'amountSats must be greater than zero'));
    const user = userEvent.setup();
    renderPaymentForm();

    await user.type(screen.getByLabelText(/amount \(satoshis\)/i), '5');
    await user.click(screen.getByRole('button', { name: /create payment/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(/greater than zero/i);
    });
    expect(screen.queryByTestId('payment-route')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: /create payment/i })).toBeEnabled();
  });
});
