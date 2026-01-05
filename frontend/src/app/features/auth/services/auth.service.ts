import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = 'http://localhost:8080';

  constructor(private http: HttpClient) {}

  startGoogleLogin() {
    window.location.href = `${this.api}/oauth2/authorization/google`;
  }

  bootstrapCsrf() {
    return this.http.get(`${this.api}/csrf`, { withCredentials: true });
  }

  me() {
    return this.http.get(`${this.api}/auth/me`, { withCredentials: true });
  }

  logout() {
    return this.http.post(`${this.api}/logout`, {}, { withCredentials: true });
  }
}
