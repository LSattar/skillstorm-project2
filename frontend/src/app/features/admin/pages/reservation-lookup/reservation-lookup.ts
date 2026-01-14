import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Footer } from '../../../../shared/footer/footer';
import { Header } from '../../../../shared/header/header';
import { AuthService } from '../../../auth/services/auth.service';

import {
  ReservationResponse,
  ReservationService,
  ReservationStatus,
} from '../../services/reservation.service';
import { ReservationEditModal } from './reservation-edit-modal';

@Component({
  selector: 'app-reservation-lookup',
  standalone: true,
  imports: [CommonModule, FormsModule, Header, Footer, ReservationEditModal],
  templateUrl: './reservation-lookup.html',
  styleUrl: './reservation-lookup.css',
})
export class ReservationLookup implements OnInit {
  protected readonly auth = inject(AuthService);
  protected readonly reservationService = inject(ReservationService);
  protected readonly router = inject(Router);

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
  private hotelNames: Record<string, string> = {
    'hotel-1': 'ReserveOne Downtown',
    'hotel-2': 'ReserveOne Airport',
    'hotel-3': 'ReserveOne Beach Resort',
  };

  private guestNames: Record<string, string> = {
    'user-101': 'Sarah Johnson',
    'user-102': 'Michael Chen',
    'user-103': 'Emily Rodriguez',
    'user-104': 'David Kim',
    'user-105': 'Jessica Taylor',
    'user-106': 'Robert Williams',
    'user-107': 'Amanda Martinez',
    'user-108': 'Christopher Lee',
    'user-109': 'Jennifer Brown',
    'user-110': 'Daniel Garcia',
    'user-111': 'Lisa Anderson',
  };

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
    this.loadMockData();
  }

  loadMockData(): void {
    this.loading = true;
    this.error = null;

    // Simulate API delay
    setTimeout(() => {
      const rawReservations: ReservationResponse[] = [
        {
          reservationId: 'res-abc123def456',
          hotelId: 'hotel-1',
          userId: 'user-101',
          roomId: 'room-205',
          roomTypeId: 'deluxe-king',
          startDate: '2026-01-15',
          endDate: '2026-01-18',
          guestCount: 2,
          status: 'CONFIRMED',
          totalAmount: 450,
          currency: 'USD',
          specialRequests: 'Late check-in requested',
          createdAt: '2026-01-05T10:30:00Z',
          updatedAt: '2026-01-05T10:30:00Z',
        },
        {
          reservationId: 'res-xyz789ghi012',
          hotelId: 'hotel-2',
          userId: 'user-102',
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
          userId: 'user-103',
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
          userId: 'user-104',
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
          userId: 'user-105',
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
          userId: 'user-106',
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
          userId: 'user-107',
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
          userId: 'user-108',
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
          userId: 'user-109',
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
          userId: 'user-110',
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
        {
          reservationId: 'res-cancelled123',
          hotelId: 'hotel-1',
          userId: 'user-111',
          roomId: 'room-301',
          roomTypeId: 'deluxe-king',
          startDate: '2026-01-22',
          endDate: '2026-01-24',
          guestCount: 2,
          status: 'CANCELLED',
          totalAmount: 420,
          currency: 'USD',
          cancellationReason: 'Guest requested cancellation',
          cancelledAt: '2026-01-07T14:30:00Z',
          createdAt: '2026-01-05T16:20:00Z',
          updatedAt: '2026-01-07T14:30:00Z',
        },
      ];

      // Add hotel names and guest names to reservations
      this.reservations = rawReservations.map((reservation) => ({
        ...reservation,
        hotelName: this.hotelNames[reservation.hotelId] || reservation.hotelId,
        guestName: this.guestNames[reservation.userId] || reservation.userId,
      }));

      // Apply filters based on search criteria
      this.applyFilters();
      this.loading = false;
    }, 500);
  }

  onSearch(): void {
    // For mock data, just reapply filters
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
      // Update the reservation in the mock data
      const index = this.reservations.findIndex(
        (r) => r.reservationId === this.selectedReservation?.reservationId
      );
      if (index !== -1) {
        this.reservations[index] = updatedReservation;
      }
    }
    this.closeEditModal();
    // Reapply filters to show updated data
    this.applyFilters();
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
