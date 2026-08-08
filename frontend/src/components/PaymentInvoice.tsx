import { useEffect, useMemo, useState } from 'react';
import QRCode from 'qrcode';
import type { Payment } from '../api/payments';
import { createBip21Uri, formatSatsAsBitcoin } from '../lib/bip21';

type CopyStatus =
  | { kind: 'idle' }
  | { kind: 'copied'; label: string }
  | { kind: 'error'; message: string };

export function PaymentInvoice({ payment }: { payment: Payment }) {
  const bip21Uri = useMemo(
    () => createBip21Uri(payment.bitcoinAddress, payment.amountSats),
    [payment.amountSats, payment.bitcoinAddress],
  );
  const bitcoinAmount = useMemo(
    () => formatSatsAsBitcoin(payment.amountSats),
    [payment.amountSats],
  );
  const [qrDataUrl, setQrDataUrl] = useState<string | null>(null);
  const [qrError, setQrError] = useState<string | null>(null);
  const [copyStatus, setCopyStatus] = useState<CopyStatus>({ kind: 'idle' });

  useEffect(() => {
    let active = true;

    setQrDataUrl(null);
    setQrError(null);

    QRCode.toDataURL(bip21Uri, {
      errorCorrectionLevel: 'M',
      margin: 1,
      width: 208,
    })
      .then((dataUrl) => {
        if (!active) return;
        setQrDataUrl(dataUrl);
      })
      .catch(() => {
        if (!active) return;
        setQrError('Could not generate the payment QR code.');
      });

    return () => {
      active = false;
    };
  }, [bip21Uri]);

  async function copyValue(value: string, label: string) {
    if (!navigator.clipboard) {
      setCopyStatus({
        kind: 'error',
        message: 'Clipboard access is not available.',
      });
      return;
    }

    try {
      await navigator.clipboard.writeText(value);
      setCopyStatus({ kind: 'copied', label });
    } catch {
      setCopyStatus({
        kind: 'error',
        message: `Could not copy ${label.toLowerCase()}.`,
      });
    }
  }

  return (
    <section className="payment-invoice" aria-labelledby="payment-invoice-title">
      <div className="payment-invoice__header">
        <h2 id="payment-invoice-title">Bitcoin invoice</h2>
        <code data-testid="bip21-uri">{bip21Uri}</code>
      </div>

      <div className="payment-invoice__body">
        <div className="payment-invoice__qr" aria-label="BIP21 QR code">
          {qrDataUrl && (
            <img
              src={qrDataUrl}
              alt="QR code for the Bitcoin payment invoice"
              data-testid="invoice-qr-code"
            />
          )}
          {!qrDataUrl && !qrError && (
            <p className="status status--loading" role="status">
              Generating QR code...
            </p>
          )}
          {qrError && (
            <p className="status status--error" role="alert">
              {qrError}
            </p>
          )}
        </div>

        <dl className="invoice-fields">
          <div>
            <dt>Bitcoin address</dt>
            <dd>
              <span data-testid="invoice-address">{payment.bitcoinAddress}</span>
              <button
                type="button"
                className="copy-button"
                onClick={() => copyValue(payment.bitcoinAddress, 'Bitcoin address')}
              >
                Copy address
              </button>
            </dd>
          </div>

          <div>
            <dt>Bitcoin amount</dt>
            <dd>
              <span data-testid="invoice-amount">{bitcoinAmount} BTC</span>
              <button
                type="button"
                className="copy-button"
                onClick={() => copyValue(bitcoinAmount, 'Bitcoin amount')}
              >
                Copy amount
              </button>
            </dd>
          </div>
        </dl>
      </div>

      {copyStatus.kind === 'copied' && (
        <p className="status status--ok" role="status">
          {copyStatus.label} copied.
        </p>
      )}
      {copyStatus.kind === 'error' && (
        <p className="status status--error" role="alert">
          {copyStatus.message}
        </p>
      )}
    </section>
  );
}
