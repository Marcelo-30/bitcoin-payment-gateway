import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
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
    render(<PaymentForm />);
    expect(screen.getByLabelText(/amount \(satoshis\)/i)).toBeInTheDocument();
  });

  it('shows a validation error and does not call the API for invalid input', async () => {
    const user = userEvent.setup();
    render(<PaymentForm />);

    await user.click(screen.getByRole('button', { name: /create payment/i }));

    expect(screen.getByRole('alert')).toHaveTextContent(/required/i);
    expect(createPaymentMock).not.toHaveBeenCalled();
  });

  it('submits and displays the created payment details', async () => {
    createPaymentMock.mockResolvedValue(samplePayment);
    const user = userEvent.setup();
    render(<PaymentForm />);

    await user.type(screen.getByLabelText(/amount \(satoshis\)/i), '50000');
    await user.click(screen.getByRole('button', { name: /create payment/i }));

    await waitFor(() => {
      expect(screen.getByTestId('payment-id')).toHaveTextContent(samplePayment.id);
    });
    expect(createPaymentMock).toHaveBeenCalledWith({ amountSats: 50000 });
    expect(screen.getByTestId('payment-address')).toHaveTextContent(samplePayment.bitcoinAddress);
    expect(screen.getByTestId('payment-amount')).toHaveTextContent('50,000 sats');
    expect(screen.getByTestId('payment-status')).toHaveTextContent('PENDING');
    expect(screen.getByTestId('payment-expires')).not.toBeEmptyDOMElement();
  });

  it('shows the backend validation error message on failure', async () => {
    createPaymentMock.mockRejectedValue(new ApiError(400, 'amountSats must be greater than zero'));
    const user = userEvent.setup();
    render(<PaymentForm />);

    await user.type(screen.getByLabelText(/amount \(satoshis\)/i), '5');
    await user.click(screen.getByRole('button', { name: /create payment/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(/greater than zero/i);
    });
    expect(screen.queryByTestId('payment-id')).not.toBeInTheDocument();
  });
});
