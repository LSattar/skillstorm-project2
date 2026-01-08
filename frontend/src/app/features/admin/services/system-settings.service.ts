import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export type UserAdminView = {
  userId: string;
  firstName?: string | null;
  lastName?: string | null;
  email?: string | null;
  status?: string;
  roles?: string[];
};

export type RoleResponse = { roleId: string; name: string };

export type UpdateUserRolesRequest = {
  add?: string[];
  remove?: string[];
};

export type CreateUserRequest = {
  firstName?: string;
  lastName?: string;
  email?: string;
};

@Injectable({ providedIn: 'root' })
export class SystemSettingsService {
  private readonly api = environment.apiBaseUrl;

  constructor(private http: HttpClient) {}

  searchUsers(q: string, limit = 20): Observable<UserAdminView[]> {
    return this.http.get<UserAdminView[]>(`${this.api}/users/search`, {
      params: { q: q ?? '', limit: String(limit) },
      withCredentials: true,
    });
  }

  getRoles(): Observable<RoleResponse[]> {
    return this.http.get<RoleResponse[]>(`${this.api}/roles`, { withCredentials: true });
  }

  updateUserRoles(userId: string, body: UpdateUserRolesRequest): Observable<UserAdminView> {
    return this.http.patch<UserAdminView>(`${this.api}/users/${userId}/roles`, body, {
      withCredentials: true,
    });
  }

  deleteUser(userId: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/users/${userId}`, { withCredentials: true });
  }

  createUser(body: CreateUserRequest): Observable<UserAdminView> {
    return this.http.post<UserAdminView>(`${this.api}/users`, body, { withCredentials: true });
  }
}
