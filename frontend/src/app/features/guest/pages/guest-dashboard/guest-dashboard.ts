import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, computed, effect, inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, finalize, forkJoin, of } from 'rxjs';
import { Header } from '../../../../shared/header/header';
import {
  ReservationResponse,
  ReservationService,
  ReservationStatus,
} from '../../../admin/services/reservation.service';
import { AuthService } from '../../../auth/services/auth.service';
import { HotelResponse, HotelService } from '../../../landing/services/hotel.service';
import { RoomManagementService } from '../../../admin/services/room-management.service';
import { RoomTypeService } from '../../../landing/services/room-type.service';
import { ReservationCancelModal } from '../../components/reservation-cancel-modal/reservation-cancel-modal';
import { ReservationModifyModal } from '../../components/reservation-modify-modal/reservation-modify-modal';

@Component({
  selector: 'app-guest-dashboard',
  standalone: true,
  imports: [CommonModule, Header, ReservationModifyModal, ReservationCancelModal],
  templateUrl: './guest-dashboard.html',
  styleUrl: './guest-dashboard.css',
})
export class GuestDashboard implements OnInit {
  protected readonly auth = inject(AuthService);
  protected readonly reservationService = inject(ReservationService);
  protected readonly hotelService = inject(HotelService);
  protected readonly roomService = inject(RoomManagementService);
  protected readonly roomTypeService = inject(RoomTypeService);
  protected readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);

  protected readonly isAuthenticated = this.auth.isAuthenticated;
  protected readonly roleLabel = this.auth.primaryRoleLabel;
  protected readonly userLabel = computed(() => {
    const me = this.auth.meSignal();
    if (!me) return '';
    const first = me.firstName?.trim();
    if (first) return first;
    return me.email || '';
  });
  protected readonly userEmail = computed(() => this.auth.meSignal()?.email ?? '');
  protected readonly firstName = computed(() => {
    const me = this.auth.meSignal();
    return me?.firstName?.trim() || '';
  });

  reservations: ReservationResponse[] = [];
  loading = false;
  error: string | null = null;
  checkingIn: Record<string, boolean> = {};

  hotels: HotelResponse[] = [];
  hotelMap: Record<string, string> = {};
  roomMap: Record<string, { roomNumber: string; floor?: string }> = {};
  roomTypeMap: Record<string, string> = {};

  isNavOpen = false;
  showProfileModal = false;
  selectedReservation: ReservationResponse | null = null;
  showModifyModal = false;
  showCancelModal = false;

  private reservationsLoaded = false;

  constructor() {
    // Watch for auth state to become available, then load reservations
    effect(() => {
      const me = this.auth.meSignal();
      if (me?.localUserId && !this.reservationsLoaded) {
        this.reservationsLoaded = true;
        // Use setTimeout to ensure change detection runs after effect
        setTimeout(() => {
          this.loadReservations();
        }, 0);
      }
    });
  }

  ngOnInit(): void {
    this.loadHotels();
    // Try to load reservations immediately if auth is already ready
    const me = this.auth.meSignal();
    if (me?.localUserId && !this.reservationsLoaded) {
      this.reservationsLoaded = true;
      this.loadReservations();
    }
  }

  private loadHotels(): void {
    this.hotelService.getAllHotels().subscribe({
      next: (hotels) => {
        this.hotels = hotels;
        // Create a map for quick lookup
        this.hotelMap = {};
        hotels.forEach((hotel) => {
          this.hotelMap[hotel.hotelId] = hotel.name;
        });
        this.cdr.detectChanges();
      },
      error: () => {
        // Silently fail - hotel names are not critical
        this.hotelMap = {};
        this.cdr.detectChanges();
      },
    });
  }

  loadReservations(): void {
    const me = this.auth.meSignal();
    if (!me?.localUserId) {
      this.error = 'You must be logged in to view your bookings.';
      this.loading = false;
      this.cdr.detectChanges();
      return;
    }

    this.loading = true;
    this.error = null;
    this.cdr.detectChanges();

    this.reservationService
      .getAllReservations({ userId: me.localUserId })
      .pipe(finalize(() => {
        this.loading = false;
        this.cdr.detectChanges();
      }))
      .subscribe({
        next: (reservations) => {
          // Sort reservations: upcoming > past > cancelled
          const today = new Date();
          today.setHours(0, 0, 0, 0);
          
          this.reservations = reservations.sort((a, b) => {
            const aEndDate = new Date(a.endDate);
            aEndDate.setHours(0, 0, 0, 0);
            const bEndDate = new Date(b.endDate);
            bEndDate.setHours(0, 0, 0, 0);
            
            const aIsCancelled = a.status === 'CANCELLED';
            const bIsCancelled = b.status === 'CANCELLED';
            const aIsUpcoming = !aIsCancelled && aEndDate >= today;
            const bIsUpcoming = !bIsCancelled && bEndDate >= today;
            const aIsPast = !aIsCancelled && aEndDate < today;
            const bIsPast = !bIsCancelled && bEndDate < today;
            
            // Priority: Upcoming > Past > Cancelled
            if (aIsUpcoming && !bIsUpcoming) return -1;
            if (!aIsUpcoming && bIsUpcoming) return 1;
            if (aIsPast && bIsCancelled) return -1;
            if (aIsCancelled && bIsPast) return 1;
            
            // Within same category, sort by date
            if (aIsUpcoming && bIsUpcoming) {
              // Upcoming: sort by startDate ascending (soonest first)
              const aStartDate = new Date(a.startDate).getTime();
              const bStartDate = new Date(b.startDate).getTime();
              return aStartDate - bStartDate;
            } else if (aIsPast && bIsPast) {
              // Past: sort by endDate descending (most recent first)
              return bEndDate.getTime() - aEndDate.getTime();
            } else if (aIsCancelled && bIsCancelled) {
              // Cancelled: sort by endDate descending (most recent first)
              return bEndDate.getTime() - aEndDate.getTime();
            }
            
            // Fallback: by creation date
            return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
          });
          
          // Load room and room type details for all reservations
          this.loadRoomAndRoomTypeDetails(reservations);
        },
        error: (err) => {
          if (err.error?.detail) {
            this.error = err.error.detail;
          } else if (err.error?.message) {
            this.error = err.error.message;
          } else {
            this.error = 'Failed to load your reservations. Please try again.';
          }
        },
      });
  }

  checkIn(reservation: ReservationResponse): void {
    // Temporarily disabled - backend integration disabled for preview
    // Just update the UI locally for demonstration
    const index = this.reservations.findIndex((r) => r.reservationId === reservation.reservationId);
    if (index !== -1 && this.reservations[index].status === 'CONFIRMED') {
      this.reservations[index] = {
        ...this.reservations[index],
        status: 'CHECKED_IN',
      };
      alert('Checked in successfully! (UI preview mode)');
    }

    // Original backend code (disabled for now):
    /*
    if (this.checkingIn[reservation.reservationId]) return;

    this.checkingIn[reservation.reservationId] = true;

    this.reservationService
      .checkIn(reservation.reservationId)
      .pipe(finalize(() => (this.checkingIn[reservation.reservationId] = false)))
      .subscribe({
        next: (updated) => {
          // Update the reservation in the list
          const index = this.reservations.findIndex(
            (r) => r.reservationId === reservation.reservationId
          );
          if (index !== -1) {
            this.reservations[index] = updated;
          }
        },
        error: (err) => {
          let message = 'Failed to check in. Please try again.';
          if (err.error?.detail) {
            message = err.error.detail;
          } else if (err.error?.message) {
            message = err.error.message;
          }
          alert(message);
        },
      });
    */
  }

  canCheckIn(reservation: ReservationResponse): boolean {
    return reservation.status === 'CONFIRMED';
  }

  getStatusClass(status: ReservationStatus): string {
    const statusMap: Record<ReservationStatus, string> = {
      PENDING: 'status-pending',
      CONFIRMED: 'status-confirmed',
      CHECKED_IN: 'status-checked-in',
      CHECKED_OUT: 'status-checked-out',
      CANCELLED: 'status-cancelled',
    };
    return statusMap[status] || '';
  }

  getStatusLabel(status: ReservationStatus): string {
    const labelMap: Record<ReservationStatus, string> = {
      PENDING: 'Pending',
      CONFIRMED: 'Confirmed',
      CHECKED_IN: 'Checked In',
      CHECKED_OUT: 'Checked Out',
      CANCELLED: 'Cancelled',
    };
    return labelMap[status] || status;
  }

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

  calculateTotalBalance(): number {
    return this.reservations
      .filter((r) => r.status !== 'CANCELLED' && r.status !== 'CHECKED_OUT')
      .reduce((sum, r) => sum + r.totalAmount, 0);
  }

  getTotalStays(): number {
    // Count all non-cancelled reservations
    return this.reservations.filter((r) => r.status !== 'CANCELLED').length;
  }

  getTotalRewardsPoints(): number {
    // Placeholder - rewards system not implemented yet
    // Returns a static value for display purposes
    return 0;
  }

  getHotelLocation(hotelId: string): string {
    return this.hotelMap[hotelId] || `Hotel ${hotelId.substring(0, 8)}`;
  }

  getRoomNumber(roomId: string): string {
    return this.roomMap[roomId]?.roomNumber || '—';
  }

  getRoomFloor(roomId: string): string {
    const floor = this.roomMap[roomId]?.floor;
    return floor ? `Floor ${floor}` : '—';
  }

  getRoomTypeName(roomTypeId: string): string {
    return this.roomTypeMap[roomTypeId] || '—';
  }

  private loadRoomAndRoomTypeDetails(reservations: ReservationResponse[]): void {
    // Get unique room IDs and room type IDs
    const uniqueRoomIds = [...new Set(reservations.map(r => r.roomId).filter(Boolean))];
    const uniqueRoomTypeIds = [...new Set(reservations.map(r => r.roomTypeId).filter(Boolean))];

    if (uniqueRoomIds.length === 0 && uniqueRoomTypeIds.length === 0) {
      return;
    }

    // Create requests for rooms with error handling (return null on error)
    const roomRequests = uniqueRoomIds.map(roomId =>
      this.roomService.getRoomById(roomId).pipe(
        catchError(() => of(null))
      )
    );

    // Create requests for room types with error handling (return null on error)
    const roomTypeRequests = uniqueRoomTypeIds.map(roomTypeId =>
      this.roomTypeService.getRoomTypeById(roomTypeId).pipe(
        catchError(() => of(null))
      )
    );

    // Fetch all room and room type details in parallel
    forkJoin({
      rooms: roomRequests.length > 0 ? forkJoin(roomRequests) : of([]),
      roomTypes: roomTypeRequests.length > 0 ? forkJoin(roomTypeRequests) : of([]),
    }).subscribe({
      next: ({ rooms, roomTypes }) => {
        // Build room map
        if (Array.isArray(rooms)) {
          rooms.forEach((room: any) => {
            if (room && room.roomId) {
              this.roomMap[room.roomId] = {
                roomNumber: room.roomNumber || '—',
                floor: room.floor,
              };
            }
          });
        }

        // Build room type map
        if (Array.isArray(roomTypes)) {
          roomTypes.forEach((roomType: any) => {
            if (roomType && roomType.roomTypeId) {
              this.roomTypeMap[roomType.roomTypeId] = roomType.name || '—';
            }
          });
        }

        this.cdr.detectChanges();
      },
      error: (err) => {
        // Silently fail - room details are not critical
        console.error('Error loading room/room type details:', err);
        this.cdr.detectChanges();
      },
    });
  }

  hasBalance(reservation: ReservationResponse): boolean {
    // Show pay button for reservations that need payment (PENDING or CONFIRMED status)
    return (
      (reservation.status === 'PENDING' || reservation.status === 'CONFIRMED') &&
      reservation.totalAmount > 0
    );
  }

  payReservation(reservation: ReservationResponse): void {
    // Navigate to payment page
    this.router.navigate(['/payment', reservation.reservationId]);
  }

  openModifyModal(reservation: ReservationResponse): void {
    this.selectedReservation = reservation;
    this.showModifyModal = true;
  }

  closeModifyModal(): void {
    this.showModifyModal = false;
    this.selectedReservation = null;
  }

  onReservationUpdated(updated: ReservationResponse): void {
    const index = this.reservations.findIndex((r) => r.reservationId === updated.reservationId);
    if (index !== -1) {
      this.reservations[index] = updated;
    }
    this.closeModifyModal();
    // Reload reservations to ensure data is fresh
    this.loadReservations();
    this.cdr.detectChanges();
  }

  openCancelModal(reservation: ReservationResponse): void {
    this.selectedReservation = reservation;
    this.showCancelModal = true;
  }

  closeCancelModal(): void {
    this.showCancelModal = false;
    this.selectedReservation = null;
  }

  onReservationCancelled(cancelled: ReservationResponse): void {
    const index = this.reservations.findIndex((r) => r.reservationId === cancelled.reservationId);
    if (index !== -1) {
      this.reservations[index] = cancelled;
    }
    this.closeCancelModal();
    // Reload reservations to ensure data is fresh
    this.loadReservations();
    this.cdr.detectChanges();
  }

  canModify(reservation: ReservationResponse): boolean {
    // Allow modifications for PENDING and CONFIRMED reservations
    return reservation.status === 'PENDING' || reservation.status === 'CONFIRMED';
  }

  canCancel(reservation: ReservationResponse): boolean {
    // Allow cancellation for PENDING, CONFIRMED, and CHECKED_IN reservations
    return (
      reservation.status === 'PENDING' ||
      reservation.status === 'CONFIRMED' ||
      reservation.status === 'CHECKED_IN'
    );
  }

  toggleNav(): void {
    this.isNavOpen = !this.isNavOpen;
  }

  closeNav(): void {
    this.isNavOpen = false;
  }

  openBooking(): void {
    this.router.navigate(['/']);
  }

  openSignIn(): void {
    this.auth.startGoogleLogin();
  }

  signOut(): void {
    this.auth.logout().subscribe(() => {
      this.router.navigate(['/']);
    });
  }

  openProfile(): void {
    this.router.navigate(['/profile-settings']);
  }

  closeProfile(): void {
    this.showProfileModal = false;
  }

  openSystemSettings(): void {
    // Not applicable for guest dashboard
  }
}
