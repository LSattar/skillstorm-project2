import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, computed, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { Header } from '../../../../shared/header/header';
import { AuthService } from '../../../auth/services/auth.service';

import {
  ReservationResponse,
  ReservationService,
  ReservationStatus,
} from '../../services/reservation.service';
import { ReservationEditModal } from './reservation-edit-modal';
import { forkJoin, map } from 'rxjs';
import { environment } from '../../../../../environments/environment';

@Component({
  selector: 'app-reservation-lookup',
  standalone: true,
  imports: [CommonModule, FormsModule, Header, ReservationEditModal],
  templateUrl: './reservation-lookup.html',
  styleUrl: './reservation-lookup.css',
})
export class ReservationLookup implements OnInit {
  protected readonly auth = inject(AuthService);
  protected readonly reservationService = inject(ReservationService);
  protected readonly router = inject(Router);
  protected readonly http = inject(HttpClient);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly api = environment.apiBaseUrl;

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

  // Search form
  searchReservationId = '';
  searchGuestLastName = '';
  searchHotelId = '';
  searchStatus: ReservationStatus | '' = '';
  searchStartDate = '';
  searchEndDate = '';

  // Hotels for dropdown
  hotels: Array<{ hotelId: string; name: string }> = [];

  // Results
  filteredReservations: (ReservationResponse & { hotelName?: string; guestName?: string })[] = [];
  loading = false;
  error: string | null = null;
  selectedReservation: ReservationResponse | null = null;
  showEditModal = false;

  // Helper maps for hotel and guest names
  private hotelNames: Record<string, string> = {};
  private guestNames: Record<string, string> = {};

  isNavOpen = false;
  today = new Date();

  statusOptions: ReservationStatus[] = [
    'PENDING',
    'CONFIRMED',
    'CANCELLED',
    'CHECKED_IN',
    'CHECKED_OUT',
  ];

  ngOnInit(): void {
    this.loadHotels();
  }

  loadHotels(): void {
    this.http.get<Array<{ hotelId: string; name: string }>>(`${this.api}/hotels`, {
      withCredentials: true,
    }).subscribe({
      next: (hotels) => {
        this.hotels = hotels;
        // Build hotel name map
        this.hotelNames = {};
        hotels.forEach((hotel) => {
          this.hotelNames[hotel.hotelId] = hotel.name;
        });
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error loading hotels:', err);
        this.hotels = [];
        this.hotelNames = {};
        this.cdr.detectChanges();
      },
    });
  }

  onSearch(): void {
    this.loadReservations();
  }

  loadReservations(): void {
    this.loading = true;
    this.error = null;

    // Prepare search parameters
    const searchParams: {
      reservationId?: string;
      guestLastName?: string;
      hotelId?: string;
      status?: ReservationStatus | '';
      startDateFrom?: string;
      endDateTo?: string;
    } = {};

    if (this.searchReservationId.trim()) {
      searchParams.reservationId = this.searchReservationId.trim();
    }
    if (this.searchGuestLastName.trim()) {
      searchParams.guestLastName = this.searchGuestLastName.trim();
    }
    if (this.searchHotelId) {
      searchParams.hotelId = this.searchHotelId;
    }
    if (this.searchStatus) {
      searchParams.status = this.searchStatus;
    }
    if (this.searchStartDate) {
      // Ensure date is in YYYY-MM-DD format
      searchParams.startDateFrom = this.searchStartDate;
    }
    if (this.searchEndDate) {
      // Ensure date is in YYYY-MM-DD format
      searchParams.endDateTo = this.searchEndDate;
    }

    // Load reservations with filters and users for name mapping
    forkJoin({
      reservations: this.reservationService.searchReservations(searchParams),
      users: this.http.get<Array<{ userId: string; firstName?: string; lastName?: string; email: string }>>(`${this.api}/users/search?q=&limit=1000`, {
        withCredentials: true,
      }),
    }).subscribe({
      next: (data) => {
        // Build guest name map
        this.guestNames = {};
        data.users.forEach((user) => {
          const name = user.firstName && user.lastName
            ? `${user.firstName} ${user.lastName}`
            : user.firstName || user.email || user.userId;
          this.guestNames[user.userId] = name;
        });

        // Enrich reservations with hotel and guest names
        this.filteredReservations = data.reservations.map((reservation) => ({
          ...reservation,
          hotelName: this.hotelNames[reservation.hotelId] || reservation.hotelId,
          guestName: this.guestNames[reservation.userId] || reservation.userId,
        }));

        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error loading reservations:', err);
        this.error = 'Failed to load reservations. Please try again.';
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  clearFilters(): void {
    this.searchReservationId = '';
    this.searchGuestLastName = '';
    this.searchHotelId = '';
    this.searchStatus = '';
    this.searchStartDate = '';
    this.searchEndDate = '';
    this.filteredReservations = [];
  }

  openEditModal(reservation: ReservationResponse): void {
    this.selectedReservation = { ...reservation };
    this.showEditModal = true;
  }

  closeEditModal(): void {
    this.showEditModal = false;
    this.selectedReservation = null;
  }

  onReservationUpdated(updatedReservation?: ReservationResponse): void {
    this.closeEditModal();
    // Reload reservations with current filters to ensure consistency
    this.loadReservations();
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
