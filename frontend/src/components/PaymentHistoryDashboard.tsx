import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  getPayments,
  type Payment,
  type PaymentHistoryPage,
  type PaymentStatus,
} from '../api/payments';

const PAGE_SIZE = 10;
const FILTERS = ['ALL', 'PENDING', 'PAID', 'EXPIRED'] as const;

type StatusFilter = typeof FILTERS[number];

type State =
  | { kind: 'loading' }
  | { kind: 'error'; message: string }
  | { kind: 'loaded'; page: PaymentHistoryPage };

function formatDate(iso: string | null): string {
  if (!iso) return '-';
  const date = new Date(iso);
  return Number.isNaN(date.getTime()) ? iso : date.toLocaleString();
}

function filterToStatus(filter: StatusFilter): PaymentStatus | undefined {
  return filter === 'ALL' ? undefined : filter;
}

export function PaymentHistoryDashboard() {
  const [filter, setFilter] = useState<StatusFilter>('ALL');
  const [pageNumber, setPageNumber] = useState(0);
  const [retryKey, setRetryKey] = useState(0);
  const [state, setState] = useState<State>({ kind: 'loading' });

  useEffect(() => {
    let active = true;

    setState({ kind: 'loading' });
    getPayments({
      page: pageNumber,
      size: PAGE_SIZE,
      status: filterToStatus(filter),
    })
      .then((page) => {
        if (active) setState({ kind: 'loaded', page });
      })
      .catch((error: unknown) => {
        if (!active) return;
        setState({
          kind: 'error',
          message: error instanceof Error ? error.message : 'Something went wrong',
        });
      });

    return () => {
      active = false;
    };
  }, [filter, pageNumber, retryKey]);

  function handleFilterChange(nextFilter: StatusFilter) {
    setFilter(nextFilter);
    setPageNumber(0);
  }

  return (
    <section className="payment-history" id="payment-history" aria-labelledby="payment-history-title">
      <div className="payment-history__header">
        <div>
          <h2 id="payment-history-title">Payment history</h2>
          <p className="payment-history__summary">View and filter recent payment requests.</p>
        </div>

        <div className="status-filter" aria-label="Filter payments by status">
          {FILTERS.map((option) => (
            <button
              key={option}
              type="button"
              className={option === filter ? 'status-filter__button is-active' : 'status-filter__button'}
              aria-pressed={option === filter}
              onClick={() => handleFilterChange(option)}
            >
              {option === 'ALL' ? 'All' : option}
            </button>
          ))}
        </div>
      </div>

      {state.kind === 'loading' && (
        <p className="status status--loading" role="status">
          Loading payments...
        </p>
      )}

      {state.kind === 'error' && (
        <div className="payment-history__state" role="alert">
          <p className="status status--error">Could not load payments: {state.message}</p>
          <button type="button" className="secondary-button" onClick={() => setRetryKey((key) => key + 1)}>
            Retry
          </button>
        </div>
      )}

      {state.kind === 'loaded' && state.page.content.length === 0 && (
        <p className="payment-history__state" data-testid="payment-history-empty">
          {filter === 'ALL' ? 'No payments found.' : `No ${filter} payments found.`}
        </p>
      )}

      {state.kind === 'loaded' && state.page.content.length > 0 && (
        <>
          <PaymentHistoryTable payments={state.page.content} />
          <PaginationControls
            page={state.page}
            onPrevious={() => setPageNumber((current) => Math.max(0, current - 1))}
            onNext={() => setPageNumber((current) => current + 1)}
          />
        </>
      )}
    </section>
  );
}

function PaymentHistoryTable({ payments }: { payments: Payment[] }) {
  return (
    <div className="payment-history__table-wrap">
      <table className="payment-history__table">
        <thead>
          <tr>
            <th scope="col">Payment</th>
            <th scope="col">Status</th>
            <th scope="col">Amount</th>
            <th scope="col">Address</th>
            <th scope="col">Created</th>
            <th scope="col">Expires</th>
            <th scope="col">Action</th>
          </tr>
        </thead>
        <tbody>
          {payments.map((payment) => (
            <tr key={payment.id}>
              <td data-label="Payment">
                <span className="payment-id">{payment.id}</span>
              </td>
              <td data-label="Status">
                <span className={`badge badge--${payment.status.toLowerCase()}`}>
                  {payment.status}
                </span>
              </td>
              <td data-label="Amount">{payment.amountSats.toLocaleString()} sats</td>
              <td data-label="Address">
                <span className="payment-address">{payment.bitcoinAddress}</span>
              </td>
              <td data-label="Created">{formatDate(payment.createdAt)}</td>
              <td data-label="Expires">{formatDate(payment.expiresAt)}</td>
              <td data-label="Action">
                <Link to={`/payments/${payment.id}`}>Open</Link>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function PaginationControls({
  page,
  onPrevious,
  onNext,
}: {
  page: PaymentHistoryPage;
  onPrevious: () => void;
  onNext: () => void;
}) {
  const totalPages = Math.max(page.totalPages, 1);

  return (
    <nav className="pagination" aria-label="Payment history pages">
      <button type="button" className="secondary-button" disabled={page.first} onClick={onPrevious}>
        Previous
      </button>
      <span data-testid="payment-history-page">
        Page {page.number + 1} of {totalPages}
      </span>
      <button type="button" className="secondary-button" disabled={page.last} onClick={onNext}>
        Next
      </button>
    </nav>
  );
}
