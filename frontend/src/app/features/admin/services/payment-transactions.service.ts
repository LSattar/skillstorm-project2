import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export type PaymentTransactionStatus = 'PAID' | 'PENDING' | 'FAILED' | 'REFUNDED';

export interface PaymentTransaction {
  id: string;
  reservationId: string;
  userId: string;
  guestName: string;
  guestEmail: string;
  checkIn: string;
  checkOut: string;
  nightlyRate?: number;
  nights?: number;
  subtotal: number;
  taxTotal: number;
  taxLineItems?: { label: string; amount: number }[];
  feesTotal?: number;
  discountTotal?: number;
  discountLineItems?: { label: string; amount: number }[];
  rewardsApplied?: number;
  rewardsEarned?: number;
  total: number;
  currency: string;
  status: PaymentTransactionStatus;
  paymentProvider: string;
  paymentMethodLast4?: string;
  transactionId?: string;
  createdAt: string;
  updatedAt: string;
}

export interface PaymentTransactionListResponse {
  content: PaymentTransaction[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

@Injectable({ providedIn: 'root' })
export class PaymentTransactionsService {
  private readonly api = '/api/payment-transactions';

  constructor(private http: HttpClient) {}

  getTransactions(params: {
    query?: string;
    status?: PaymentTransactionStatus | '';
    from?: string;
    to?: string;
    page?: number;
    size?: number;
    sort?: string;
  }): Observable<PaymentTransactionListResponse> {
    const queryParams: Record<string, string> = {};
    if (params.query) queryParams['query'] = params.query;
    if (params.status) queryParams['status'] = params.status;
    if (params.from) queryParams['from'] = params.from;
    if (params.to) queryParams['to'] = params.to;
    if (params.page !== undefined) queryParams['page'] = String(params.page);
    if (params.size !== undefined) queryParams['size'] = String(params.size);
    if (params.sort) queryParams['sort'] = params.sort;

    const queryString = new URLSearchParams(queryParams).toString();
    return this.http.get<PaymentTransactionListResponse>(
      `${this.api}${queryString ? `?${queryString}` : ''}`
    );
  }

  getTransactionById(id: string): Observable<PaymentTransaction> {
    return this.http.get<PaymentTransaction>(`${this.api}/${id}`);
  }
}
