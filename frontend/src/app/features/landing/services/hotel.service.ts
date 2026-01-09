import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export type HotelResponse = {
  hotelId: string;
  name: string;
  phone: string;
  address1: string;
  address2?: string;
  city: string;
  state: string;
  zip: string;
  timezone: string;
  createdAt: string;
  updatedAt: string;
};

@Injectable({ providedIn: 'root' })
export class HotelService {
  private readonly api = environment.apiBaseUrl;

  constructor(private http: HttpClient) {}

  /**
   * Get all hotels
   */
  getAllHotels(): Observable<HotelResponse[]> {
    return this.http.get<HotelResponse[]>(`${this.api}/hotels`, {
      withCredentials: true,
    });
  }

  /**
   * Get a single hotel by ID
   */
  getHotelById(id: string): Observable<HotelResponse> {
    return this.http.get<HotelResponse>(`${this.api}/hotels/${id}`, {
      withCredentials: true,
    });
  }
}
