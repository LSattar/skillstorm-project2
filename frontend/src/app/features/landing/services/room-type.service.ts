import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export type RoomTypeResponse = {
  roomTypeId: string;
  hotelId: string;
  name: string;
  description?: string;
  basePrice: number;
  maxGuests: number;
  bedCount: number;
  bedType: 'TWIN' | 'FULL' | 'QUEEN' | 'KING' | 'SOFA';
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
};

export type AmenityResponse = {
  amenityId: string;
  name: string;
  category: 'ROOM' | 'PROPERTY';
  isActive: boolean;
};

export type RoomTypeWithAmenities = RoomTypeResponse & {
  amenities?: AmenityResponse[];
};

@Injectable({ providedIn: 'root' })
export class RoomTypeService {
  private readonly api = environment.apiBaseUrl;

  constructor(private http: HttpClient) {}

  /**
   * Get all room types, optionally filtered by hotel
   */
  getAllRoomTypes(hotelId?: string): Observable<RoomTypeResponse[]> {
    const url = hotelId
      ? `${this.api}/room-types?hotelId=${hotelId}`
      : `${this.api}/room-types`;
    return this.http.get<RoomTypeResponse[]>(url, {
      withCredentials: true,
    });
  }

  /**
   * Get a single room type by ID
   */
  getRoomTypeById(roomTypeId: string): Observable<RoomTypeResponse> {
    return this.http.get<RoomTypeResponse>(`${this.api}/room-types/${roomTypeId}`, {
      withCredentials: true,
    });
  }

  /**
   * Get amenities for a room type
   * Note: This endpoint may need to be created in the backend
   */
  getRoomTypeAmenities(roomTypeId: string): Observable<AmenityResponse[]> {
    return this.http.get<AmenityResponse[]>(`${this.api}/room-types/${roomTypeId}/amenities`, {
      withCredentials: true,
    });
  }
}
