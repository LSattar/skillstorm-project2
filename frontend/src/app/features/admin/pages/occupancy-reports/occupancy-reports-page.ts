import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { Header } from '../../../../shared/header/header';
import { AuthService } from '../../../auth/services/auth.service';
import { AdminMetricsService } from '../../services/admin-metrics.service';
import { HttpClient } from '@angular/common/http';
import { forkJoin, map } from 'rxjs';
import { environment } from '../../../../../environments/environment';

export type OccupancyData = {
  date: string;
  occupiedRooms: number;
  totalRooms: number;
  occupancyRate: number;
  checkIns: number;
  checkOuts: number;
};

@Component({
  selector: 'app-occupancy-reports-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, Header],
  templateUrl: './occupancy-reports-page.html',
  styleUrls: ['./occupancy-reports-page.css'],
})
export class OccupancyReportsPage implements OnInit {
  protected readonly auth = inject(AuthService);
  protected readonly router = inject(Router);
  protected readonly adminMetricsService = inject(AdminMetricsService);
  protected readonly http = inject(HttpClient);
  private readonly api = environment.apiBaseUrl;

  // Header bindings
  get isAuthenticated() {
    return this.auth.isAuthenticated();
  }
  get roleLabel() {
    return this.auth.primaryRoleLabel();
  }
  get userLabel() {
    const me = this.auth.meSignal();
    if (!me) return '';
    const first = me.firstName?.trim();
    if (first) return first;
    return me.email || '';
  }
  get userEmail() {
    return this.auth.meSignal()?.email ?? '';
  }

  isNavOpen = false;
  today = new Date();

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
  openProfile() {
    this.router.navigate(['/profile-settings']);
  }

  // Filters
  fromDate = '';
  toDate = '';
  selectedHotelId = '';

  // Data
  loading = false;
  error: string | null = null;
  hotels: Array<{ hotelId: string; name: string }> = [];
  occupancyData: OccupancyData[] = [];
  currentMetrics: {
    totalRooms: number;
    occupiedRooms: number;
    occupancyRate: number;
  } | null = null;

  // Summary stats
  averageOccupancyRate = 0;
  peakOccupancyDate = '';
  peakOccupancyRate = 0;
  totalCheckIns = 0;
  totalCheckOuts = 0;

  ngOnInit(): void {
    // Set default date range (last 30 days)
    const to = new Date();
    const from = new Date();
    from.setDate(from.getDate() - 30);
    this.toDate = to.toISOString().slice(0, 10);
    this.fromDate = from.toISOString().slice(0, 10);

    this.loadHotels();
    this.loadData();
  }

  loadHotels(): void {
    this.http
      .get<Array<{ hotelId: string; name: string }>>(`${this.api}/hotels`, {
        withCredentials: true,
      })
      .subscribe({
        next: (hotels) => {
          this.hotels = hotels;
        },
        error: (err) => {
          console.error('Error loading hotels:', err);
        },
      });
  }

  loadData(): void {
    if (!this.fromDate || !this.toDate) {
      return;
    }

    this.loading = true;
    this.error = null;

    const hotelId = this.selectedHotelId || undefined;
    const startDate = this.fromDate;
    const endDate = this.toDate;

    forkJoin({
      occupancyReport: this.http.get<{
        dailyData: Array<{
          date: string;
          occupiedRooms: number;
          totalRooms: number;
          occupancyRate: number;
          checkIns: number;
          checkOuts: number;
        }>;
        averageOccupancyRate: number;
        peakOccupancyRate: number;
        peakOccupancyDate: string;
        totalCheckIns: number;
        totalCheckOuts: number;
      }>(`${this.api}/admin/metrics/occupancy-report?${hotelId ? `hotelId=${encodeURIComponent(hotelId)}&` : ''}startDate=${startDate}&endDate=${endDate}`, {
        withCredentials: true,
      }),
      metrics: this.selectedHotelId
        ? this.adminMetricsService.getOperationalMetrics(this.selectedHotelId)
        : this.adminMetricsService.getOperationalMetrics(),
    }).subscribe({
      next: (data) => {
        this.currentMetrics = {
          totalRooms: data.metrics.totalRooms,
          occupiedRooms: data.metrics.occupiedRooms,
          occupancyRate: data.metrics.occupancyRate,
        };

        // Convert backend data to frontend format
        this.occupancyData = data.occupancyReport.dailyData.map((d) => ({
          date: d.date,
          occupiedRooms: d.occupiedRooms,
          totalRooms: d.totalRooms,
          occupancyRate: d.occupancyRate,
          checkIns: d.checkIns,
          checkOuts: d.checkOuts,
        }));

        this.averageOccupancyRate = data.occupancyReport.averageOccupancyRate;
        this.peakOccupancyRate = data.occupancyReport.peakOccupancyRate;
        this.peakOccupancyDate = data.occupancyReport.peakOccupancyDate;
        this.totalCheckIns = data.occupancyReport.totalCheckIns;
        this.totalCheckOuts = data.occupancyReport.totalCheckOuts;

        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading occupancy data:', err);
        this.error = 'Failed to load occupancy data. Please try again.';
        this.loading = false;
      },
    });
  }


  onDateRangeChange(): void {
    if (this.fromDate && this.toDate) {
      this.loadData();
    }
  }

  onHotelChange(): void {
    this.loadData();
  }

  setDatePreset(preset: 'last7' | 'last30' | 'last90' | 'thisMonth'): void {
    const now = new Date();
    let from = new Date();

    if (preset === 'last7') {
      from.setDate(now.getDate() - 6);
    } else if (preset === 'last30') {
      from.setDate(now.getDate() - 29);
    } else if (preset === 'last90') {
      from.setDate(now.getDate() - 89);
    } else if (preset === 'thisMonth') {
      from = new Date(now.getFullYear(), now.getMonth(), 1);
    }

    this.fromDate = from.toISOString().slice(0, 10);
    this.toDate = now.toISOString().slice(0, 10);
    this.onDateRangeChange();
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

  formatPercent(value: number): string {
    return `${value.toFixed(1)}%`;
  }

  getMaxOccupancyRate(): number {
    if (this.occupancyData.length === 0) return 100;
    return Math.max(...this.occupancyData.map((d) => d.occupancyRate), 0);
  }

  getBarHeight(rate: number): number {
    const max = this.getMaxOccupancyRate();
    if (max === 0) return 0;
    return Math.max((rate / max) * 100, 2);
  }
}
