import { apiClient } from './client';

export type PaymentStatus = 'PENDING' | 'PAID' | 'EXPIRED';

/** Body sent to POST /api/payments. */
export interface CreatePaymentRequest {
  amountSats: number;
}

/** Payment as returned by the backend. */
export interface Payment {
  id: string;
  amountSats: number;
  bitcoinAddress: string;
  status: PaymentStatus;
  createdAt: string;
  expiresAt: string;
  paidAt: string | null;
}

/** Creates a simulated Bitcoin payment request. */
export function createPayment(request: CreatePaymentRequest): Promise<Payment> {
  return apiClient.post<Payment>('/api/payments', request);
}
