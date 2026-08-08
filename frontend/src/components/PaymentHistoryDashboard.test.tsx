import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { getPayments, type Payment, type PaymentHistoryPage } from '../api/payments';
import { PaymentHistoryDashboard } from './PaymentHistoryDashboard';

vi.mock('../api/payments', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../api/payments')>();
  return {
    ...actual,
    getPayments: vi.fn(),
  };
});

const getPaymentsMock = vi.mocked(getPayments);

const pendingPayment: Payment = {
  id: 'b1b827ea-7f47-4fca-a6ce-6753a4758c64',
  amountSats: 50000,
  bitcoinAddress: 'tb1qpendingaddress0000000000000000000',
  status: 'PENDING',
  createdAt: '2026-07-23T10:00:00Z',
  expiresAt: '2026-07-23T10:15:00Z',
  paidAt: null,
};

const paidPayment: Payment = {
  id: 'd6e93617-b1ad-4f23-95bd-cdf6a983d6f2',
  amountSats: 75000,
  bitcoinAddress: 'tb1qpaidaddress0000000000000000000000',
  status: 'PAID',
  createdAt: '2026-07-24T10:00:00Z',
  expiresAt: '2026-07-24T10:15:00Z',
  paidAt: '2026-07-24T10:03:00Z',
};

function page(content: Payment[], overrides: Partial<PaymentHistoryPage> = {}): PaymentHistoryPage {
  return {
    content,
    number: 0,
    size: 10,
    totalElements: content.length,
    totalPages: content.length === 0 ? 0 : 1,
    first: true,
    last: true,
    ...overrides,
  };
}

function renderDashboard() {
  return render(
    <MemoryRouter>
      <PaymentHistoryDashboard />
    </MemoryRouter>,
  );
}

beforeEach(() => {
  getPaymentsMock.mockReset();
});

describe('PaymentHistoryDashboard', () => {
  it('displays payments returned by the API with links to status pages', async () => {
    getPaymentsMock.mockResolvedValue(page([pendingPayment, paidPayment]));

    renderDashboard();

    expect(await screen.findByText(pendingPayment.id)).toBeInTheDocument();
    expect(screen.getByText(paidPayment.id)).toBeInTheDocument();
    expect(screen.getByText('50,000 sats')).toBeInTheDocument();
    expect(screen.getByText('75,000 sats')).toBeInTheDocument();
    expect(screen.getAllByRole('link', { name: 'Open' })[0]).toHaveAttribute(
      'href',
      `/payments/${pendingPayment.id}`,
    );
    expect(getPaymentsMock).toHaveBeenCalledWith({ page: 0, size: 10, status: undefined });
  });

  it('filters payments by status', async () => {
    const user = userEvent.setup();
    getPaymentsMock
      .mockResolvedValueOnce(page([pendingPayment]))
      .mockResolvedValueOnce(page([paidPayment]));

    renderDashboard();
    await screen.findByText(pendingPayment.id);

    await user.click(screen.getByRole('button', { name: 'PAID' }));

    await waitFor(() => {
      expect(getPaymentsMock).toHaveBeenLastCalledWith({ page: 0, size: 10, status: 'PAID' });
    });
    expect(await screen.findByText(paidPayment.id)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'PAID' })).toHaveAttribute('aria-pressed', 'true');
  });

  it('preserves the selected filter when changing pages', async () => {
    const user = userEvent.setup();
    getPaymentsMock
      .mockResolvedValueOnce(page([pendingPayment], { last: false, totalElements: 2, totalPages: 2 }))
      .mockResolvedValueOnce(page([paidPayment], {
        last: false,
        totalElements: 2,
        totalPages: 2,
      }))
      .mockResolvedValueOnce(page([{ ...paidPayment, id: '5ca1ad16-a71c-492c-b9aa-f0b3b4b2c920' }], {
        number: 1,
        first: false,
        last: true,
        totalElements: 2,
        totalPages: 2,
      }));

    renderDashboard();
    await screen.findByText(pendingPayment.id);
    await user.click(screen.getByRole('button', { name: 'PAID' }));
    await screen.findByText(paidPayment.id);

    await user.click(screen.getByRole('button', { name: 'Next' }));

    await waitFor(() => {
      expect(getPaymentsMock).toHaveBeenLastCalledWith({ page: 1, size: 10, status: 'PAID' });
    });
    expect(await screen.findByTestId('payment-history-page')).toHaveTextContent('Page 2 of 2');
  });

  it('displays a loading state while payments are requested', () => {
    getPaymentsMock.mockReturnValue(new Promise(() => undefined));

    renderDashboard();

    expect(screen.getByRole('status')).toHaveTextContent(/loading payments/i);
  });

  it('displays an empty state when no payments exist', async () => {
    getPaymentsMock.mockResolvedValue(page([]));

    renderDashboard();

    expect(await screen.findByTestId('payment-history-empty')).toHaveTextContent(/no payments/i);
  });

  it('displays an error state and retries a failed request', async () => {
    const user = userEvent.setup();
    getPaymentsMock
      .mockRejectedValueOnce(new Error('Failed to fetch'))
      .mockResolvedValueOnce(page([pendingPayment]));

    renderDashboard();

    expect(await screen.findByRole('alert')).toHaveTextContent(/could not load payments/i);
    await user.click(screen.getByRole('button', { name: /retry/i }));

    expect(await screen.findByText(pendingPayment.id)).toBeInTheDocument();
    expect(getPaymentsMock).toHaveBeenCalledTimes(2);
  });
});
