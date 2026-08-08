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

export interface PaymentHistoryPage {
  content: Payment[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface PaymentHistoryParams {
  page: number;
  size: number;
  status?: PaymentStatus;
}

/** Creates a simulated Bitcoin payment request. */
export function createPayment(request: CreatePaymentRequest): Promise<Payment> {
  return apiClient.post<Payment>('/api/payments', request);
}

export function getPayments(params: PaymentHistoryParams): Promise<PaymentHistoryPage> {
  const searchParams = new URLSearchParams({
    page: params.page.toString(),
    size: params.size.toString(),
  });

  if (params.status) {
    searchParams.set('status', params.status);
  }

  return apiClient.get<PaymentHistoryPage>(`/api/payments?${searchParams.toString()}`);
}

export function getPayment(id: string): Promise<Payment> {
  return apiClient.get<Payment>(`/api/payments/${id}`);
}

export function simulatePayment(id: string): Promise<Payment> {
  return apiClient.post<Payment>(`/api/payments/${id}/simulate-payment`);
}
