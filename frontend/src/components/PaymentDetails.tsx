import type { Payment } from '../api/payments';

/** Formats an ISO instant as a readable local date-time, or a dash if absent. */
function formatDate(iso: string | null): string {
  if (!iso) return '—';
  const date = new Date(iso);
  return Number.isNaN(date.getTime()) ? iso : date.toLocaleString();
}

export function PaymentDetails({ payment }: { payment: Payment }) {
  return (
    <dl className="payment-details" aria-label="Payment details">
      <dt>Payment ID</dt>
      <dd data-testid="payment-id">{payment.id}</dd>

      <dt>Bitcoin address</dt>
      <dd data-testid="payment-address">{payment.bitcoinAddress}</dd>

      <dt>Amount</dt>
      <dd data-testid="payment-amount">{payment.amountSats.toLocaleString()} sats</dd>

      <dt>Status</dt>
      <dd>
        <span
          className={`badge badge--${payment.status.toLowerCase()}`}
          data-testid="payment-status"
        >
          {payment.status}
        </span>
      </dd>

      <dt>Expires at</dt>
      <dd data-testid="payment-expires">{formatDate(payment.expiresAt)}</dd>
    </dl>
  );
}
