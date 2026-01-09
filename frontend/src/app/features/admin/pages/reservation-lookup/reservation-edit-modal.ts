import { Component, EventEmitter, Input, Output, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  ReservationService,
  ReservationResponse,
  ReservationRequest,
  ReservationStatus,
} from '../../services/reservation.service';

@Component({
  selector: 'app-reservation-edit-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reservation-edit-modal.html',
  styleUrl: './reservation-edit-modal.css',
})
export class ReservationEditModal implements OnInit {
  @Input() reservation!: ReservationResponse & { hotelName?: string; guestName?: string };
  @Output() close = new EventEmitter<void>();
  @Output() updated = new EventEmitter<ReservationResponse>();

  private readonly reservationService = inject(ReservationService);

  editForm: ReservationRequest = {
    hotelId: '',
    userId: '',
    roomId: '',
    roomTypeId: '',
    startDate: '',
    endDate: '',
    guestCount: 1,
    status: 'PENDING',
    totalAmount: 0,
    currency: 'USD',
    specialRequests: '',
  };

  // Editable form fields (display-friendly)
  selectedHotelId = '';
  selectedUserId = '';
  roomNumberInput = '';
  selectedRoomTypeId = '';

  loading = false;
  error: string | null = null;
  showCancelConfirm = false;
  cancelReason = '';

  statusOptions: ReservationStatus[] = ['PENDING', 'CONFIRMED', 'CANCELLED', 'CHECKED_IN', 'CHECKED_OUT'];

  // Helper maps for hotel and guest names
  hotelNames: Record<string, string> = {
    'hotel-1': 'ReserveOne Downtown',
    'hotel-2': 'ReserveOne Airport',
    'hotel-3': 'ReserveOne Beach Resort',
  };

  guestNames: Record<string, string> = {
    'user-101': 'Sarah Johnson',
    'user-102': 'Michael Chen',
    'user-103': 'Emily Rodriguez',
    'user-104': 'David Kim',
    'user-105': 'Jessica Taylor',
    'user-106': 'Robert Williams',
    'user-107': 'Amanda Martinez',
    'user-108': 'Christopher Lee',
    'user-109': 'Jennifer Brown',
    'user-110': 'Daniel Garcia',
    'user-111': 'Lisa Anderson',
  };

  roomTypes: Record<string, string> = {
    'standard-double': 'Standard Double',
    'standard-king': 'Standard King',
    'deluxe-double': 'Deluxe Double',
    'deluxe-king': 'Deluxe King',
    'suite': 'Suite',
  };

  // Get arrays for dropdowns
  get hotelOptions(): { id: string; name: string }[] {
    return Object.entries(this.hotelNames).map(([id, name]) => ({ id, name }));
  }

  get guestOptions(): { id: string; name: string }[] {
    return Object.entries(this.guestNames).map(([id, name]) => ({ id, name }));
  }

  get roomTypeOptions(): { id: string; name: string }[] {
    return Object.entries(this.roomTypes).map(([id, name]) => ({ id, name }));
  }

  getCurrentHotelName(): string {
    return this.hotelNames[this.selectedHotelId] || this.selectedHotelId;
  }

  getCurrentGuestName(): string {
    return this.guestNames[this.selectedUserId] || this.selectedUserId;
  }

  getCurrentRoomTypeName(): string {
    return this.roomTypes[this.selectedRoomTypeId] || this.selectedRoomTypeId;
  }

  onHotelChange(): void {
    this.editForm.hotelId = this.selectedHotelId;
  }

  onGuestChange(): void {
    this.editForm.userId = this.selectedUserId;
  }

  onRoomNumberChange(): void {
    // Update roomId from room number
    if (this.roomNumberInput) {
      const roomNum = this.roomNumberInput.replace(/\D/g, ''); // Extract only digits
      if (roomNum) {
        this.editForm.roomId = `room-${roomNum}`;
      }
    }
  }

  onRoomTypeChange(): void {
    this.editForm.roomTypeId = this.selectedRoomTypeId;
  }

  ngOnInit(): void {
    if (this.reservation) {
      // Extract room number from roomId
      const roomId = this.reservation.roomId || '';
      const roomMatch = roomId.match(/room-?(\d+)/i) || roomId.match(/(\d+)/);
      this.roomNumberInput = roomMatch ? roomMatch[1] : '';

      // Set selected values for dropdowns
      this.selectedHotelId = this.reservation.hotelId;
      this.selectedUserId = this.reservation.userId;
      this.selectedRoomTypeId = this.reservation.roomTypeId;

      this.editForm = {
        hotelId: this.reservation.hotelId,
        userId: this.reservation.userId,
        roomId: this.reservation.roomId,
        roomTypeId: this.reservation.roomTypeId,
        startDate: this.reservation.startDate,
        endDate: this.reservation.endDate,
        guestCount: this.reservation.guestCount,
        status: this.reservation.status,
        totalAmount: this.reservation.totalAmount,
        currency: this.reservation.currency || 'USD',
        specialRequests: this.reservation.specialRequests || '',
      };
    }
  }

  onClose(): void {
    this.close.emit();
  }

  onSave(): void {
    if (!this.reservation) return;

    // Ensure all form values are synced from the editable fields
    this.editForm.hotelId = this.selectedHotelId;
    this.editForm.userId = this.selectedUserId;
    this.editForm.roomTypeId = this.selectedRoomTypeId;
    
    // Update roomId from room number
    if (this.roomNumberInput) {
      const roomNum = this.roomNumberInput.replace(/\D/g, ''); // Extract only digits
      if (roomNum) {
        this.editForm.roomId = `room-${roomNum}`;
      }
    }

    this.loading = true;
    this.error = null;

    // Simulate API call with mock data
    setTimeout(() => {
      const updatedReservation: ReservationResponse = {
        ...this.reservation,
        hotelId: this.editForm.hotelId,
        userId: this.editForm.userId,
        roomId: this.editForm.roomId,
        roomTypeId: this.editForm.roomTypeId,
        startDate: this.editForm.startDate,
        endDate: this.editForm.endDate,
        guestCount: this.editForm.guestCount,
        status: this.editForm.status || this.reservation.status,
        totalAmount: this.editForm.totalAmount,
        currency: this.editForm.currency,
        specialRequests: this.editForm.specialRequests,
        updatedAt: new Date().toISOString(),
      };

      this.loading = false;
      this.updated.emit(updatedReservation);
    }, 500);
  }

  onCancelReservation(): void {
    if (!this.reservation) return;

    this.loading = true;
    this.error = null;

    // Simulate API call with mock data
    setTimeout(() => {
      const cancelledReservation: ReservationResponse = {
        ...this.reservation,
        status: 'CANCELLED',
        cancellationReason: this.cancelReason || undefined,
        cancelledAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };

      this.loading = false;
      this.updated.emit(cancelledReservation);
    }, 500);
  }

  formatCurrency(amount: number): string {
    const safeAmount = Number.isFinite(amount) ? amount : 0;
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(safeAmount);
  }

  formatDate(dateString: string): string {
    if (!dateString) return '—';
    const d = new Date(dateString);
    if (!Number.isFinite(d.getTime())) return '—';
    return d.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  }
}
