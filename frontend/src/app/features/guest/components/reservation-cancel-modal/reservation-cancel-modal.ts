import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReservationResponse } from '../../../admin/services/reservation.service';

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

    // Temporarily disabled - backend integration disabled for preview
    // Just emit cancelled reservation
    setTimeout(() => {
      const cancelledReservation: ReservationResponse = {
        ...this.reservation,
        status: 'CANCELLED',
        cancellationReason: this.cancelReason || undefined,
        cancelledAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };

      this.loading = false;
      this.cancelled.emit(cancelledReservation);
      this.onClose();
    }, 500);
  }

  onClose(): void {
    this.close.emit();
  }
}
