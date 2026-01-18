import { Component, EventEmitter, Input, Output, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  ReservationService,
  ReservationResponse,
  ReservationRequest,
  ReservationStatus,
} from '../../services/reservation.service';

@Component({
  selector: 'app-reservation-edit-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reservation-edit-modal.html',
  styleUrl: './reservation-edit-modal.css',
})
export class ReservationEditModal implements OnInit {
  @Input() reservation!: ReservationResponse & { hotelName?: string; guestName?: string };
  @Output() close = new EventEmitter<void>();
  @Output() updated = new EventEmitter<ReservationResponse>();

  private readonly reservationService = inject(ReservationService);

  editForm: ReservationRequest = {
    hotelId: '',
    userId: '',
    roomId: '',
    roomTypeId: '',
    startDate: '',
    endDate: '',
    guestCount: 1,
    status: 'PENDING',
    totalAmount: 0,
    currency: 'USD',
    specialRequests: '',
  };

  loading = false;
  error: string | null = null;
  showCancelConfirm = false;
  cancelReason = '';

  statusOptions: ReservationStatus[] = ['PENDING', 'CONFIRMED', 'CANCELLED', 'CHECKED_IN', 'CHECKED_OUT'];

  ngOnInit(): void {
    if (this.reservation) {
      // Initialize form with all required fields (some are read-only but still sent to backend)
      this.editForm = {
        hotelId: this.reservation.hotelId,
        userId: this.reservation.userId,
        roomId: this.reservation.roomId,
        roomTypeId: this.reservation.roomTypeId,
        startDate: this.reservation.startDate,
        endDate: this.reservation.endDate,
        guestCount: this.reservation.guestCount,
        status: this.reservation.status,
        totalAmount: this.reservation.totalAmount,
        currency: this.reservation.currency || 'USD',
        specialRequests: this.reservation.specialRequests || '',
      };
    }
  }

  onClose(): void {
    this.close.emit();
  }

  onSave(): void {
    if (!this.reservation) return;

    this.loading = true;
    this.error = null;

    // Update reservation via API
    this.reservationService.updateReservation(this.reservation.reservationId, this.editForm).subscribe({
      next: (updatedReservation) => {
        this.loading = false;
        this.updated.emit(updatedReservation);
      },
      error: (err) => {
        console.error('Error updating reservation:', err);
        this.error = 'Failed to update reservation. Please try again.';
        this.loading = false;
      },
    });
  }

  onCancelReservation(): void {
    if (!this.reservation) return;

    this.loading = true;
    this.error = null;

    // Cancel reservation via API
    this.reservationService.cancelReservation(
      this.reservation.reservationId,
      this.cancelReason || undefined
    ).subscribe({
      next: (cancelledReservation) => {
        this.loading = false;
        this.updated.emit(cancelledReservation);
        this.showCancelConfirm = false;
        this.cancelReason = '';
      },
      error: (err) => {
        console.error('Error cancelling reservation:', err);
        this.error = 'Failed to cancel reservation. Please try again.';
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
}
