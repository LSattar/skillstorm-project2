import { HttpClient } from '@angular/common/http';
import { Injectable, computed, signal } from '@angular/core';
import { catchError, of, tap } from 'rxjs';

export type AuthMe = {
  // Backend returns localUserId (UUID) when authenticated
  localUserId?: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  dbRoles?: string[];
  // Backend currently returns these arrays (names are kept flexible for evolution)
  roles?: string[]; // may include ROLE_* and/or OIDC/SCOPE_*
  principalAuthorities?: string[];
};

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = 'http://localhost:8080';
  private readonly _me = signal<AuthMe | null>(null);

  private readCookie(name: string): string | null {
    const parts = document.cookie.split(';').map((c) => c.trim());
    const prefix = `${encodeURIComponent(name)}=`;
    for (const p of parts) {
      if (p.startsWith(prefix)) return decodeURIComponent(p.substring(prefix.length));
    }
    return null;
  }

  readonly meSignal = this._me.asReadonly();
  readonly isAuthenticated = computed(() => this._me() !== null);

  /**
   * UI-only helper: pick a single role label to display.
   * Prefers highest-privilege role when multiple exist.
   */
  readonly primaryRoleLabel = computed(() => {
    const me = this._me();
    if (!me) return '';

    const normalizeRole = (r: string): string => r.trim().toUpperCase();
    const titleize = (r: string): string =>
      normalizeRole(r)
        .toLowerCase()
        .split('_')
        .filter(Boolean)
        .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
        .join(' ');

    const dbRoleNames = (me.dbRoles ?? [])
      .filter((r): r is string => typeof r === 'string')
      .map(normalizeRole);

    const authorities = [...(me.roles ?? []), ...(me.principalAuthorities ?? [])];

    const roleNames = authorities
      .filter((a): a is string => typeof a === 'string')
      .filter((a) => a.startsWith('ROLE_'))
      .map((a) => a.substring('ROLE_'.length));

    const allRoleNames = Array.from(new Set([...dbRoleNames, ...roleNames.map(normalizeRole)]));

    const elevated = ['ADMIN', 'BUSINESS_OWNER', 'MANAGER', 'EMPLOYEE'];
    for (const p of elevated) {
      if (allRoleNames.includes(p)) return titleize(p);
    }

    // Default role when authenticated but not in an elevated role.
    return 'GUEST';
  });

  constructor(private http: HttpClient) {}

  startGoogleLogin() {
    window.location.href = `${this.api}/oauth2/authorization/google`;
  }

  bootstrapCsrf() {
    return this.http.get(`${this.api}/csrf`, { withCredentials: true });
  }

  refreshMe() {
    return this.http.get<AuthMe>(`${this.api}/auth/me`, { withCredentials: true }).pipe(
      tap((me) => this._me.set(me)),
      catchError(() => {
        this._me.set(null);
        return of(null);
      })
    );
  }

  logout() {
    const xsrf = this.readCookie('XSRF-TOKEN');
    return this.http
      .post(
        `${this.api}/logout`,
        {},
        {
          withCredentials: true,
          headers: xsrf ? { 'X-XSRF-TOKEN': xsrf } : undefined,
        }
      )
      .pipe(
        tap(() => this._me.set(null)),
        catchError((err) => {
          this._me.set(null);
          return of(err);
        })
      );
  }
}
