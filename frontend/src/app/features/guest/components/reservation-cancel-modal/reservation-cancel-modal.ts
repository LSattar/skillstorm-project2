import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReservationResponse, ReservationService } from '../../../admin/services/reservation.service';

@Component({
  selector: 'app-reservation-cancel-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reservation-cancel-modal.html',
  styleUrl: './reservation-cancel-modal.css',
})
export class ReservationCancelModal {
  @Input() reservation!: ReservationResponse;
  @Output() close = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<ReservationResponse>();

  private readonly reservationService = inject(ReservationService);

  cancelReason = '';
  loading = false;
  error: string | null = null;

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

  onCancel(): void {
    this.loading = true;
    this.error = null;

    this.reservationService.cancelReservation(this.reservation.reservationId, this.cancelReason || undefined).subscribe({
      next: (cancelled) => {
        this.loading = false;
        this.cancelled.emit(cancelled);
        this.onClose();
      },
      error: (err) => {
        this.loading = false;
        if (err.error?.detail) {
          this.error = err.error.detail;
        } else if (err.error?.message) {
          this.error = err.error.message;
        } else {
          this.error = 'Failed to cancel reservation. Please try again.';
        }
      },
    });
  }

  onClose(): void {
    this.close.emit();
  }
}
