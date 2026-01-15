import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, switchMap } from 'rxjs';
import { environment } from '../../../../environments/environment';

export type RoomStatus = 'AVAILABLE' | 'OCCUPIED' | 'MAINTENANCE' | 'OUT_OF_SERVICE';

export type BedType = 'SINGLE' | 'DOUBLE' | 'QUEEN' | 'KING' | 'TWIN';

export type RoomResponse = {
  roomId: string;
  hotelId: string;
  roomTypeId: string;
  roomNumber: string;
  floor?: string;
  status: RoomStatus;
  notes?: string;
  createdAt: string;
  updatedAt: string;
};

export type RoomRequest = {
  hotelId: string;
  roomTypeId: string;
  roomNumber: string;
  floor?: string;
  status?: RoomStatus;
  notes?: string;
};

export type RoomTypeResponse = {
  roomTypeId: string;
  hotelId: string;
  name: string;
  description?: string;
  basePrice: number;
  maxGuests: number;
  bedCount?: number;
  bedType?: BedType;
  isActive?: boolean;
  createdAt: string;
  updatedAt: string;
};

export type RoomTypeRequest = {
  hotelId: string;
  name: string;
  description?: string;
  basePrice: number;
  maxGuests: number;
  bedCount?: number;
  bedType?: BedType;
  isActive?: boolean;
};

export type AmenityResponse = {
  amenityId: string;
  name: string;
  category?: string;
  description?: string;
};

export type RoomTypeAmenityResponse = {
  roomTypeId: string;
  amenityId: string;
  amenity?: AmenityResponse;
};

@Injectable({ providedIn: 'root' })
export class RoomManagementService {
  private readonly api = environment.apiBaseUrl;

  constructor(private http: HttpClient) {}

  private ensureCsrfToken() {
    return this.http.get<{ headerName: string; token: string }>(`${this.api}/csrf`, {
      withCredentials: true,
    });
  }

  // Hotels
  getAllHotels(): Observable<Array<{ hotelId: string; name: string }>> {
    return this.http.get<Array<{ hotelId: string; name: string }>>(`${this.api}/hotels`, {
      withCredentials: true,
    });
  }

  // Rooms
  getAllRooms(hotelId?: string): Observable<RoomResponse[]> {
    const url = hotelId 
      ? `${this.api}/rooms?hotelId=${encodeURIComponent(hotelId)}`
      : `${this.api}/rooms`;
    return this.http.get<RoomResponse[]>(url, {
      withCredentials: true,
    });
  }

  getRoomById(id: string): Observable<RoomResponse> {
    return this.http.get<RoomResponse>(`${this.api}/rooms/${id}`, {
      withCredentials: true,
    });
  }

  createRoom(payload: RoomRequest): Observable<RoomResponse> {
    return this.ensureCsrfToken().pipe(
      switchMap((csrf) =>
        this.http.post<RoomResponse>(`${this.api}/rooms`, payload, {
          withCredentials: true,
          headers: csrf?.token ? { [csrf.headerName || 'X-XSRF-TOKEN']: csrf.token } : undefined,
        })
      )
    );
  }

  updateRoom(id: string, payload: RoomRequest): Observable<RoomResponse> {
    return this.ensureCsrfToken().pipe(
      switchMap((csrf) =>
        this.http.put<RoomResponse>(`${this.api}/rooms/${id}`, payload, {
          withCredentials: true,
          headers: csrf?.token ? { [csrf.headerName || 'X-XSRF-TOKEN']: csrf.token } : undefined,
        })
      )
    );
  }

  deleteRoom(id: string): Observable<void> {
    return this.ensureCsrfToken().pipe(
      switchMap((csrf) =>
        this.http.delete<void>(`${this.api}/rooms/${id}`, {
          withCredentials: true,
          headers: csrf?.token ? { [csrf.headerName || 'X-XSRF-TOKEN']: csrf.token } : undefined,
        })
      )
    );
  }

  // Room Types
  getAllRoomTypes(hotelId?: string): Observable<RoomTypeResponse[]> {
    const url = hotelId 
      ? `${this.api}/room-types?hotelId=${encodeURIComponent(hotelId)}`
      : `${this.api}/room-types`;
    return this.http.get<RoomTypeResponse[]>(url, {
      withCredentials: true,
    });
  }

  getRoomTypeById(id: string): Observable<RoomTypeResponse> {
    return this.http.get<RoomTypeResponse>(`${this.api}/room-types/${id}`, {
      withCredentials: true,
    });
  }

  createRoomType(payload: RoomTypeRequest): Observable<RoomTypeResponse> {
    return this.ensureCsrfToken().pipe(
      switchMap((csrf) =>
        this.http.post<RoomTypeResponse>(`${this.api}/room-types`, payload, {
          withCredentials: true,
          headers: csrf?.token ? { [csrf.headerName || 'X-XSRF-TOKEN']: csrf.token } : undefined,
        })
      )
    );
  }

  updateRoomType(id: string, payload: RoomTypeRequest): Observable<RoomTypeResponse> {
    return this.ensureCsrfToken().pipe(
      switchMap((csrf) =>
        this.http.put<RoomTypeResponse>(`${this.api}/room-types/${id}`, payload, {
          withCredentials: true,
          headers: csrf?.token ? { [csrf.headerName || 'X-XSRF-TOKEN']: csrf.token } : undefined,
        })
      )
    );
  }

  deleteRoomType(id: string): Observable<void> {
    return this.ensureCsrfToken().pipe(
      switchMap((csrf) =>
        this.http.delete<void>(`${this.api}/room-types/${id}`, {
          withCredentials: true,
          headers: csrf?.token ? { [csrf.headerName || 'X-XSRF-TOKEN']: csrf.token } : undefined,
        })
      )
    );
  }

  // Amenities
  getAllAmenities(): Observable<AmenityResponse[]> {
    return this.http.get<AmenityResponse[]>(`${this.api}/amenities`, {
      withCredentials: true,
    });
  }

  // Room Type Amenities
  getRoomTypeAmenities(roomTypeId: string): Observable<RoomTypeAmenityResponse[]> {
    return this.http.get<RoomTypeAmenityResponse[]>(
      `${this.api}/room-type-amenities?roomTypeId=${encodeURIComponent(roomTypeId)}`,
      {
        withCredentials: true,
      }
    );
  }

  addAmenityToRoomType(roomTypeId: string, amenityId: string): Observable<RoomTypeAmenityResponse> {
    return this.ensureCsrfToken().pipe(
      switchMap((csrf) =>
        this.http.post<RoomTypeAmenityResponse>(
          `${this.api}/room-type-amenities`,
          { roomTypeId, amenityId },
          {
            withCredentials: true,
            headers: csrf?.token ? { [csrf.headerName || 'X-XSRF-TOKEN']: csrf.token } : undefined,
          }
        )
      )
    );
  }

  removeAmenityFromRoomType(roomTypeId: string, amenityId: string): Observable<void> {
    return this.ensureCsrfToken().pipe(
      switchMap((csrf) =>
        this.http.delete<void>(
          `${this.api}/room-type-amenities/${roomTypeId}/${amenityId}`,
          {
            withCredentials: true,
            headers: csrf?.token ? { [csrf.headerName || 'X-XSRF-TOKEN']: csrf.token } : undefined,
          }
        )
      )
    );
  }
}
