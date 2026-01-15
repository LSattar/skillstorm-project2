import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../../environments/environment';

export type ReservationStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'CHECKED_IN' | 'CHECKED_OUT';

export type Reservation = {
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
  // Display fields (may be joined from other tables)
  hotelName?: string;
  guestName?: string;
};

export type BookingStatistics = {
  totalBookings: number;
  confirmedBookings: number;
  pendingBookings: number;
  cancelledBookings: number;
  checkedInBookings: number;
  checkedOutBookings: number;
  totalRevenue: number;
  averageBookingValue: number;
  bookingsByStatus: Record<ReservationStatus, number>;
};

export type MonthlyRevenue = {
  month: string;
  year: number;
  revenue: number;
  bookingCount: number;
};

export type Alert = {
  type: 'warning' | 'info' | 'error' | 'success';
  title: string;
  message: string;
  count?: number;
};

@Injectable({ providedIn: 'root' })
export class AdminMetricsService {
  private readonly api = environment.apiBaseUrl;

  constructor(private http: HttpClient) {}

  private toFiniteNumber(value: unknown, fallback = 0): number {
    const n =
      typeof value === 'number'
        ? value
        : typeof value === 'string'
          ? Number(value)
          : fallback;
    return Number.isFinite(n) ? n : fallback;
  }

  private toStringSafe(value: unknown, fallback = ''): string {
    if (value === null || value === undefined) return fallback;
    if (typeof value === 'string') return value;
    if (typeof value === 'number' || typeof value === 'boolean') return String(value);
    return fallback;
  }

  private toReservationStatus(value: unknown): ReservationStatus {
    switch (value) {
      case 'PENDING':
      case 'CONFIRMED':
      case 'CANCELLED':
      case 'CHECKED_IN':
      case 'CHECKED_OUT':
        return value;
      default:
        return 'PENDING';
    }
  }

  private parseValidDate(value: unknown): Date | null {
    const s = this.toStringSafe(value, '');
    if (!s) return null;
    const d = new Date(s);
    return Number.isFinite(d.getTime()) ? d : null;
  }

  private normalizeReservations(payload: unknown): Reservation[] {
    if (!Array.isArray(payload)) return [];
    return payload.map((raw) => {
      const r = (raw ?? {}) as Partial<Record<keyof Reservation, unknown>>;
      const createdAt = this.toStringSafe(r.createdAt, '');
      const updatedAt = this.toStringSafe(r.updatedAt, createdAt);
      const startDate = this.toStringSafe(r.startDate, '');
      const endDate = this.toStringSafe(r.endDate, '');

      return {
        reservationId: this.toStringSafe(r.reservationId, ''),
        hotelId: this.toStringSafe(r.hotelId, ''),
        userId: this.toStringSafe(r.userId, ''),
        roomId: this.toStringSafe(r.roomId, ''),
        roomTypeId: this.toStringSafe(r.roomTypeId, ''),
        startDate,
        endDate,
        guestCount: Math.max(0, Math.floor(this.toFiniteNumber(r.guestCount, 0))),
        status: this.toReservationStatus(r.status),
        totalAmount: this.toFiniteNumber(r.totalAmount, 0),
        currency: this.toStringSafe(r.currency, 'USD') || 'USD',
        specialRequests: this.toStringSafe(r.specialRequests, '') || undefined,
        cancellationReason: this.toStringSafe(r.cancellationReason, '') || undefined,
        cancelledAt: this.toStringSafe(r.cancelledAt, '') || undefined,
        cancelledByUserId: this.toStringSafe(r.cancelledByUserId, '') || undefined,
        createdAt,
        updatedAt,
      };
    });
  }

  getAllReservations(): Observable<Reservation[]> {
    return this.http
      .get<unknown>(`${this.api}/reservations`, {
        withCredentials: true,
      })
      .pipe(map((payload) => this.normalizeReservations(payload)));
  }

  getBookingStatistics(): Observable<BookingStatistics> {
    return this.getAllReservations().pipe(
      map((reservations) => {
        const safeReservations = Array.isArray(reservations) ? reservations : [];
        const totalBookings = safeReservations.length;
        const bookingsByStatus = safeReservations.reduce(
          (acc, res) => {
            const status = this.toReservationStatus(res?.status);
            acc[status] = (acc[status] || 0) + 1;
            return acc;
          },
          {} as Record<ReservationStatus, number>
        );

        const totalRevenue = safeReservations
          .filter((r) => this.toReservationStatus(r?.status) !== 'CANCELLED')
          .reduce((sum, r) => sum + this.toFiniteNumber(r?.totalAmount, 0), 0);

        const confirmedBookings = bookingsByStatus.CONFIRMED || 0;
        const pendingBookings = bookingsByStatus.PENDING || 0;
        const cancelledBookings = bookingsByStatus.CANCELLED || 0;
        const checkedInBookings = bookingsByStatus.CHECKED_IN || 0;
        const checkedOutBookings = bookingsByStatus.CHECKED_OUT || 0;

        const averageBookingValue =
          totalBookings > 0 ? totalRevenue / totalBookings : 0;

        return {
          totalBookings,
          confirmedBookings,
          pendingBookings,
          cancelledBookings,
          checkedInBookings,
          checkedOutBookings,
          totalRevenue,
          averageBookingValue,
          bookingsByStatus: {
            PENDING: pendingBookings,
            CONFIRMED: confirmedBookings,
            CANCELLED: cancelledBookings,
            CHECKED_IN: checkedInBookings,
            CHECKED_OUT: checkedOutBookings,
          },
        };
      })
    );
  }

  getMonthlyRevenue(): Observable<MonthlyRevenue[]> {
    return this.getAllReservations().pipe(
      map((reservations) => {
        const revenueMap = new Map<string, { revenue: number; count: number }>();

        (Array.isArray(reservations) ? reservations : [])
          .filter((r) => this.toReservationStatus(r?.status) !== 'CANCELLED')
          .forEach((reservation) => {
            const date = this.parseValidDate(reservation?.createdAt);
            if (!date) return;

            const monthKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
            const existing = revenueMap.get(monthKey) || { revenue: 0, count: 0 };
            revenueMap.set(monthKey, {
              revenue: existing.revenue + this.toFiniteNumber(reservation?.totalAmount, 0),
              count: existing.count + 1,
            });
          });

        return Array.from(revenueMap.entries())
          .map(([key, data]) => {
            const [year, month] = key.split('-');
            const yearNum = this.toFiniteNumber(year, NaN);
            const monthNum = this.toFiniteNumber(month, NaN);
            const date = new Date(yearNum, monthNum - 1);
            return {
              month: Number.isFinite(date.getTime())
                ? date.toLocaleDateString('en-US', { month: 'short' })
                : '—',
              year: Number.isFinite(yearNum) ? yearNum : 0,
              revenue: this.toFiniteNumber(data.revenue, 0),
              bookingCount: Math.max(0, Math.floor(this.toFiniteNumber(data.count, 0))),
            };
          })
          .sort((a, b) => {
            if (a.year !== b.year) return a.year - b.year;
            const monthOrder = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
            return monthOrder.indexOf(a.month) - monthOrder.indexOf(b.month);
          })
          .slice(-12); 
      })
    );
  }

  getAlerts(): Observable<Alert[]> {
    return this.getAllReservations().pipe(
      map((reservations) => {
        const alerts: Alert[] = [];

        const safeReservations = Array.isArray(reservations) ? reservations : [];
        const pendingCount = safeReservations.filter((r) => this.toReservationStatus(r?.status) === 'PENDING').length;
        if (pendingCount > 0) {
          alerts.push({
            type: 'warning',
            title: 'Pending Reservations',
            message: `${pendingCount} reservation${pendingCount !== 1 ? 's' : ''} awaiting confirmation`,
            count: pendingCount,
          });
        }

        const recentCancellations = safeReservations.filter(
          (r) => this.toReservationStatus(r?.status) === 'CANCELLED' && !!this.toStringSafe(r?.cancelledAt, '')
        ).length;
        if (recentCancellations > 0) {
          alerts.push({
            type: 'error',
            title: 'Cancellations',
            message: `${recentCancellations} cancelled reservation${recentCancellations !== 1 ? 's' : ''}`,
            count: recentCancellations,
          });
        }

        const checkedInCount = safeReservations.filter((r) => this.toReservationStatus(r?.status) === 'CHECKED_IN').length;
        if (checkedInCount > 0) {
          alerts.push({
            type: 'info',
            title: 'Active Check-ins',
            message: `${checkedInCount} guest${checkedInCount !== 1 ? 's' : ''} currently checked in`,
            count: checkedInCount,
          });
        }

        const today = new Date();
        today.setHours(0, 0, 0, 0);
        const upcomingCheckIns = safeReservations.filter((r) => {
          if (this.toReservationStatus(r?.status) !== 'CONFIRMED') return false;
          const checkInDate = this.parseValidDate(r?.startDate);
          if (!checkInDate) return false;
          checkInDate.setHours(0, 0, 0, 0);
          const daysUntil = Math.ceil((checkInDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
          return daysUntil >= 0 && daysUntil <= 7;
        }).length;

        if (upcomingCheckIns > 0) {
          alerts.push({
            type: 'info',
            title: 'Upcoming Check-ins',
            message: `${upcomingCheckIns} confirmed reservation${upcomingCheckIns !== 1 ? 's' : ''} in the next 7 days`,
            count: upcomingCheckIns,
          });
        }

        if (alerts.length === 0) {
          alerts.push({
            type: 'success',
            title: 'All Clear',
            message: 'No urgent alerts at this time',
          });
        }

        return alerts;
      })
    );
  }

  getRecentBookings(limit: number = 10): Observable<Reservation[]> {
    return this.getAllReservations().pipe(
      map((reservations) =>
        (Array.isArray(reservations) ? reservations : [])
          .sort((a, b) => {
            const bTime = this.parseValidDate(b?.createdAt)?.getTime() ?? 0;
            const aTime = this.parseValidDate(a?.createdAt)?.getTime() ?? 0;
            return bTime - aTime;
          })
          .slice(0, Math.max(0, Math.floor(this.toFiniteNumber(limit, 10))))
      )
    );
  }

  getOperationalMetrics(hotelId?: string): Observable<{
    totalRooms: number;
    occupiedRooms: number;
    occupancyRate: number;
    checkInsToday: number;
    checkInsPending: number;
    checkOutsToday: number;
    checkOutsPending: number;
  }> {
    const url = hotelId 
      ? `${this.api}/admin/metrics?hotelId=${encodeURIComponent(hotelId)}`
      : `${this.api}/admin/metrics`;
    
    return this.http.get<{
      totalRooms: number;
      occupiedRooms: number;
      occupancyRate: number;
      checkInsToday: number;
      checkInsPending: number;
      checkOutsToday: number;
      checkOutsPending: number;
    }>(url, {
      withCredentials: true,
    }).pipe(
      map((metrics) => ({
        totalRooms: this.toFiniteNumber(metrics?.totalRooms, 0),
        occupiedRooms: this.toFiniteNumber(metrics?.occupiedRooms, 0),
        occupancyRate: this.toFiniteNumber(metrics?.occupancyRate, 0),
        checkInsToday: this.toFiniteNumber(metrics?.checkInsToday, 0),
        checkInsPending: this.toFiniteNumber(metrics?.checkInsPending, 0),
        checkOutsToday: this.toFiniteNumber(metrics?.checkOutsToday, 0),
        checkOutsPending: this.toFiniteNumber(metrics?.checkOutsPending, 0),
      }))
    );
  }
}

