import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Footer } from '../../../../shared/footer/footer';
import { Header } from '../../../../shared/header/header';
import { AuthService } from '../../../auth/services/auth.service';
import { UserProfile, UserProfileService, UserProfileUpdate } from '../../user-profile.service';

@Component({
  selector: 'app-user-profile-page',
  standalone: true,
  imports: [CommonModule, FormsModule, Header, Footer],
  templateUrl: './user-profile-page.html',
  styleUrls: ['./user-profile-page.css'],
})
export class UserProfilePage {
  protected readonly auth = inject(AuthService);
  get isAuthenticated() {
    return this.auth.isAuthenticated();
  }
  get roleLabel() {
    return this.auth.primaryRoleLabel();
  }
  get userLabel() {
    const me = this.auth.meSignal();
    if (!me) return '';
    const first = me.firstName?.trim();
    if (first) return first;
    return me.email || '';
  }
  get userEmail() {
    return this.auth.meSignal()?.email ?? '';
  }
  profile: UserProfileUpdate = {
    firstName: '',
    lastName: '',
    phone: '',
    address1: '',
    address2: '',
    city: '',
    state: '',
    zip: '',
  };
  loading = false;
  saving = false;
  error = '';

  constructor(private userProfile: UserProfileService, private router: Router) {
    this.loadProfile();
  }

  save(): void {
    this.saving = true;
    this.error = '';
    this.userProfile.updateMe(this.profile).subscribe({
      next: () => {
        this.saving = false;
        this.loading = false;
        this.router.navigate(['/']);
      },
      error: (err: unknown) => {
        this.error = this.formatHttpError('Could not save your profile', err);
        this.saving = false;
        this.loading = false;
      },
    });
  }

  private loadProfile(): void {
    this.loading = true;
    this.error = '';
    this.userProfile.getMe().subscribe({
      next: (me: UserProfile) => {
        if (!me) return;
        this.profile = {
          firstName: me.firstName ?? '',
          lastName: me.lastName ?? '',
          phone: me.phone ?? '',
          address1: me.address1 ?? '',
          address2: me.address2 ?? '',
          city: me.city ?? '',
          state: me.state ?? '',
          zip: me.zip ?? '',
        };
        this.loading = false;
      },
      error: () => {
        this.error = 'Could not load your profile. Please sign in and try again.';
        this.loading = false;
      },
    });
  }

  private formatHttpError(prefix: string, err: unknown): string {
    if (err && typeof err === 'object' && 'status' in err) {
      const status = (err as any).status;
      const maybeBody = (err as any).error;
      const messageFromBody =
        (typeof maybeBody === 'string' && maybeBody.trim()) ||
        (maybeBody && typeof maybeBody === 'object' && (maybeBody.message || maybeBody.error));
      const detail = messageFromBody ? `: ${messageFromBody}` : '';
      if (status === 401) return `${prefix}. Unauthorized (401). Please sign in again.${detail}`;
      if (status === 403) return `${prefix}. Forbidden (403).${detail}`;
      if (status === 400) return `${prefix}. Bad request (400).${detail}`;
      return `${prefix}. Request failed (${status}).${detail}`;
    }
    return `${prefix}. Please try again.`;
  }
}
