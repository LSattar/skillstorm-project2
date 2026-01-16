import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, switchMap } from 'rxjs';
import { environment } from '../../../../environments/environment';

export type PaymentIntentResponse = {
  clientSecret: string;
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
   * Create a Stripe PaymentIntent
   * Amount + currency are resolved by backend from Reservation
   */
  createPaymentIntent(params: {
    reservationId: string;
    amount: number;
    currency: string;
  }): Observable<PaymentIntentResponse> {
    return this.ensureCsrfToken().pipe(
      switchMap((csrf) =>
        this.http.post<PaymentIntentResponse>(`${this.api}/payments/intents`, params, {
          withCredentials: true,
          headers: csrf?.token ? { [csrf.headerName || 'X-XSRF-TOKEN']: csrf.token } : undefined,
        })
      )
    );
  }

  /**
   * Confirm payment after Stripe Elements completes
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
