import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { Footer } from '../../../../shared/footer/footer';
import { Header } from '../../../../shared/header/header';
import {
  ReservationResponse,
  ReservationService,
  ReservationStatus,
} from '../../../admin/services/reservation.service';
import { AuthService } from '../../../auth/services/auth.service';
import { ReservationCancelModal } from '../../components/reservation-cancel-modal/reservation-cancel-modal';
import { ReservationModifyModal } from '../../components/reservation-modify-modal/reservation-modify-modal';

@Component({
  selector: 'app-guest-dashboard',
  standalone: true,
  imports: [CommonModule, Header, Footer, ReservationModifyModal, ReservationCancelModal],
  templateUrl: './guest-dashboard.html',
  styleUrl: './guest-dashboard.css',
})
export class GuestDashboard implements OnInit {
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

  reservations: ReservationResponse[] = [];
  loading = false;
  error: string | null = null;
  checkingIn: Record<string, boolean> = {};

  isNavOpen = false;
  showProfileModal = false;
  selectedReservation: ReservationResponse | null = null;
  showModifyModal = false;
  showCancelModal = false;

  ngOnInit(): void {
    // Temporarily disabled backend calls - just show UI
    // this.loadReservations();

    // Mock data for UI preview
    this.reservations = [
      {
        reservationId: 'res-abc123def456',
        hotelId: 'hotel-1',
        userId: 'user-101',
        roomId: 'room-201',
        roomTypeId: 'type-deluxe',
        startDate: '2025-02-15',
        endDate: '2025-02-18',
        guestCount: 2,
        status: 'CONFIRMED',
        totalAmount: 450.0,
        currency: 'USD',
        specialRequests: 'Late check-in requested',
        createdAt: '2025-01-20T10:30:00Z',
        updatedAt: '2025-01-20T10:30:00Z',
      },
      {
        reservationId: 'res-xyz789ghi012',
        hotelId: 'hotel-2',
        userId: 'user-101',
        roomId: 'room-305',
        roomTypeId: 'type-standard',
        startDate: '2025-03-01',
        endDate: '2025-03-03',
        guestCount: 1,
        status: 'CHECKED_IN',
        totalAmount: 280.0,
        currency: 'USD',
        createdAt: '2025-01-15T14:20:00Z',
        updatedAt: '2025-03-01T12:00:00Z',
      },
    ];
  }

  loadReservations(): void {
    // Temporarily disabled - backend integration disabled for preview
    const me = this.auth.meSignal();
    if (!me?.localUserId) {
      this.error = 'You must be logged in to view your bookings.';
      return;
    }

    this.loading = true;
    this.error = null;

    this.reservationService
      .getAllReservations({ userId: me.localUserId })
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (reservations) => {
          // Sort by creation date (most recent first)
          this.reservations = reservations.sort((a, b) => {
            const dateA = new Date(a.createdAt).getTime();
            const dateB = new Date(b.createdAt).getTime();
            return dateB - dateA;
          });
        },
        error: (err) => {
          if (err.error?.detail) {
            this.error = err.error.detail;
          } else if (err.error?.message) {
            this.error = err.error.message;
          } else {
            this.error = 'Failed to load your reservations. Please try again.';
          }
        },
      });
  }

  checkIn(reservation: ReservationResponse): void {
    // Temporarily disabled - backend integration disabled for preview
    // Just update the UI locally for demonstration
    const index = this.reservations.findIndex((r) => r.reservationId === reservation.reservationId);
    if (index !== -1 && this.reservations[index].status === 'CONFIRMED') {
      this.reservations[index] = {
        ...this.reservations[index],
        status: 'CHECKED_IN',
      };
      alert('Checked in successfully! (UI preview mode)');
    }

    // Original backend code (disabled for now):
    /*
    if (this.checkingIn[reservation.reservationId]) return;

    this.checkingIn[reservation.reservationId] = true;

    this.reservationService
      .checkIn(reservation.reservationId)
      .pipe(finalize(() => (this.checkingIn[reservation.reservationId] = false)))
      .subscribe({
        next: (updated) => {
          // Update the reservation in the list
          const index = this.reservations.findIndex(
            (r) => r.reservationId === reservation.reservationId
          );
          if (index !== -1) {
            this.reservations[index] = updated;
          }
        },
        error: (err) => {
          let message = 'Failed to check in. Please try again.';
          if (err.error?.detail) {
            message = err.error.detail;
          } else if (err.error?.message) {
            message = err.error.message;
          }
          alert(message);
        },
      });
    */
  }

  canCheckIn(reservation: ReservationResponse): boolean {
    return reservation.status === 'CONFIRMED';
  }

  getStatusClass(status: ReservationStatus): string {
    const statusMap: Record<ReservationStatus, string> = {
      PENDING: 'status-pending',
      CONFIRMED: 'status-confirmed',
      CHECKED_IN: 'status-checked-in',
      CHECKED_OUT: 'status-checked-out',
      CANCELLED: 'status-cancelled',
    };
    return statusMap[status] || '';
  }

  getStatusLabel(status: ReservationStatus): string {
    const labelMap: Record<ReservationStatus, string> = {
      PENDING: 'Pending',
      CONFIRMED: 'Confirmed',
      CHECKED_IN: 'Checked In',
      CHECKED_OUT: 'Checked Out',
      CANCELLED: 'Cancelled',
    };
    return labelMap[status] || status;
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  }

  formatCurrency(amount: number, currency: string = 'USD'): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency,
    }).format(amount);
  }

  calculateTotalBalance(): number {
    return this.reservations
      .filter((r) => r.status !== 'CANCELLED' && r.status !== 'CHECKED_OUT')
      .reduce((sum, r) => sum + r.totalAmount, 0);
  }

  getTotalStays(): number {
    // Count all non-cancelled reservations
    return this.reservations.filter((r) => r.status !== 'CANCELLED').length;
  }

  getTotalRewardsPoints(): number {
    // Placeholder - rewards system not implemented yet
    // Returns a static value for display purposes
    return 0;
  }

  getHotelLocation(hotelId: string): string {
    // Temporary mapping for mock data - will be replaced with actual hotel service call
    const hotelMap: Record<string, string> = {
      'hotel-1': 'ReserveOne Downtown',
      'hotel-2': 'ReserveOne Airport',
      'hotel-3': 'ReserveOne Beach Resort',
    };
    return hotelMap[hotelId] || hotelId;
  }

  hasBalance(reservation: ReservationResponse): boolean {
    // Show pay button for reservations that need payment (PENDING or CONFIRMED status)
    return (
      (reservation.status === 'PENDING' || reservation.status === 'CONFIRMED') &&
      reservation.totalAmount > 0
    );
  }

  payReservation(reservation: ReservationResponse): void {
    // Navigate to payment page
    this.router.navigate(['/payment', reservation.reservationId]);
  }

  openModifyModal(reservation: ReservationResponse): void {
    this.selectedReservation = reservation;
    this.showModifyModal = true;
  }

  closeModifyModal(): void {
    this.showModifyModal = false;
    this.selectedReservation = null;
  }

  onReservationUpdated(updated: ReservationResponse): void {
    const index = this.reservations.findIndex((r) => r.reservationId === updated.reservationId);
    if (index !== -1) {
      this.reservations[index] = updated;
    }
    this.closeModifyModal();
  }

  openCancelModal(reservation: ReservationResponse): void {
    this.selectedReservation = reservation;
    this.showCancelModal = true;
  }

  closeCancelModal(): void {
    this.showCancelModal = false;
    this.selectedReservation = null;
  }

  onReservationCancelled(cancelled: ReservationResponse): void {
    const index = this.reservations.findIndex((r) => r.reservationId === cancelled.reservationId);
    if (index !== -1) {
      this.reservations[index] = cancelled;
    }
    this.closeCancelModal();
  }

  canModify(reservation: ReservationResponse): boolean {
    // Allow modifications for PENDING and CONFIRMED reservations
    return reservation.status === 'PENDING' || reservation.status === 'CONFIRMED';
  }

  canCancel(reservation: ReservationResponse): boolean {
    // Allow cancellation for PENDING, CONFIRMED, and CHECKED_IN reservations
    return (
      reservation.status === 'PENDING' ||
      reservation.status === 'CONFIRMED' ||
      reservation.status === 'CHECKED_IN'
    );
  }

  toggleNav(): void {
    this.isNavOpen = !this.isNavOpen;
  }

  closeNav(): void {
    this.isNavOpen = false;
  }

  openBooking(): void {
    this.router.navigate(['/']);
  }

  openSignIn(): void {
    this.auth.startGoogleLogin();
  }

  signOut(): void {
    this.auth.logout().subscribe(() => {
      this.router.navigate(['/']);
    });
  }

  openProfile(): void {
    this.router.navigate(['/profile-settings']);
  }

  closeProfile(): void {
    this.showProfileModal = false;
  }

  openSystemSettings(): void {
    // Not applicable for guest dashboard
  }
}
