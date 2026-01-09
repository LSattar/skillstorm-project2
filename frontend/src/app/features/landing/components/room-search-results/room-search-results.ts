import { CommonModule } from '@angular/common';
import {
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  inject,
} from '@angular/core';
import { finalize } from 'rxjs/operators';
import { AuthService } from '../../../auth/services/auth.service';
import { ReservationService, ReservationRequest } from '../../../admin/services/reservation.service';
import { RoomResponse, RoomSearchParams } from '../../services/room-search.service';
import { HotelService, HotelResponse } from '../../services/hotel.service';

export type SearchResultsData = {
  rooms: RoomResponse[];
  searchParams: RoomSearchParams;
  hotel?: HotelResponse;
};

@Component({
  selector: 'app-room-search-results',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './room-search-results.html',
  styleUrls: ['./room-search-results.css'],
})
export class RoomSearchResults implements OnChanges {
  private readonly auth = inject(AuthService);
  private readonly reservationService = inject(ReservationService);
  private readonly hotelService = inject(HotelService);
  private readonly cdr = inject(ChangeDetectorRef);

  @Input() resultsData?: SearchResultsData;
  @Input() open = false;

  @Output() closed = new EventEmitter<void>();
  @Output() bookingComplete = new EventEmitter<void>();
  @Output() modifySearch = new EventEmitter<void>();
  @Output() signInRequired = new EventEmitter<void>();

  booking = false;
  error = '';
  hotel?: HotelResponse;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['resultsData']?.currentValue && this.resultsData?.searchParams.hotelId) {
      this.loadHotel();
    }
  }

  get hasAvailableRooms(): boolean {
    return this.resultsData ? this.resultsData.rooms.length > 0 : false;
  }

  get selectedRoom(): RoomResponse | null {
    if (!this.hasAvailableRooms) return null;
    // Just pick the first available room - user doesn't choose specific room
    return this.resultsData!.rooms[0];
  }

  close(): void {
    this.closed.emit();
  }

  onModifySearch(): void {
    this.modifySearch.emit();
    this.close();
  }

  bookNow(): void {
    if (!this.selectedRoom || !this.resultsData) {
      this.error = 'Missing room or search data. Please try searching again.';
      return;
    }

    const me = this.auth.meSignal();
    if (!me || !me.localUserId) {
      // User not signed in - prompt to sign in
      this.signInRequired.emit();
      return;
    }

    // Validate all required fields
    if (!this.resultsData.searchParams.hotelId) {
      this.error = 'Hotel ID is missing. Please try searching again.';
      return;
    }

    if (!this.resultsData.searchParams.startDate || !this.resultsData.searchParams.endDate) {
      this.error = 'Check-in or check-out date is missing. Please try searching again.';
      return;
    }

    if (!this.selectedRoom.roomId || !this.selectedRoom.roomTypeId) {
      this.error = 'Room information is incomplete. Please try searching again.';
      return;
    }

    this.booking = true;
    this.error = '';

    // Calculate number of nights for pricing
    const startDate = new Date(this.resultsData.searchParams.startDate);
    const endDate = new Date(this.resultsData.searchParams.endDate);
    const nights = Math.ceil((endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24));

    // For now, we'll use a placeholder price. In a real app, you'd fetch room type details
    // to get the actual basePrice and calculate total
    const basePricePerNight = 100; // TODO: Fetch from room type
    const totalAmount = basePricePerNight * nights;

    const reservationRequest: ReservationRequest = {
      hotelId: this.resultsData.searchParams.hotelId,
      userId: me.localUserId,
      roomId: this.selectedRoom.roomId,
      roomTypeId: this.selectedRoom.roomTypeId,
      startDate: this.resultsData.searchParams.startDate,
      endDate: this.resultsData.searchParams.endDate,
      guestCount: this.resultsData.searchParams.guestCount || 1,
      totalAmount: totalAmount,
      currency: 'USD',
    };

    // Log the request for debugging (remove in production if needed)
    console.log('Creating reservation with data:', {
      hotelId: reservationRequest.hotelId,
      userId: reservationRequest.userId,
      roomId: reservationRequest.roomId,
      roomTypeId: reservationRequest.roomTypeId,
      startDate: reservationRequest.startDate,
      endDate: reservationRequest.endDate,
      guestCount: reservationRequest.guestCount,
      totalAmount: reservationRequest.totalAmount,
      currency: reservationRequest.currency,
    });

    this.reservationService
      .createReservation(reservationRequest)
      .pipe(
        finalize(() => {
          this.booking = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: () => {
          this.bookingComplete.emit();
          this.close();
          this.cdr.markForCheck();
        },
        error: (err) => {
          // Check for 401 Unauthorized
          if (err.status === 401) {
            this.signInRequired.emit();
            return;
          }

          if (err.error?.detail) {
            this.error = err.error.detail;
          } else if (err.error?.message) {
            this.error = err.error.message;
          } else {
            this.error = 'Could not complete booking. Please try again.';
          }
          this.cdr.markForCheck();
        },
      });
  }

  private loadHotel(): void {
    if (!this.resultsData?.searchParams.hotelId) return;

    this.hotelService.getHotelById(this.resultsData.searchParams.hotelId).subscribe({
      next: (hotel) => {
        this.hotel = hotel;
        if (this.resultsData) {
          this.resultsData.hotel = hotel;
        }
        this.cdr.markForCheck();
      },
      error: () => {
        // Hotel fetch failed, but we can still show results
        this.cdr.markForCheck();
      },
    });
  }
}
