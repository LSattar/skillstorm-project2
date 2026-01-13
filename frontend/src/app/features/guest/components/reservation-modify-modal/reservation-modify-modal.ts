import { Component, EventEmitter, Input, Output, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReservationResponse } from '../../../admin/services/reservation.service';

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

    // Temporarily disabled - backend integration disabled for preview
    // Just emit updated reservation with modified fields
    setTimeout(() => {
      const updatedReservation: ReservationResponse = {
        ...this.reservation,
        startDate: this.startDate,
        endDate: this.endDate,
        guestCount: this.guestCount,
        specialRequests: this.specialRequests || undefined,
        updatedAt: new Date().toISOString(),
      };

      this.loading = false;
      this.updated.emit(updatedReservation);
      this.onClose();
    }, 500);
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
