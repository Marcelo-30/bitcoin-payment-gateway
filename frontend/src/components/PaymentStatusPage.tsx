import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiError } from '../api/client';
import { getPayment, simulatePayment, type Payment } from '../api/payments';
import { PaymentDetails } from './PaymentDetails';

const POLL_INTERVAL_MS = 3000;

type State =
  | { kind: 'loading' }
  | { kind: 'not-found' }
  | { kind: 'error'; message: string }
  | { kind: 'loaded'; payment: Payment };

export function PaymentStatusPage() {
  const { paymentId } = useParams<{ paymentId: string }>();
  const [state, setState] = useState<State>({ kind: 'loading' });
  const [isSimulating, setIsSimulating] = useState(false);
  const [simulationError, setSimulationError] = useState<string | null>(null);
  const [copyStatus, setCopyStatus] = useState<'idle' | 'copied' | 'error'>('idle');

  useEffect(() => {
    if (!paymentId) return;

    let active = true;
    let timeoutId: ReturnType<typeof setTimeout> | undefined;

    async function poll() {
      try {
        const payment = await getPayment(paymentId!);
        if (!active) return;

        setState({ kind: 'loaded', payment });

        if (payment.status === 'PENDING') {
          timeoutId = setTimeout(poll, POLL_INTERVAL_MS);
        }
      } catch (err: unknown) {
        if (!active) return;

        if (err instanceof ApiError && err.status === 404) {
          setState({ kind: 'not-found' });
          return;
        }
        setState({
          kind: 'error',
          message: err instanceof Error ? err.message : 'Something went wrong',
        });
      }
    }

    poll();

    return () => {
      active = false;
      if (timeoutId) clearTimeout(timeoutId);
    };
  }, [paymentId]);

  async function handleSimulation() {
    if (!paymentId || state.kind !== 'loaded'
      || state.payment.status !== 'PENDING' || isSimulating) return;

    setIsSimulating(true);
    setSimulationError(null);
    try {
      const payment = await simulatePayment(paymentId);
      setState({ kind: 'loaded', payment });
    } catch (err: unknown) {
      setSimulationError(
        err instanceof Error ? err.message : 'Something went wrong',
      );
    } finally {
      setIsSimulating(false);
    }
  }

  async function copyPaymentLink() {
    if (!paymentId || !navigator.clipboard) {
      setCopyStatus('error');
      return;
    }

    try {
      await navigator.clipboard.writeText(`${window.location.origin}/payments/${paymentId}`);
      setCopyStatus('copied');
    } catch {
      setCopyStatus('error');
    }
  }

  const navigation = (
    <nav className="page-navigation" aria-label="Payment navigation">
      <Link to="/">Create payment</Link>
      <Link to="/#payment-history">Payment history</Link>
    </nav>
  );

  switch (state.kind) {
    case 'loading':
      return (
        <div className="payment-status-page">
          {navigation}
          <p className="status status--loading" role="status">Loading payment…</p>
        </div>
      );
    case 'not-found':
      return (
        <div className="payment-status-page">
          {navigation}
          <p className="status status--error" role="alert">Payment not found.</p>
        </div>
      );
    case 'error':
      return (
        <div className="payment-status-page">
          {navigation}
          <p className="status status--error" role="alert">Cannot reach backend: {state.message}</p>
        </div>
      );
    case 'loaded':
      return (
        <div className="payment-status-page">
          {navigation}
          <PaymentDetails payment={state.payment} />
          <div className="payment-status-actions">
            <button type="button" className="secondary-button" onClick={copyPaymentLink}>
              Copy payment link
            </button>
            <Link className="secondary-link" to={`/payments/${state.payment.id}`}>
              Open payment status page
            </Link>
          </div>
          {copyStatus === 'copied' && (
            <p className="status status--ok" role="status">
              Payment link copied.
            </p>
          )}
          {copyStatus === 'error' && (
            <p className="status status--error" role="alert">
              Could not copy payment link.
            </p>
          )}
          {state.payment.status === 'PENDING' && (
            <button
              type="button"
              className="simulate-payment"
              disabled={isSimulating}
              onClick={handleSimulation}
            >
              {isSimulating ? 'Simulating payment...' : 'Simulate payment'}
            </button>
          )}
          {simulationError && (
            <p className="status status--error" role="alert">
              Could not simulate payment: {simulationError}
            </p>
          )}
        </div>
      );
  }
}
