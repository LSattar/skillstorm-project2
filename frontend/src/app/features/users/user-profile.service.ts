import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { switchMap } from 'rxjs/operators';

export type UserProfile = {
  userId?: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  phone?: string;
  address1?: string;
  address2?: string;
  city?: string;
  state?: string;
  zip?: string;
  status?: string;
};

export type UserProfileUpdate = {
  firstName?: string;
  lastName?: string;
  phone?: string;
  address1?: string;
  address2?: string;
  city?: string;
  state?: string;
  zip?: string;
};

@Injectable({ providedIn: 'root' })
export class UserProfileService {
  private readonly api = 'http://reserveone-env.eba-4wue3g7x.us-east-1.elasticbeanstalk.com';

  constructor(private http: HttpClient) {}

  private readCookie(name: string): string | null {
    const parts = document.cookie.split(';').map((c) => c.trim());
    const prefix = `${encodeURIComponent(name)}=`;
    for (const p of parts) {
      if (p.startsWith(prefix)) return decodeURIComponent(p.substring(prefix.length));
    }
    return null;
  }

  /** Get the currently-authenticated user's profile */
  getMe() {
    return this.http.get<UserProfile>(`${this.api}/users/me`, { withCredentials: true });
  }

  private ensureCsrfToken() {
    return this.http.get<{ headerName: string; token: string }>(`${this.api}/csrf`, {
      withCredentials: true,
    });
  }

  /** Update the currently-authenticated user's profile fields */
  updateMe(payload: UserProfileUpdate) {
    // Spring Security returns the correct header name + token from /csrf.
    // Using that token avoids relying on the cookie value matching.
    return this.ensureCsrfToken().pipe(
      switchMap((csrf) =>
        this.http.patch<UserProfile>(`${this.api}/users/me`, payload ?? {}, {
          withCredentials: true,
          headers: csrf?.token ? { [csrf.headerName || 'X-XSRF-TOKEN']: csrf.token } : undefined,
        })
      )
    );
  }
}
