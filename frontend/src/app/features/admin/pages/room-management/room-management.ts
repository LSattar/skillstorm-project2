import { ChangeDetectorRef, Component, OnInit, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Header } from '../../../../shared/header/header';
import { Footer } from '../../../../shared/footer/footer';
import { AuthService } from '../../../auth/services/auth.service';
import {
  RoomManagementService,
  RoomResponse,
  RoomRequest,
  RoomTypeResponse,
  RoomTypeRequest,
  RoomStatus,
  BedType,
  AmenityResponse,
  RoomTypeAmenityResponse,
} from '../../services/room-management.service';

@Component({
  selector: 'app-room-management',
  standalone: true,
  imports: [CommonModule, FormsModule, Header, Footer],
  templateUrl: './room-management.html',
  styleUrl: './room-management.css',
})
export class RoomManagement implements OnInit {
  protected readonly auth = inject(AuthService);
  protected readonly router = inject(Router);
  protected readonly roomService = inject(RoomManagementService);
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

  // Data
  hotels: Array<{ hotelId: string; name: string }> = [];
  selectedHotelId = '';
  roomTypes: RoomTypeResponse[] = [];
  selectedRoomTypeId = '';
  rooms: RoomResponse[] = [];
  filteredRooms: RoomResponse[] = [];
  allAmenities: AmenityResponse[] = [];
  roomTypeAmenities: RoomTypeAmenityResponse[] = [];

  // Loading and error states
  loading = false;
  error: string | null = null;

  // Modals
  showRoomModal = false;
  showRoomTypeModal = false;
  showAmenityModal = false;

  // Forms
  roomForm: RoomRequest = {
    hotelId: '',
    roomTypeId: '',
    roomNumber: '',
    floor: '',
    status: 'AVAILABLE',
    notes: '',
  };
  roomTypeForm: RoomTypeRequest = {
    hotelId: '',
    name: '',
    description: '',
    basePrice: 0,
    maxGuests: 1,
    bedCount: 1,
    bedType: 'QUEEN',
    isActive: true,
  };

  editingRoom: RoomResponse | null = null;
  editingRoomType: RoomTypeResponse | null = null;

  statusOptions: RoomStatus[] = ['AVAILABLE', 'OCCUPIED', 'MAINTENANCE', 'OUT_OF_SERVICE'];
  bedTypeOptions: BedType[] = ['SINGLE', 'DOUBLE', 'QUEEN', 'KING', 'TWIN'];

  isNavOpen = false;
  today = new Date();

  ngOnInit(): void {
    this.loadHotels();
  }

  loadHotels(): void {
    this.loading = true;
    this.error = null;
    this.roomService.getAllHotels().subscribe({
      next: (hotels) => {
        this.hotels = hotels;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error loading hotels:', err);
        this.error = 'Failed to load hotels. Please try again.';
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  onHotelChange(): void {
    if (!this.selectedHotelId) {
      this.roomTypes = [];
      this.rooms = [];
      this.filteredRooms = [];
      this.selectedRoomTypeId = '';
      return;
    }

    this.loading = true;
    this.error = null;

    // Update forms with selected hotel
    this.roomForm.hotelId = this.selectedHotelId;
    this.roomTypeForm.hotelId = this.selectedHotelId;

    // Load room types for selected hotel
    this.roomService.getAllRoomTypes(this.selectedHotelId).subscribe({
      next: (roomTypes) => {
        this.roomTypes = roomTypes;
        // Also load all rooms and filter client-side since backend might not filter
        this.roomService.getAllRooms().subscribe({
          next: (allRooms) => {
            this.rooms = allRooms.filter((room) => room.hotelId === this.selectedHotelId);
            this.applyRoomTypeFilter();
            this.loading = false;
            this.cdr.detectChanges();
          },
          error: (err) => {
            console.error('Error loading rooms:', err);
            this.rooms = [];
            this.filteredRooms = [];
            this.loading = false;
            this.cdr.detectChanges();
          },
        });
      },
      error: (err) => {
        console.error('Error loading room types:', err);
        this.error = 'Failed to load room types. Please try again.';
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  onRoomTypeChange(): void {
    this.applyRoomTypeFilter();
    if (this.selectedRoomTypeId) {
      this.loadRoomTypeAmenities(this.selectedRoomTypeId);
    } else {
      this.roomTypeAmenities = [];
    }
  }

  applyRoomTypeFilter(): void {
    if (!this.selectedRoomTypeId) {
      this.filteredRooms = [...this.rooms];
      return;
    }
    this.filteredRooms = this.rooms.filter(
      (room) => room.roomTypeId === this.selectedRoomTypeId
    );
  }

  loadRoomTypeAmenities(roomTypeId: string): void {
    this.roomService.getRoomTypeAmenities(roomTypeId).subscribe({
      next: (amenities) => {
        this.roomTypeAmenities = amenities;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error loading room type amenities:', err);
        this.cdr.detectChanges();
      },
    });
  }

  // Room CRUD
  openAddRoomModal(): void {
    if (!this.selectedHotelId || !this.selectedRoomTypeId) {
      this.error = 'Please select a hotel and room type first.';
      return;
    }
    this.editingRoom = null;
    this.roomForm = {
      hotelId: this.selectedHotelId,
      roomTypeId: this.selectedRoomTypeId,
      roomNumber: '',
      floor: '',
      status: 'AVAILABLE',
      notes: '',
    };
    this.showRoomModal = true;
  }

  openEditRoomModal(room: RoomResponse): void {
    this.editingRoom = room;
    this.roomForm = {
      hotelId: room.hotelId,
      roomTypeId: room.roomTypeId,
      roomNumber: room.roomNumber,
      floor: room.floor || '',
      status: room.status,
      notes: room.notes || '',
    };
    this.showRoomModal = true;
  }

  closeRoomModal(): void {
    this.showRoomModal = false;
    this.editingRoom = null;
    this.error = null;
  }

  saveRoom(): void {
    if (!this.roomForm.roomNumber.trim()) {
      this.error = 'Room number is required.';
      return;
    }

    const action = this.editingRoom
      ? this.roomService.updateRoom(this.editingRoom.roomId, this.roomForm)
      : this.roomService.createRoom(this.roomForm);

    this.loading = true;
    this.error = null;

    action.subscribe({
      next: (savedRoom) => {
        if (this.editingRoom) {
          const index = this.rooms.findIndex((r) => r.roomId === savedRoom.roomId);
          if (index !== -1) {
            this.rooms[index] = savedRoom;
          }
        } else {
          this.rooms.push(savedRoom);
        }
        this.applyRoomTypeFilter();
        this.closeRoomModal();
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error saving room:', err);
        this.error = err.error?.message || 'Failed to save room. Please try again.';
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  deleteRoom(room: RoomResponse): void {
    if (!confirm(`Are you sure you want to delete room ${room.roomNumber}?`)) {
      return;
    }

    this.loading = true;
    this.error = null;

    this.roomService.deleteRoom(room.roomId).subscribe({
      next: () => {
        this.rooms = this.rooms.filter((r) => r.roomId !== room.roomId);
        this.applyRoomTypeFilter();
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error deleting room:', err);
        this.error = err.error?.message || 'Failed to delete room. Please try again.';
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  // Room Type CRUD
  openAddRoomTypeModal(): void {
    if (!this.selectedHotelId) {
      this.error = 'Please select a hotel first.';
      return;
    }
    this.editingRoomType = null;
    this.roomTypeForm = {
      hotelId: this.selectedHotelId,
      name: '',
      description: '',
      basePrice: 0,
      maxGuests: 1,
      bedCount: 1,
      bedType: 'QUEEN',
      isActive: true,
    };
    this.showRoomTypeModal = true;
  }

  openEditRoomTypeModal(roomType: RoomTypeResponse): void {
    this.editingRoomType = roomType;
    this.roomTypeForm = {
      hotelId: roomType.hotelId,
      name: roomType.name,
      description: roomType.description || '',
      basePrice: roomType.basePrice,
      maxGuests: roomType.maxGuests,
      bedCount: roomType.bedCount || 1,
      bedType: roomType.bedType || 'QUEEN',
      isActive: roomType.isActive ?? true,
    };
    this.showRoomTypeModal = true;
    this.loadRoomTypeAmenities(roomType.roomTypeId);
    this.loadAllAmenities();
  }

  closeRoomTypeModal(): void {
    this.showRoomTypeModal = false;
    this.editingRoomType = null;
    this.showAmenityModal = false;
    this.error = null;
  }

  saveRoomType(): void {
    if (!this.roomTypeForm.name.trim()) {
      this.error = 'Room type name is required.';
      return;
    }
    if (this.roomTypeForm.basePrice <= 0) {
      this.error = 'Base price must be greater than 0.';
      return;
    }
    if (this.roomTypeForm.maxGuests < 1) {
      this.error = 'Max guests must be at least 1.';
      return;
    }

    const action = this.editingRoomType
      ? this.roomService.updateRoomType(this.editingRoomType.roomTypeId, this.roomTypeForm)
      : this.roomService.createRoomType(this.roomTypeForm);

    this.loading = true;
    this.error = null;

    action.subscribe({
      next: (savedRoomType) => {
        if (this.editingRoomType) {
          const index = this.roomTypes.findIndex((rt) => rt.roomTypeId === savedRoomType.roomTypeId);
          if (index !== -1) {
            this.roomTypes[index] = savedRoomType;
          }
        } else {
          this.roomTypes.push(savedRoomType);
        }
        this.closeRoomTypeModal();
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error saving room type:', err);
        this.error = err.error?.message || 'Failed to save room type. Please try again.';
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  deleteRoomType(roomType: RoomTypeResponse): void {
    if (!confirm(`Are you sure you want to delete room type "${roomType.name}"? This will also delete all rooms of this type.`)) {
      return;
    }

    this.loading = true;
    this.error = null;

    this.roomService.deleteRoomType(roomType.roomTypeId).subscribe({
      next: () => {
        this.roomTypes = this.roomTypes.filter((rt) => rt.roomTypeId !== roomType.roomTypeId);
        this.rooms = this.rooms.filter((r) => r.roomTypeId !== roomType.roomTypeId);
        this.applyRoomTypeFilter();
        if (this.selectedRoomTypeId === roomType.roomTypeId) {
          this.selectedRoomTypeId = '';
        }
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error deleting room type:', err);
        this.error = err.error?.message || 'Failed to delete room type. Please try again.';
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  // Amenity management
  loadAllAmenities(): void {
    this.roomService.getAllAmenities().subscribe({
      next: (amenities) => {
        this.allAmenities = amenities;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error loading amenities:', err);
        this.cdr.detectChanges();
      },
    });
  }

  openAmenityModal(): void {
    if (!this.editingRoomType) {
      return;
    }
    this.loadAllAmenities();
    this.loadRoomTypeAmenities(this.editingRoomType.roomTypeId);
    this.showAmenityModal = true;
  }

  closeAmenityModal(): void {
    this.showAmenityModal = false;
  }

  addAmenityToRoomType(amenityId: string): void {
    if (!this.editingRoomType) return;

    const alreadyAdded = this.roomTypeAmenities.some((rta) => rta.amenityId === amenityId);
    if (alreadyAdded) {
      return;
    }

    this.roomService.addAmenityToRoomType(this.editingRoomType.roomTypeId, amenityId).subscribe({
      next: (added) => {
        this.roomTypeAmenities.push(added);
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error adding amenity:', err);
        this.error = err.error?.message || 'Failed to add amenity.';
        this.cdr.detectChanges();
      },
    });
  }

  removeAmenityFromRoomType(amenityId: string): void {
    if (!this.editingRoomType) return;

    this.roomService
      .removeAmenityFromRoomType(this.editingRoomType.roomTypeId, amenityId)
      .subscribe({
        next: () => {
          this.roomTypeAmenities = this.roomTypeAmenities.filter(
            (rta) => rta.amenityId !== amenityId
          );
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error removing amenity:', err);
          this.error = err.error?.message || 'Failed to remove amenity.';
          this.cdr.detectChanges();
        },
      });
  }

  getAvailableAmenities(): AmenityResponse[] {
    const addedIds = new Set(this.roomTypeAmenities.map((rta) => rta.amenityId));
    return this.allAmenities.filter((a) => !addedIds.has(a.amenityId));
  }

  getCurrentRoomTypeName(): string {
    if (!this.selectedRoomTypeId) return '—';
    const roomType = this.roomTypes.find((rt) => rt.roomTypeId === this.selectedRoomTypeId);
    return roomType?.name || '—';
  }

  getCurrentHotelName(): string {
    if (!this.selectedHotelId) return '—';
    const hotel = this.hotels.find((h) => h.hotelId === this.selectedHotelId);
    return hotel?.name || '—';
  }

  formatCurrency(amount: number): string {
    const safeAmount = Number.isFinite(amount) ? amount : 0;
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(safeAmount);
  }

  getStatusClass(status: string): string {
    const statusMap: Record<string, string> = {
      AVAILABLE: 'status-available',
      OCCUPIED: 'status-occupied',
      MAINTENANCE: 'status-maintenance',
      OUT_OF_SERVICE: 'status-out-of-service',
    };
    return statusMap[status] || 'status-default';
  }

  toggleNav() {
    this.isNavOpen = !this.isNavOpen;
  }

  closeNav() {
    this.isNavOpen = false;
  }

  openBooking() {}

  openSignIn() {}

  signOut() {
    this.auth.logout().subscribe({
      next: () => {},
      error: () => {},
    });
  }

  openProfile() {}
}
