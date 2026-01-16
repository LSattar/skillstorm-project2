import { ChangeDetectorRef, Component, EventEmitter, Input, Output, OnInit, inject } from '@angular/core';
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
  private readonly cdr = inject(ChangeDetectorRef);

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
      
      // Allow past check-in dates for ongoing reservations (not checked out)
      // This allows guests to extend their stay while currently at the hotel
      if (this.reservation.status !== 'CHECKED_OUT') {
        // Set minDate to a very early date to allow past dates
        this.minDate = '1900-01-01';
      }
    }
  }

  onSave(): void {
    if (!this.validateDates()) {
      this.cdr.detectChanges();
      return;
    }

    if (!this.validateGuestCount()) {
      this.cdr.detectChanges();
      return;
    }

    this.loading = true;
    this.error = null;
    this.cdr.detectChanges();

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
        this.error = null;
        this.cdr.detectChanges();
        this.updated.emit(updated);
        this.onClose();
      },
      error: (err) => {
        this.loading = false;
        
        // Extract error message from various possible formats
        let errorMessage = 'Failed to update reservation. Please try again.';
        
        if (err.error) {
          // Check for detail field (common in FastAPI/Spring Boot)
          if (err.error.detail) {
            if (typeof err.error.detail === 'string') {
              errorMessage = err.error.detail;
            } else if (Array.isArray(err.error.detail)) {
              // Handle validation errors array
              errorMessage = err.error.detail.map((d: any) => d.msg || d.message || JSON.stringify(d)).join(', ');
            } else if (err.error.detail.message) {
              errorMessage = err.error.detail.message;
            }
          } 
          // Check for message field
          else if (err.error.message) {
            errorMessage = err.error.message;
          }
          // Check for error field
          else if (err.error.error) {
            errorMessage = typeof err.error.error === 'string' ? err.error.error : err.error.error.message || errorMessage;
          }
          // Check for validation errors in Spring Boot format
          else if (err.error.errors && Array.isArray(err.error.errors)) {
            errorMessage = err.error.errors.map((e: any) => e.defaultMessage || e.message || JSON.stringify(e)).join(', ');
          }
        }
        
        // Check HTTP status for specific error types
        if (err.status === 400) {
          if (!errorMessage.includes('Failed to update')) {
            // Keep the extracted message
          } else {
            errorMessage = 'Invalid request. Please check your input and try again.';
          }
        } else if (err.status === 403) {
          errorMessage = 'You do not have permission to modify this reservation.';
        } else if (err.status === 404) {
          errorMessage = 'Reservation not found. It may have been cancelled or deleted.';
        } else if (err.status === 409) {
          errorMessage = 'The requested changes conflict with existing reservations. Please try different dates or room.';
        } else if (err.status === 422) {
          errorMessage = errorMessage || 'Validation error. Please check your input values.';
        } else if (err.status >= 500) {
          errorMessage = 'Server error. Please try again later.';
        }
        
        this.error = errorMessage;
        this.cdr.detectChanges();
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
    
    // Allow past check-in dates for ongoing reservations (not checked out)
    // This allows guests to extend their stay while currently at the hotel
    const isCheckedOut = this.reservation?.status === 'CHECKED_OUT';
    
    if (!isCheckedOut) {
      // For ongoing reservations, only validate that check-out is after check-in
      // Don't restrict check-in date to future dates
      return true;
    }
    
    // For checked-out reservations, check-in date must be in the future
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const checkInDate = new Date(this.startDate);
    checkInDate.setHours(0, 0, 0, 0);
    
    if (checkInDate < today) {
      this.error = 'Check-in date cannot be in the past';
      return false;
    }
    
    return true;
  }

  validateGuestCount(): boolean {
    if (!this.guestCount || this.guestCount < 1) {
      this.error = 'Guest count must be at least 1';
      return false;
    }
    
    if (this.guestCount > 20) {
      this.error = 'Guest count cannot exceed 20. Please contact support for larger groups.';
      return false;
    }
    
    return true;
  }

  clearError(): void {
    if (this.error) {
      this.error = null;
      this.cdr.detectChanges();
    }
  }
}
