// payment-transactions.service.ts
import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export type PaymentTransactionStatus =
  | 'PROCESSING'
  | 'SUCCEEDED'
  | 'FAILED'
  | 'REFUNDED'
  | 'CANCELLED';

export interface PaymentTransaction {
  id: string;
  reservationId: string;
  userId: string;

  guestName: string;
  guestEmail: string;

  checkIn: string;
  checkOut: string;

  amount: number;
  currency: string;

  status: PaymentTransactionStatus;

  provider: string;
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

export interface PaymentTransactionQueryParams {
  query?: string;
  status?: PaymentTransactionStatus | '';
  from?: string; // expects YYYY-MM-DD (recommended) OR ISO datetime if your backend uses OffsetDateTime
  to?: string; // expects YYYY-MM-DD (recommended) OR ISO datetime if your backend uses OffsetDateTime
  page?: number;
  size?: number;
  sort?: string; // e.g. "createdAt,desc"
}

@Injectable({ providedIn: 'root' })
export class PaymentTransactionsService {
  private readonly apiUrl = `${environment.apiBaseUrl}/payment-transactions`;

  constructor(private http: HttpClient) {}

  getTransactions(
    params: PaymentTransactionQueryParams
  ): Observable<PaymentTransactionListResponse> {
    let httpParams = new HttpParams();

    if (params.query) httpParams = httpParams.set('query', params.query);

    // Only send status if it's a real value (avoid sending empty string)
    if (params.status && params.status.trim() !== '') {
      httpParams = httpParams.set('status', params.status);
    }

    if (params.from) httpParams = httpParams.set('from', params.from);
    if (params.to) httpParams = httpParams.set('to', params.to);

    if (params.page !== undefined) httpParams = httpParams.set('page', String(params.page));
    if (params.size !== undefined) httpParams = httpParams.set('size', String(params.size));

    // Default sort if not provided (optional, but nice)
    httpParams = httpParams.set('sort', params.sort ?? 'createdAt,desc');

    // Important if your backend uses SESSION cookies
    return this.http.get<PaymentTransactionListResponse>(this.apiUrl, {
      params: httpParams,
      withCredentials: true,
    });
  }

  getTransactionById(id: string): Observable<PaymentTransaction> {
    return this.http.get<PaymentTransaction>(`${this.apiUrl}/${encodeURIComponent(id)}`, {
      withCredentials: true,
    });
  }
}
