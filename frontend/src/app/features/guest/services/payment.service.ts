import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, switchMap } from 'rxjs';
import { environment } from '../../../../environments/environment';

export type PaymentIntentResponse = {
  clientSecret: string;
  paymentIntentId: string;
};

export type PaymentRequest = {
  reservationId: string;
  amount: number;
  currency: string;
};

export type PaymentResponse = {
  paymentId: string;
  reservationId: string;
  status: 'PROCESSING' | 'SUCCEEDED' | 'FAILED' | 'REFUNDED' | 'CANCELLED';
  amount: number;
  currency: string;
  transactionId?: string;
  receiptUrl?: string;
};

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private readonly api = environment.apiBaseUrl;

  constructor(private http: HttpClient) {}

  private ensureCsrfToken() {
    return this.http.get<{ headerName: string; token: string }>(`${this.api}/csrf`, {
      withCredentials: true,
    });
  }

  /**
   * Create a payment intent for a reservation
   * This will be called by the backend to create a Stripe PaymentIntent
   */
  createPaymentIntent(request: PaymentRequest): Observable<PaymentIntentResponse> {
    return this.ensureCsrfToken().pipe(
      switchMap((csrf) =>
        this.http.post<PaymentIntentResponse>(`${this.api}/payments/create-intent`, request, {
          withCredentials: true,
          headers: csrf?.token ? { [csrf.headerName || 'X-XSRF-TOKEN']: csrf.token } : undefined,
        })
      )
    );
  }

  /**
   * Confirm a payment after Stripe payment is completed
   */
  confirmPayment(paymentIntentId: string, reservationId: string): Observable<PaymentResponse> {
    return this.ensureCsrfToken().pipe(
      switchMap((csrf) =>
        this.http.post<PaymentResponse>(
          `${this.api}/payments/confirm`,
          { paymentIntentId, reservationId },
          {
            withCredentials: true,
            headers: csrf?.token ? { [csrf.headerName || 'X-XSRF-TOKEN']: csrf.token } : undefined,
          }
        )
      )
    );
  }

  /**
   * Get payment status for a reservation
   */
  getPaymentStatus(reservationId: string): Observable<PaymentResponse[]> {
    return this.http.get<PaymentResponse[]>(`${this.api}/payments/reservation/${reservationId}`, {
      withCredentials: true,
    });
  }
}
