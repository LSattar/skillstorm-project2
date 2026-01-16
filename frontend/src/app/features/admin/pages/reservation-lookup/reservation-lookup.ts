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
  searchHotelName = '';
  searchStatus: ReservationStatus | '' = '';
  searchStartDate = '';
  searchEndDate = '';

  // Results
  reservations: (ReservationResponse & { hotelName?: string; guestName?: string })[] = [];
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
    this.loadReservations();
  }

  loadReservations(): void {
    this.loading = true;
    this.error = null;

    forkJoin({
      reservations: this.reservationService.getAllReservations(),
      hotels: this.http.get<Array<{ hotelId: string; name: string }>>(`${this.api}/hotels`, {
        withCredentials: true,
      }),
      users: this.http.get<Array<{ userId: string; firstName?: string; lastName?: string; email: string }>>(`${this.api}/users/search?q=&limit=1000`, {
        withCredentials: true,
      }),
    }).subscribe({
      next: (data) => {
        // Build hotel name map
        this.hotelNames = {};
        data.hotels.forEach((hotel) => {
          this.hotelNames[hotel.hotelId] = hotel.name;
        });

        // Build guest name map
        this.guestNames = {};
        data.users.forEach((user) => {
          const name = user.firstName && user.lastName
            ? `${user.firstName} ${user.lastName}`
            : user.firstName || user.email || user.userId;
          this.guestNames[user.userId] = name;
        });

        // Enrich reservations with hotel and guest names
        this.reservations = data.reservations.map((reservation) => ({
          ...reservation,
          hotelName: this.hotelNames[reservation.hotelId] || reservation.hotelId,
          guestName: this.guestNames[reservation.userId] || reservation.userId,
        }));

        // Apply filters
        this.applyFilters();
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

  onSearch(): void {
    // Reapply filters on search
    this.applyFilters();
  }

  applyFilters(): void {
    let filtered = [...this.reservations];

    // Filter by reservation ID
    if (this.searchReservationId.trim()) {
      const searchId = this.searchReservationId.trim().toLowerCase();
      filtered = filtered.filter((r) => r.reservationId.toLowerCase().includes(searchId));
    }

    // Filter by guest last name
    if (this.searchGuestLastName.trim()) {
      const searchLastName = this.searchGuestLastName.trim().toLowerCase();
      filtered = filtered.filter((r) => {
        if (!r.guestName) return false;
        // Extract last name from full name (assuming format "FirstName LastName")
        const parts = r.guestName.trim().split(/\s+/);
        const lastName = parts.length > 1 ? parts[parts.length - 1] : parts[0];
        return lastName.toLowerCase().includes(searchLastName);
      });
    }

    // Filter by hotel name
    if (this.searchHotelName.trim()) {
      const searchHotelName = this.searchHotelName.trim().toLowerCase();
      filtered = filtered.filter((r) => {
        if (!r.hotelName) return false;
        return r.hotelName.toLowerCase().includes(searchHotelName);
      });
    }

    // Filter by status
    if (this.searchStatus) {
      filtered = filtered.filter((r) => r.status === this.searchStatus);
    }

    // Filter by date range
    if (this.searchStartDate) {
      const startDate = new Date(this.searchStartDate);
      filtered = filtered.filter((r) => {
        const rStartDate = new Date(r.startDate);
        return rStartDate >= startDate;
      });
    }

    if (this.searchEndDate) {
      const endDate = new Date(this.searchEndDate);
      filtered = filtered.filter((r) => {
        const rEndDate = new Date(r.endDate);
        return rEndDate <= endDate;
      });
    }

    this.filteredReservations = filtered;
    this.cdr.detectChanges();
  }

  clearFilters(): void {
    this.searchReservationId = '';
    this.searchGuestLastName = '';
    this.searchHotelName = '';
    this.searchStatus = '';
    this.searchStartDate = '';
    this.searchEndDate = '';
    this.applyFilters();
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
    if (updatedReservation && this.selectedReservation) {
      // Update the reservation in the list
      const index = this.reservations.findIndex(
        (r) => r.reservationId === this.selectedReservation?.reservationId
      );
      if (index !== -1) {
        // Preserve hotel and guest names
        this.reservations[index] = {
          ...updatedReservation,
          hotelName: this.hotelNames[updatedReservation.hotelId] || updatedReservation.hotelId,
          guestName: this.guestNames[updatedReservation.userId] || updatedReservation.userId,
        };
      }
    }
    this.closeEditModal();
    // Reapply filters to show updated data
    this.applyFilters();
    this.cdr.detectChanges();
    // Optionally reload all reservations to ensure consistency
    // this.loadReservations();
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
