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
import { Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { finalize, map, catchError } from 'rxjs/operators';
import { AuthService } from '../../../auth/services/auth.service';
import { ReservationService, ReservationRequest, ReservationResponse } from '../../../admin/services/reservation.service';
import { RoomResponse, RoomSearchParams } from '../../services/room-search.service';
import { HotelService, HotelResponse } from '../../services/hotel.service';
import { RoomTypeService, RoomTypeResponse } from '../../services/room-type.service';
import { RoomManagementService, AmenityResponse } from '../../../admin/services/room-management.service';

export type SearchResultsData = {
  rooms: RoomResponse[];
  searchParams: RoomSearchParams;
  hotel?: HotelResponse;
};

export type RoomTypeOption = {
  roomType: RoomTypeResponse;
  rooms: RoomResponse[];
  selectedRoom: RoomResponse | null;
  amenities: AmenityResponse[];
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
  private readonly roomTypeService = inject(RoomTypeService);
  private readonly roomManagementService = inject(RoomManagementService);
  private readonly router = inject(Router);
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
  roomTypeOptions: RoomTypeOption[] = [];
  loadingRoomTypes = false;
  selectedRoomTypeOption: RoomTypeOption | null = null;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['resultsData']?.currentValue && this.resultsData?.searchParams.hotelId) {
      this.loadHotel();
      this.loadRoomTypes();
    }
  }

  get hasAvailableRooms(): boolean {
    return this.resultsData ? this.resultsData.rooms.length > 0 : false;
  }

  get selectedRoom(): RoomResponse | null {
    if (!this.selectedRoomTypeOption) return null;
    return this.selectedRoomTypeOption.selectedRoom || this.selectedRoomTypeOption.rooms[0] || null;
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
    const nights = this.getNights();

    // Get price from selected room type
    let totalAmount = 100; // Fallback price
    if (this.selectedRoomTypeOption) {
      totalAmount = this.calculateTotalPrice(this.selectedRoomTypeOption.roomType, nights);
    }

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

    this.reservationService
      .createReservation(reservationRequest)
      .pipe(
        finalize(() => {
          this.booking = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (reservation: ReservationResponse) => {
          // Navigate to payment page with the reservation ID
          this.bookingComplete.emit();
          this.close();
          this.router.navigate(['/payment', reservation.reservationId]);
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

  private loadRoomTypes(): void {
    if (!this.resultsData || !this.resultsData.rooms.length) {
      this.roomTypeOptions = [];
      return;
    }

    this.loadingRoomTypes = true;

    // Group rooms by room type ID
    const roomsByTypeId = new Map<string, RoomResponse[]>();
    const uniqueRoomTypeIds = new Set<string>();

    this.resultsData.rooms.forEach((room) => {
      if (room.roomTypeId) {
        uniqueRoomTypeIds.add(room.roomTypeId);
        if (!roomsByTypeId.has(room.roomTypeId)) {
          roomsByTypeId.set(room.roomTypeId, []);
        }
        roomsByTypeId.get(room.roomTypeId)!.push(room);
      }
    });

    // Fetch room type details for each unique room type
    const roomTypeRequests = Array.from(uniqueRoomTypeIds).map((roomTypeId) =>
      this.roomTypeService.getRoomTypeById(roomTypeId).pipe(
        map((roomType) => ({
          roomType,
          roomTypeId,
        }))
      )
    );

    if (roomTypeRequests.length === 0) {
      this.loadingRoomTypes = false;
      this.roomTypeOptions = [];
      this.cdr.markForCheck();
      return;
    }

    forkJoin(roomTypeRequests).subscribe({
      next: (results) => {
        // Create initial room type options
        this.roomTypeOptions = results.map(({ roomType, roomTypeId }) => ({
          roomType,
          rooms: roomsByTypeId.get(roomTypeId) || [],
          selectedRoom: null,
          amenities: [],
        }));

        // Sort by base price (lowest first)
        this.roomTypeOptions.sort((a, b) => a.roomType.basePrice - b.roomType.basePrice);

        // Fetch amenities for each room type
        const amenityRequests = this.roomTypeOptions.map((option) =>
          this.roomManagementService.getRoomTypeAmenities(option.roomType.roomTypeId).pipe(
            map((roomTypeAmenities) => ({
              roomTypeId: option.roomType.roomTypeId,
              amenities: roomTypeAmenities
                .map((rta) => rta.amenity)
                .filter((amenity): amenity is AmenityResponse => amenity !== undefined && amenity !== null),
            })),
            catchError(() => of({ roomTypeId: option.roomType.roomTypeId, amenities: [] }))
          )
        );

        if (amenityRequests.length > 0) {
          forkJoin(amenityRequests).subscribe({
            next: (amenityResults) => {
              // Map amenities to room type options
              const amenityMap = new Map<string, AmenityResponse[]>();
              amenityResults.forEach(({ roomTypeId, amenities }) => {
                amenityMap.set(roomTypeId, amenities);
              });

              this.roomTypeOptions = this.roomTypeOptions.map((option) => ({
                ...option,
                amenities: amenityMap.get(option.roomType.roomTypeId) || [],
              }));

              // Auto-select first option if none selected
              if (!this.selectedRoomTypeOption && this.roomTypeOptions.length > 0) {
                this.selectRoomType(this.roomTypeOptions[0]);
              }

              this.loadingRoomTypes = false;
              this.cdr.markForCheck();
            },
            error: () => {
              // If amenities fail to load, continue without them
              if (!this.selectedRoomTypeOption && this.roomTypeOptions.length > 0) {
                this.selectRoomType(this.roomTypeOptions[0]);
              }
              this.loadingRoomTypes = false;
              this.cdr.markForCheck();
            },
          });
        } else {
          // Auto-select first option if none selected
          if (!this.selectedRoomTypeOption && this.roomTypeOptions.length > 0) {
            this.selectRoomType(this.roomTypeOptions[0]);
          }
          this.loadingRoomTypes = false;
          this.cdr.markForCheck();
        }
      },
      error: () => {
        this.loadingRoomTypes = false;
        // If room types fail to load, still allow booking with first room
        this.roomTypeOptions = [];
        this.cdr.markForCheck();
      },
    });
  }

  selectRoomType(option: RoomTypeOption): void {
    this.selectedRoomTypeOption = option;
    // Auto-select first room in this type
    if (option.rooms.length > 0 && !option.selectedRoom) {
      option.selectedRoom = option.rooms[0];
    }
    this.cdr.markForCheck();
  }

  formatBedType(bedType: string): string {
    const bedTypeMap: Record<string, string> = {
      TWIN: 'Twin',
      FULL: 'Full',
      QUEEN: 'Queen',
      KING: 'King',
      SOFA: 'Sofa Bed',
    };
    return bedTypeMap[bedType] || bedType;
  }

  calculateTotalPrice(roomType: RoomTypeResponse, nights: number): number {
    return Math.round((roomType.basePrice * nights) * 100) / 100;
  }

  formatPrice(price: number): string {
    return price.toFixed(2);
  }

  getNights(): number {
    if (!this.resultsData?.searchParams.startDate || !this.resultsData?.searchParams.endDate) {
      return 0;
    }
    const startDate = new Date(this.resultsData.searchParams.startDate);
    const endDate = new Date(this.resultsData.searchParams.endDate);
    return Math.ceil((endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24));
  }
}
