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
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import { HotelService, HotelResponse } from '../../services/hotel.service';
import { RoomSearchService, RoomResponse, RoomSearchParams } from '../../services/room-search.service';

export type RoomTypeOption = {
  roomTypeId: string;
  name: string;
};

@Component({
  selector: 'app-room-search-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './room-search-modal.html',
  styleUrls: ['./room-search-modal.css'],
})
export class RoomSearchModal implements OnChanges {
  private readonly hotelService = inject(HotelService);
  private readonly roomSearchService = inject(RoomSearchService);
  private readonly cdr = inject(ChangeDetectorRef);

  @Input() open = false;

  @Output() closed = new EventEmitter<void>();
  @Output() searchResults = new EventEmitter<{ rooms: RoomResponse[]; searchParams: RoomSearchParams }>();
  @Output() signInRequired = new EventEmitter<void>();

  hotels: HotelResponse[] = [];
  roomTypes: RoomTypeOption[] = [];
  
  searchForm = {
    hotelId: '',
    checkin: '',
    checkout: '',
    guests: 1,
    roomTypeId: '',
  };

  loading = false;
  searching = false;
  error = '';

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['open']?.currentValue === true) {
      this.loadHotels();
      this.resetForm();
    }
  }

  close(): void {
    this.closed.emit();
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      event.preventDefault();
      this.close();
    }
  }

  onHotelChange(): void {
    // When hotel changes, load room types for that hotel
    if (this.searchForm.hotelId) {
      this.loadRoomTypes(this.searchForm.hotelId);
    } else {
      this.roomTypes = [];
      this.searchForm.roomTypeId = '';
    }
  }

  search(): void {
    if (!this.searchForm.checkin || !this.searchForm.checkout) {
      this.error = 'Please select check-in and check-out dates';
      return;
    }

    if (!this.searchForm.hotelId) {
      this.error = 'Please select a hotel';
      return;
    }

    // Validate dates - compare only date parts (year, month, day) to avoid timezone issues
    const checkin = new Date(this.searchForm.checkin + 'T00:00:00');
    const checkout = new Date(this.searchForm.checkout + 'T00:00:00');
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    // Compare date components directly (year, month, day) to avoid timezone issues
    const checkinDate = new Date(checkin.getFullYear(), checkin.getMonth(), checkin.getDate());
    const todayDate = new Date(today.getFullYear(), today.getMonth(), today.getDate());

    if (checkinDate < todayDate) {
      this.error = 'Check-in date cannot be in the past';
      return;
    }

    if (checkout <= checkin) {
      this.error = 'Check-out date must be after check-in date';
      return;
    }

    this.searching = true;
    this.error = '';

    const params: RoomSearchParams = {
      hotelId: this.searchForm.hotelId,
      startDate: this.searchForm.checkin,
      endDate: this.searchForm.checkout,
      guestCount: this.searchForm.guests,
    };

    if (this.searchForm.roomTypeId) {
      params.roomTypeId = this.searchForm.roomTypeId;
    }

    this.roomSearchService
      .searchAvailableRooms(params)
      .pipe(
        finalize(() => {
          this.searching = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (rooms) => {
          this.searchResults.emit({ rooms, searchParams: params });
          this.close();
          this.cdr.markForCheck();
        },
        error: (err) => {
          // Check for 401 Unauthorized
          if (err.status === 401) {
            this.signInRequired.emit();
            this.close();
            return;
          }

          if (err.error?.detail) {
            this.error = err.error.detail;
          } else if (err.error?.message) {
            this.error = err.error.message;
          } else {
            this.error = 'Could not search for rooms. Please try again.';
          }
          this.cdr.markForCheck();
        },
      });
  }

  private loadHotels(): void {
    this.loading = true;
    this.error = '';

    this.hotelService
      .getAllHotels()
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (hotels) => {
          this.hotels = hotels;
          this.cdr.markForCheck();
        },
        error: () => {
          this.error = 'Could not load hotels. Please try again.';
          this.cdr.markForCheck();
        },
      });
  }

  private loadRoomTypes(hotelId: string): void {
    // For now, we'll fetch room types from the backend
    // Since the backend endpoint accepts hotelId, we can use it
    // But we need to create a room type service first, or we can
    // just let the user select "any" and not filter by room type
    // For now, let's keep it simple and allow "any" selection
    this.roomTypes = [];
    this.searchForm.roomTypeId = '';
    
    // TODO: Add room type service to fetch room types by hotel
    // For now, room type selection will be optional
  }

  private resetForm(): void {
    const today = new Date();
    const tomorrow = new Date(today);
    tomorrow.setDate(tomorrow.getDate() + 1);

    this.searchForm = {
      hotelId: '',
      checkin: this.formatDate(today),
      checkout: this.formatDate(tomorrow),
      guests: 1,
      roomTypeId: '',
    };
    this.error = '';
  }

  private formatDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
