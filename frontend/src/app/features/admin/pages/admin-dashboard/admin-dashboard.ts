import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { Footer } from '../../../../shared/footer/footer';
import { Header } from '../../../../shared/header/header';
import { AuthService } from '../../../auth/services/auth.service';
import { UserProfileModal } from '../../../users/components/user-profile-modal/user-profile-modal';
import { Alert, MonthlyRevenue, Reservation } from '../../services/admin-metrics.service';

export type OperationalMetrics = {
  totalRooms: number;
  occupiedRooms: number;
  occupancyRate: number;
  checkInsToday: number;
  checkInsPending: number;
  checkOutsToday: number;
  checkOutsPending: number;
};

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, Header, Footer, UserProfileModal],
  templateUrl: './admin-dashboard.html',
  styleUrl: './admin-dashboard.css',
})
export class AdminDashboard implements OnInit {
  protected readonly auth = inject(AuthService);
  protected readonly router = inject(Router);

  goToSystemSettings() {
    this.router.navigate(['/admin/system-settings']);
  }

  protected readonly isAuthenticated = this.auth.isAuthenticated;
  protected readonly roleLabel = this.auth.primaryRoleLabel;
  protected readonly userLabel = computed(() => {
    const me = this.auth.meSignal();
    if (!me) return '';
    const first = me.firstName?.trim();
    if (first) return first;
    return me.email || '';
  });
  protected readonly userEmail = computed(() => this.auth.meSignal()?.email ?? '');

  operationalMetrics: OperationalMetrics | null = null;
  monthlyRevenue: MonthlyRevenue[] = [];
  alerts: Alert[] = [];
  recentBookings: Reservation[] = [];
  loading = false;
  error: string | null = null;
  totalRevenueLast12 = 0;
  totalBookingsLast12 = 0;

  isNavOpen = false;
  today = new Date();

  ngOnInit(): void {
    this.loadMockData();
  }

  loadMockData(): void {
    this.operationalMetrics = {
      totalRooms: 120,
      occupiedRooms: 87,
      occupancyRate: 73,
      checkInsToday: 14,
      checkInsPending: 6,
      checkOutsToday: 11,
      checkOutsPending: 3,
    };

    this.monthlyRevenue = [
      { month: 'Feb', year: 2025, revenue: 12500, bookingCount: 28 },
      { month: 'Mar', year: 2025, revenue: 18200, bookingCount: 35 },
      { month: 'Apr', year: 2025, revenue: 15800, bookingCount: 31 },
      { month: 'May', year: 2025, revenue: 24100, bookingCount: 42 },
      { month: 'Jun', year: 2025, revenue: 31500, bookingCount: 52 },
      { month: 'Jul', year: 2025, revenue: 38000, bookingCount: 61 },
      { month: 'Aug', year: 2025, revenue: 35200, bookingCount: 58 },
      { month: 'Sep', year: 2025, revenue: 22300, bookingCount: 40 },
      { month: 'Oct', year: 2025, revenue: 19700, bookingCount: 36 },
      { month: 'Nov', year: 2025, revenue: 16100, bookingCount: 32 },
      { month: 'Dec', year: 2025, revenue: 28800, bookingCount: 48 },
      { month: 'Jan', year: 2026, revenue: 26400, bookingCount: 45 },
    ];

    this.alerts = [
      {
        type: 'warning',
        title: 'Pending Reservations',
        message: '23 reservations awaiting confirmation',
        count: 23,
      },
      {
        type: 'info',
        title: 'Active Check-ins',
        message: '12 guests currently checked in',
        count: 12,
      },
      {
        type: 'info',
        title: 'Upcoming Check-ins',
        message: '8 confirmed reservations in the next 7 days',
        count: 8,
      },
      {
        type: 'error',
        title: 'Cancellations',
        message: '3 cancellations in the last 24 hours',
        count: 3,
      },
    ];

    this.recentBookings = [
      {
        reservationId: 'res-abc123def456',
        hotelId: 'hotel-1',
        hotelName: 'ReserveOne Downtown',
        userId: 'user-101',
        guestName: 'Sarah Johnson',
        roomId: 'room-205',
        roomTypeId: 'deluxe-king',
        startDate: '2026-01-15',
        endDate: '2026-01-18',
        guestCount: 2,
        status: 'CONFIRMED',
        totalAmount: 450,
        currency: 'USD',
        createdAt: '2026-01-05T10:30:00Z',
        updatedAt: '2026-01-05T10:30:00Z',
      },
      {
        reservationId: 'res-xyz789ghi012',
        hotelId: 'hotel-2',
        hotelName: 'ReserveOne Airport',
        userId: 'user-102',
        guestName: 'Michael Chen',
        roomId: 'room-310',
        roomTypeId: 'suite',
        startDate: '2026-01-10',
        endDate: '2026-01-12',
        guestCount: 3,
        status: 'CHECKED_IN',
        totalAmount: 680,
        currency: 'USD',
        createdAt: '2026-01-04T14:20:00Z',
        updatedAt: '2026-01-10T15:00:00Z',
      },
      {
        reservationId: 'res-mno345pqr678',
        hotelId: 'hotel-1',
        hotelName: 'ReserveOne Downtown',
        userId: 'user-103',
        guestName: 'Emily Rodriguez',
        roomId: 'room-102',
        roomTypeId: 'standard-double',
        startDate: '2026-01-20',
        endDate: '2026-01-23',
        guestCount: 2,
        status: 'PENDING',
        totalAmount: 320,
        currency: 'USD',
        createdAt: '2026-01-06T09:15:00Z',
        updatedAt: '2026-01-06T09:15:00Z',
      },
      {
        reservationId: 'res-stu901vwx234',
        hotelId: 'hotel-3',
        hotelName: 'ReserveOne Beach Resort',
        userId: 'user-104',
        guestName: 'David Kim',
        roomId: 'room-405',
        roomTypeId: 'deluxe-double',
        startDate: '2026-01-08',
        endDate: '2026-01-11',
        guestCount: 4,
        status: 'CHECKED_OUT',
        totalAmount: 520,
        currency: 'USD',
        createdAt: '2025-12-28T16:45:00Z',
        updatedAt: '2026-01-11T11:00:00Z',
      },
      {
        reservationId: 'res-def567hij890',
        hotelId: 'hotel-2',
        hotelName: 'ReserveOne Airport',
        userId: 'user-105',
        guestName: 'Jessica Taylor',
        roomId: 'room-201',
        roomTypeId: 'standard-king',
        startDate: '2026-01-25',
        endDate: '2026-01-27',
        guestCount: 1,
        status: 'CONFIRMED',
        totalAmount: 280,
        currency: 'USD',
        createdAt: '2026-01-06T11:00:00Z',
        updatedAt: '2026-01-06T11:00:00Z',
      },
      {
        reservationId: 'res-klm234nop567',
        hotelId: 'hotel-1',
        hotelName: 'ReserveOne Downtown',
        userId: 'user-106',
        guestName: 'Robert Williams',
        roomId: 'room-308',
        roomTypeId: 'suite',
        startDate: '2026-02-01',
        endDate: '2026-02-05',
        guestCount: 2,
        status: 'CONFIRMED',
        totalAmount: 890,
        currency: 'USD',
        createdAt: '2026-01-05T13:30:00Z',
        updatedAt: '2026-01-05T13:30:00Z',
      },
      {
        reservationId: 'res-qrs678tuv901',
        hotelId: 'hotel-3',
        hotelName: 'ReserveOne Beach Resort',
        userId: 'user-107',
        guestName: 'Amanda Martinez',
        roomId: 'room-105',
        roomTypeId: 'standard-double',
        startDate: '2026-01-12',
        endDate: '2026-01-14',
        guestCount: 2,
        status: 'CHECKED_IN',
        totalAmount: 340,
        currency: 'USD',
        createdAt: '2026-01-03T08:20:00Z',
        updatedAt: '2026-01-12T14:00:00Z',
      },
      {
        reservationId: 'res-wxy234zab567',
        hotelId: 'hotel-2',
        hotelName: 'ReserveOne Airport',
        userId: 'user-108',
        guestName: 'Christopher Lee',
        roomId: 'room-410',
        roomTypeId: 'deluxe-king',
        startDate: '2026-01-18',
        endDate: '2026-01-21',
        guestCount: 3,
        status: 'PENDING',
        totalAmount: 510,
        currency: 'USD',
        createdAt: '2026-01-07T10:45:00Z',
        updatedAt: '2026-01-07T10:45:00Z',
      },
      {
        reservationId: 'res-cde890fgh123',
        hotelId: 'hotel-1',
        hotelName: 'ReserveOne Downtown',
        userId: 'user-109',
        guestName: 'Jennifer Brown',
        roomId: 'room-203',
        roomTypeId: 'standard-king',
        startDate: '2026-01-05',
        endDate: '2026-01-07',
        guestCount: 1,
        status: 'CHECKED_OUT',
        totalAmount: 290,
        currency: 'USD',
        createdAt: '2025-12-30T15:10:00Z',
        updatedAt: '2026-01-07T12:00:00Z',
      },
      {
        reservationId: 'res-ijk456lmn789',
        hotelId: 'hotel-3',
        hotelName: 'ReserveOne Beach Resort',
        userId: 'user-110',
        guestName: 'Daniel Garcia',
        roomId: 'room-505',
        roomTypeId: 'suite',
        startDate: '2026-02-10',
        endDate: '2026-02-14',
        guestCount: 4,
        status: 'CONFIRMED',
        totalAmount: 950,
        currency: 'USD',
        createdAt: '2026-01-06T17:00:00Z',
        updatedAt: '2026-01-06T17:00:00Z',
      },
    ];

    this.totalRevenueLast12 = this.monthlyRevenue.reduce((sum, m) => sum + m.revenue, 0);
    this.totalBookingsLast12 = this.monthlyRevenue.reduce((sum, m) => sum + m.bookingCount, 0);
  }

  formatCurrency(amount: number): string {
    const safeAmount = Number.isFinite(amount) ? amount : 0;
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(safeAmount);
  }

  formatDate(dateString: string): string {
    if (!dateString) return '—';
    const d = new Date(dateString);
    if (!Number.isFinite(d.getTime())) return '—';
    return d.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  }

  getStatusClass(status: string): string {
    const statusMap: Record<string, string> = {
      PENDING: 'status-pending',
      CONFIRMED: 'status-confirmed',
      CANCELLED: 'status-cancelled',
      CHECKED_IN: 'status-checked-in',
      CHECKED_OUT: 'status-checked-out',
    };
    return statusMap[status] || 'status-default';
  }

  formatBookingId(id: string): string {
    const s = id ? String(id) : '';
    if (!s) return '—';
    return s.length > 8 ? `${s.substring(0, 8)}...` : s;
  }

  getMaxRevenue(): number {
    if (this.monthlyRevenue.length === 0) return 0;
    return Math.max(
      ...this.monthlyRevenue.map((m) => (Number.isFinite(m?.revenue) ? m.revenue : 0))
    );
  }

  getBarHeight(revenue: number): number {
    const max = this.getMaxRevenue();
    if (max === 0) return 0;
    const safeRevenue = Number.isFinite(revenue) ? revenue : 0;
    return Math.max((safeRevenue / max) * 100, 2);
  }

  toggleNav() {
    this.isNavOpen = !this.isNavOpen;
  }

  closeNav() {
    this.isNavOpen = false;
  }

  openBooking() {}

  openSignIn() {}

  signOut() {
    this.auth.logout().subscribe({
      next: () => {},
      error: () => {},
    });
  }

  isProfileOpen = false;

  openProfile() {
    this.isProfileOpen = true;
    document.body.style.overflow = 'hidden';
  }

  closeProfile() {
    this.isProfileOpen = false;
    document.body.style.overflow = '';
  }
}
