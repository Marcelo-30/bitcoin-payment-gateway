import { useState, type FormEvent } from 'react';
import { ApiError } from '../api/client';
import { createPayment, type Payment } from '../api/payments';
import { validateAmount } from '../lib/validateAmount';
import { PaymentDetails } from './PaymentDetails';

export function PaymentForm() {
  const [amount, setAmount] = useState('');
  const [validationError, setValidationError] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [payment, setPayment] = useState<Payment | null>(null);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setSubmitError(null);

    const error = validateAmount(amount);
    setValidationError(error);
    if (error) {
      return;
    }

    setLoading(true);
    setPayment(null);
    try {
      const created = await createPayment({ amountSats: Number(amount) });
      setPayment(created);
    } catch (err: unknown) {
      setSubmitError(
        err instanceof ApiError || err instanceof Error ? err.message : 'Something went wrong',
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <div>
      <form className="payment-form" onSubmit={handleSubmit} noValidate>
        <label htmlFor="amountSats">Amount (satoshis)</label>
        <input
          id="amountSats"
          name="amountSats"
          type="number"
          inputMode="numeric"
          min={1}
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          aria-invalid={validationError != null}
          aria-describedby={validationError ? 'amount-error' : undefined}
          disabled={loading}
        />

        {validationError && (
          <p id="amount-error" className="field-error" role="alert">
            {validationError}
          </p>
        )}

        <button type="submit" disabled={loading}>
          {loading ? 'Creating…' : 'Create payment'}
        </button>
      </form>

      {loading && (
        <p className="status status--loading" role="status">
          Creating payment…
        </p>
      )}

      {submitError && (
        <p className="status status--error" role="alert">
          {submitError}
        </p>
      )}

      {payment && <PaymentDetails payment={payment} />}
    </div>
  );
}
