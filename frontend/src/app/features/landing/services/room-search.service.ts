import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export type RoomResponse = {
  roomId: string;
  hotelId: string;
  roomTypeId: string;
  roomNumber: string;
  floor?: string;
  status: 'AVAILABLE' | 'OCCUPIED' | 'MAINTENANCE' | 'OUT_OF_SERVICE';
  notes?: string;
  createdAt: string;
  updatedAt: string;
};

export type RoomSearchParams = {
  hotelId?: string;
  startDate: string; // ISO date string (YYYY-MM-DD)
  endDate: string; // ISO date string (YYYY-MM-DD)
  guestCount?: number;
  roomTypeId?: string;
};

@Injectable({ providedIn: 'root' })
export class RoomSearchService {
  private readonly api = environment.apiBaseUrl;

  constructor(private http: HttpClient) {}

  /**
   * Search for available rooms
   */
  searchAvailableRooms(params: RoomSearchParams): Observable<RoomResponse[]> {
    const queryParams: Record<string, string> = {
      startDate: params.startDate,
      endDate: params.endDate,
    };

    if (params.hotelId) {
      queryParams['hotelId'] = params.hotelId;
    }
    if (params.guestCount) {
      queryParams['guestCount'] = params.guestCount.toString();
    }
    if (params.roomTypeId) {
      queryParams['roomTypeId'] = params.roomTypeId;
    }

    const queryString = new URLSearchParams(queryParams).toString();
    const url = `${this.api}/rooms/available?${queryString}`;

    return this.http.get<RoomResponse[]>(url, {
      withCredentials: true,
    });
  }
}
