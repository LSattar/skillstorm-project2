import { Component, EventEmitter, Input, Output, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReservationResponse, ReservationService, ReservationRequest } from '../../../admin/services/reservation.service';

@Component({
  selector: 'app-reservation-modify-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reservation-modify-modal.html',
  styleUrl: './reservation-modify-modal.css',
})
export class ReservationModifyModal implements OnInit {
  @Input() reservation!: ReservationResponse;
  @Output() close = new EventEmitter<void>();
  @Output() updated = new EventEmitter<ReservationResponse>();

  private readonly reservationService = inject(ReservationService);

  startDate = '';
  endDate = '';
  guestCount = 1;
  specialRequests = '';

  loading = false;
  error: string | null = null;

  minDate = new Date().toISOString().split('T')[0];

  ngOnInit(): void {
    if (this.reservation) {
      this.startDate = this.reservation.startDate;
      this.endDate = this.reservation.endDate;
      this.guestCount = this.reservation.guestCount;
      this.specialRequests = this.reservation.specialRequests || '';
    }
  }

  onSave(): void {
    if (!this.validateDates()) {
      return;
    }

    this.loading = true;
    this.error = null;

    const updateRequest: ReservationRequest = {
      hotelId: this.reservation.hotelId,
      userId: this.reservation.userId,
      roomId: this.reservation.roomId,
      roomTypeId: this.reservation.roomTypeId,
      startDate: this.startDate,
      endDate: this.endDate,
      guestCount: this.guestCount,
      totalAmount: this.reservation.totalAmount,
      currency: this.reservation.currency,
      specialRequests: this.specialRequests || undefined,
    };

    this.reservationService.updateReservation(this.reservation.reservationId, updateRequest).subscribe({
      next: (updated) => {
        this.loading = false;
        this.updated.emit(updated);
        this.onClose();
      },
      error: (err) => {
        this.loading = false;
        if (err.error?.detail) {
          this.error = err.error.detail;
        } else if (err.error?.message) {
          this.error = err.error.message;
        } else {
          this.error = 'Failed to update reservation. Please try again.';
        }
      },
    });
  }

  onClose(): void {
    this.close.emit();
  }

  validateDates(): boolean {
    if (new Date(this.startDate) >= new Date(this.endDate)) {
      this.error = 'Check-out date must be after check-in date';
      return false;
    }
    return true;
  }
}
