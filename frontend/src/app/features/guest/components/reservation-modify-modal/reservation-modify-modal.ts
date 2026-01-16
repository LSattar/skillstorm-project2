import { ChangeDetectorRef, Component, EventEmitter, Input, Output, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged, switchMap, catchError } from 'rxjs/operators';
import { of, Subject } from 'rxjs';
import { ReservationResponse, ReservationService, ReservationRequest } from '../../../admin/services/reservation.service';
import { RoomSearchService } from '../../../landing/services/room-search.service';
import { RoomTypeService } from '../../../landing/services/room-type.service';

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
  private readonly roomSearchService = inject(RoomSearchService);
  private readonly roomTypeService = inject(RoomTypeService);
  private readonly cdr = inject(ChangeDetectorRef);

  startDate = '';
  endDate = '';
  guestCount = 1;
  specialRequests = '';

  loading = false;
  checkingAvailability = false;
  calculatingPrice = false;
  error: string | null = null;
  availabilityMessage: string | null = null;
  priceInfo: { originalPrice: number; newPrice: number; nights: number } | null = null;
  roomTypePrice: number = 0;

  minDate = new Date().toISOString().split('T')[0];
  
  private dateChangeSubject = new Subject<void>();

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

      // Load room type to get base price
      this.loadRoomTypePrice();

      // Set up debounced availability and price checking
      this.dateChangeSubject.pipe(
        debounceTime(500),
        distinctUntilChanged(),
        switchMap(() => {
          if (this.startDate && this.endDate && new Date(this.startDate) < new Date(this.endDate)) {
            this.checkAvailabilityAndPrice();
          } else {
            this.availabilityMessage = null;
            this.priceInfo = null;
          }
          return of(null);
        })
      ).subscribe();
    }
  }

  onDateChange(): void {
    this.clearError();
    this.dateChangeSubject.next();
  }

  private loadRoomTypePrice(): void {
    if (!this.reservation?.roomTypeId) return;

    this.roomTypeService.getRoomTypeById(this.reservation.roomTypeId).subscribe({
      next: (roomType) => {
        this.roomTypePrice = roomType.basePrice;
        this.calculatePrice();
        this.cdr.detectChanges();
      },
      error: () => {
        // If we can't load room type, use original price
        this.roomTypePrice = this.reservation.totalAmount / this.getNights(this.reservation.startDate, this.reservation.endDate);
        this.calculatePrice();
        this.cdr.detectChanges();
      },
    });
  }

  private checkAvailabilityAndPrice(): void {
    if (!this.startDate || !this.endDate || !this.reservation) return;

    // Validate dates first
    if (new Date(this.startDate) >= new Date(this.endDate)) {
      this.availabilityMessage = null;
      this.priceInfo = null;
      return;
    }

    this.checkingAvailability = true;
    this.calculatingPrice = true;
    this.availabilityMessage = null;
    this.cdr.detectChanges();

    // Check if the specific room is available for the new dates
    // We need to check if the room (excluding current reservation) is available
    this.roomSearchService.searchAvailableRooms({
      hotelId: this.reservation.hotelId,
      roomTypeId: this.reservation.roomTypeId,
      startDate: this.startDate,
      endDate: this.endDate,
      guestCount: this.guestCount,
    }).pipe(
      catchError(() => of([]))
    ).subscribe({
      next: (availableRooms) => {
        this.checkingAvailability = false;
        
        // Check if the current room is in the available rooms list
        const currentRoomAvailable = availableRooms.some(room => room.roomId === this.reservation.roomId);
        
        // Also check if any room of the same type is available (for flexibility)
        const roomTypeAvailable = availableRooms.length > 0;

        if (currentRoomAvailable) {
          this.availabilityMessage = 'Room is available for the selected dates.';
        } else if (roomTypeAvailable) {
          this.availabilityMessage = 'Room type is available, but your current room may not be. The system will assign an available room.';
        } else {
          this.availabilityMessage = 'No rooms of this type are available for the selected dates. Please try different dates.';
        }

        // Calculate price
        this.calculatePrice();
        this.cdr.detectChanges();
      },
      error: () => {
        this.checkingAvailability = false;
        this.availabilityMessage = 'Unable to verify availability. Please proceed with caution.';
        this.calculatePrice();
        this.cdr.detectChanges();
      },
    });
  }

  private calculatePrice(): void {
    if (!this.startDate || !this.endDate || !this.reservation || this.roomTypePrice === 0) {
      this.priceInfo = null;
      this.calculatingPrice = false;
      return;
    }

    const nights = this.getNights(this.startDate, this.endDate);
    const originalNights = this.getNights(this.reservation.startDate, this.reservation.endDate);
    const originalPrice = this.reservation.totalAmount;
    const newPrice = Math.round(this.roomTypePrice * nights * 100) / 100;

    this.priceInfo = {
      originalPrice,
      newPrice,
      nights,
    };

    this.calculatingPrice = false;
    this.cdr.detectChanges();
  }

  private getNights(startDate: string, endDate: string): number {
    const start = new Date(startDate);
    const end = new Date(endDate);
    return Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24));
  }

  protected formatPrice(price: number): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: this.reservation?.currency || 'USD',
    }).format(price);
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

    // Use the calculated new price if available, otherwise keep original
    const newTotalAmount = this.priceInfo?.newPrice ?? this.reservation.totalAmount;

    const updateRequest: ReservationRequest = {
      hotelId: this.reservation.hotelId,
      userId: this.reservation.userId,
      roomId: this.reservation.roomId,
      roomTypeId: this.reservation.roomTypeId,
      startDate: this.startDate,
      endDate: this.endDate,
      guestCount: this.guestCount,
      totalAmount: newTotalAmount,
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
