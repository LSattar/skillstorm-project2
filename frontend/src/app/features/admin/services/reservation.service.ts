import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, switchMap } from 'rxjs';
import { environment } from '../../../../environments/environment';

export type ReservationStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'CHECKED_IN' | 'CHECKED_OUT';

export type ReservationResponse = {
  reservationId: string;
  hotelId: string;
  userId: string;
  roomId: string;
  roomTypeId: string;
  startDate: string;
  endDate: string;
  guestCount: number;
  status: ReservationStatus;
  totalAmount: number;
  currency: string;
  specialRequests?: string;
  cancellationReason?: string;
  cancelledAt?: string;
  cancelledByUserId?: string;
  createdAt: string;
  updatedAt: string;
};

export type ReservationRequest = {
  hotelId: string;
  userId: string;
  roomId: string;
  roomTypeId: string;
  startDate: string;
  endDate: string;
  guestCount: number;
  status?: ReservationStatus;
  totalAmount: number;
  currency: string;
  specialRequests?: string;
};

@Injectable({ providedIn: 'root' })
export class ReservationService {
  private readonly api = environment.apiBaseUrl;

  constructor(private http: HttpClient) {}

  private ensureCsrfToken() {
    return this.http.get<{ headerName: string; token: string }>(`${this.api}/csrf`, {
      withCredentials: true,
    });
  }

  /**
   * Get all reservations, optionally filtered by userId, hotelId, or roomId
   */
  getAllReservations(params?: {
    userId?: string;
    hotelId?: string;
    roomId?: string;
  }): Observable<ReservationResponse[]> {
    const queryParams: Record<string, string> = {};
    if (params?.userId) queryParams['userId'] = params.userId;
    if (params?.hotelId) queryParams['hotelId'] = params.hotelId;
    if (params?.roomId) queryParams['roomId'] = params.roomId;

    const queryString = new URLSearchParams(queryParams).toString();
    const url = `${this.api}/reservations${queryString ? `?${queryString}` : ''}`;

    return this.http.get<ReservationResponse[]>(url, {
      withCredentials: true,
    });
  }

  /**
   * Get a single reservation by ID
   */
  getReservationById(id: string): Observable<ReservationResponse> {
    return this.http.get<ReservationResponse>(`${this.api}/reservations/${id}`, {
      withCredentials: true,
    });
  }

  /**
   * Create a new reservation
   */
  createReservation(payload: ReservationRequest): Observable<ReservationResponse> {
    return this.ensureCsrfToken().pipe(
      switchMap((csrf) =>
        this.http.post<ReservationResponse>(`${this.api}/reservations`, payload, {
          withCredentials: true,
          headers: csrf?.token ? { [csrf.headerName || 'X-XSRF-TOKEN']: csrf.token } : undefined,
        })
      )
    );
  }

  /**
   * Update a reservation
   */
  updateReservation(id: string, payload: ReservationRequest): Observable<ReservationResponse> {
    return this.ensureCsrfToken().pipe(
      switchMap((csrf) =>
        this.http.put<ReservationResponse>(`${this.api}/reservations/${id}`, payload, {
          withCredentials: true,
          headers: csrf?.token ? { [csrf.headerName || 'X-XSRF-TOKEN']: csrf.token } : undefined,
        })
      )
    );
  }

  /**
   * Cancel a reservation
   */
  cancelReservation(id: string, reason?: string): Observable<ReservationResponse> {
    return this.ensureCsrfToken().pipe(
      switchMap((csrf) => {
        const queryParams = reason ? `?reason=${encodeURIComponent(reason)}` : '';
        return this.http.post<ReservationResponse>(
          `${this.api}/reservations/${id}/cancel${queryParams}`,
          {},
          {
            withCredentials: true,
            headers: csrf?.token ? { [csrf.headerName || 'X-XSRF-TOKEN']: csrf.token } : undefined,
          }
        );
      })
    );
  }

  /**
   * Delete a reservation
   */
  deleteReservation(id: string): Observable<void> {
    return this.ensureCsrfToken().pipe(
      switchMap((csrf) =>
        this.http.delete<void>(`${this.api}/reservations/${id}`, {
          withCredentials: true,
          headers: csrf?.token ? { [csrf.headerName || 'X-XSRF-TOKEN']: csrf.token } : undefined,
        })
      )
    );
  }
}
