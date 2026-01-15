import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { MonthlyRevenue, Alert, Reservation, AdminMetricsService } from '../../services/admin-metrics.service';
import { Header } from '../../../../shared/header/header';
import { AuthService } from '../../../auth/services/auth.service';
import { HttpClient } from '@angular/common/http';
import { forkJoin, map } from 'rxjs';
import { environment } from '../../../../../environments/environment';

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
  imports: [CommonModule, RouterModule, Header],
  templateUrl: './admin-dashboard.html',
  styleUrl: './admin-dashboard.css',
})
export class AdminDashboard implements OnInit {
  protected readonly auth = inject(AuthService);
  protected readonly router = inject(Router);
  protected readonly adminMetricsService = inject(AdminMetricsService);
  protected readonly http = inject(HttpClient);
  private readonly api = environment.apiBaseUrl;

  goToSystemSettings() {
    this.router.navigate(['/admin/system-settings']);
  }

  // Show Payment Transactions in header if admin
  get showPaymentTransactions(): boolean {
    return this.auth.isAdmin();
  }

  // Navigate to payment transactions page
  goToPaymentTransactions(): void {
    this.router.navigate(['/admin/payment-transactions']);
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
    // Scroll to fragment if present
    this.router.events.subscribe((event: any) => {
      if (event?.constructor?.name === 'NavigationEnd') {
        const fragment = this.router.parseUrl(this.router.url).fragment;
        if (fragment) {
          setTimeout(() => {
            const el = document.getElementById(fragment);
            if (el) {
              el.scrollIntoView({ behavior: 'smooth', block: 'start' });
            }
          }, 100);
        }
      }
    });
  }

  loadDashboardData(): void {
    this.loading = true;
    this.error = null;

    forkJoin({
      operationalMetrics: this.adminMetricsService.getOperationalMetrics(),
      monthlyRevenue: this.adminMetricsService.getMonthlyRevenue(),
      alerts: this.adminMetricsService.getAlerts(),
      recentBookings: this.adminMetricsService.getRecentBookings(10),
      hotels: this.http.get<Array<{ hotelId: string; name: string }>>(`${this.api}/hotels`, {
        withCredentials: true,
      }).pipe(
        map((hotels) => {
          const hotelMap: Record<string, string> = {};
          hotels.forEach((hotel) => {
            hotelMap[hotel.hotelId] = hotel.name;
          });
          return hotelMap;
        })
      ),
      users: this.http.get<Array<{ userId: string; firstName?: string; lastName?: string; email: string }>>(`${this.api}/users/search?q=&limit=1000`, {
        withCredentials: true,
      }).pipe(
        map((users) => {
          const userMap: Record<string, string> = {};
          users.forEach((user) => {
            const name = user.firstName && user.lastName
              ? `${user.firstName} ${user.lastName}`
              : user.firstName || user.email || user.userId;
            userMap[user.userId] = name;
          });
          return userMap;
        })
      ),
    }).subscribe({
      next: (data) => {
        this.operationalMetrics = data.operationalMetrics;
        this.monthlyRevenue = data.monthlyRevenue;
        this.alerts = data.alerts;
        
        // Enrich bookings with hotel and guest names
        this.recentBookings = data.recentBookings.map((booking) => ({
          ...booking,
          hotelName: data.hotels[booking.hotelId] || booking.hotelId,
          guestName: data.users[booking.userId] || booking.userId,
        }));
        
        this.totalRevenueLast12 = this.monthlyRevenue.reduce((sum, m) => sum + m.revenue, 0);
        this.totalBookingsLast12 = this.monthlyRevenue.reduce((sum, m) => sum + m.bookingCount, 0);
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading dashboard data:', err);
        this.error = 'Failed to load dashboard data. Please try again.';
        this.loading = false;
      },
    });
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
      next: () => {
        localStorage.clear();
        sessionStorage.clear();
        this.router.navigate(['/']);
      },
      error: () => {
        localStorage.clear();
        sessionStorage.clear();
        this.router.navigate(['/']);
      },
    });
  }

  isProfileOpen = false;

  openProfile() {
    this.router.navigate(['/profile-settings']);
  }

  closeProfile() {
    this.isProfileOpen = false;
    document.body.style.overflow = '';
  }
}
