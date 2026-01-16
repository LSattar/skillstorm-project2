import { CommonModule, DOCUMENT } from '@angular/common';
import { ChangeDetectorRef, Component, effect, inject, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Header } from '../../../../shared/header/header';
import { AuthService } from '../../../auth/services/auth.service';
import {
  UserProfile,
  UserProfileService,
  UserProfileUpdate,
} from '../../services/user-profile.service';
import { ToastService } from './../../../../shared/services/toast.service';

@Component({
  selector: 'app-user-profile-page',
  standalone: true,
  imports: [CommonModule, FormsModule, Header],
  templateUrl: './user-profile-page.html',
  styleUrls: ['./user-profile-page.css'],
})
export class UserProfilePage implements OnInit, OnDestroy {
  private readonly document = inject(DOCUMENT);

  // Scroll lock listeners
  private removeWheel?: () => void;
  private removeTouch?: () => void;
  private removeKeydown?: () => void;

  protected readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);

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

  private profileLoaded = false;
  private readonly userProfile = inject(UserProfileService);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);

  constructor() {
    // Watch for auth state to become available, then load profile
    effect(() => {
      const me = this.auth.meSignal();
      if (me?.localUserId && !this.profileLoaded) {
        this.profileLoaded = true;
        this.loadProfile();
      }
    });

    // Try to load profile immediately if auth is already ready
    const me = this.auth.meSignal();
    if (me?.localUserId && !this.profileLoaded) {
      this.profileLoaded = true;
      this.loadProfile();
    }
  }

  ngOnInit(): void {
    // 1) CSS lock
    this.document.body.classList.add('page-lock');

    // 2) Hard block scroll input (macOS bounce killer)
    this.installHardScrollBlock();

    // load data
    this.loadProfile();
  }

  ngOnDestroy(): void {
    this.uninstallHardScrollBlock();
    this.document.body.classList.remove('page-lock');
  }

  private installHardScrollBlock(): void {
    const doc = this.document;

    const block = (e: Event) => {
      // If we are on a locked page, block scrolling input entirely
      if (doc.body.classList.contains('page-lock')) {
        e.preventDefault();
      }
    };

    const blockKeys = (e: KeyboardEvent) => {
      if (!doc.body.classList.contains('page-lock')) return;

      // Keys that scroll the viewport
      const scrollKeys = new Set([
        'ArrowUp',
        'ArrowDown',
        'PageUp',
        'PageDown',
        'Home',
        'End',
        ' ',
        'Spacebar',
      ]);

      if (scrollKeys.has(e.key)) {
        e.preventDefault();
      }
    };

    // Wheel (trackpad/mouse wheel)
    doc.addEventListener('wheel', block, { passive: false });

    // Touch scrolling (mobile/mac trackpad edge cases)
    doc.addEventListener('touchmove', block, { passive: false });

    // Keyboard scrolling
    doc.addEventListener('keydown', blockKeys, { passive: false });

    this.removeWheel = () => doc.removeEventListener('wheel', block as any);
    this.removeTouch = () => doc.removeEventListener('touchmove', block as any);
    this.removeKeydown = () => doc.removeEventListener('keydown', blockKeys as any);
  }

  private uninstallHardScrollBlock(): void {
    this.removeWheel?.();
    this.removeTouch?.();
    this.removeKeydown?.();
    this.removeWheel = undefined;
    this.removeTouch = undefined;
    this.removeKeydown = undefined;
  }

  save(): void {
    if (this.saving || this.loading) return;

    this.saving = true;
    this.error = '';

    // Optional: show a sticky "loading" toast while the request is in flight
    const loadingToastId = this.toast.loading('Saving your profile…');

    this.userProfile.updateMe(this.profile).subscribe({
      next: () => {
        // Optional safety: only dismiss if we actually received an id
        if (loadingToastId) {
          this.toast.dismiss(loadingToastId);
        }

        this.saving = false;
        this.loading = false;

        // Success toast
        this.toast.success('Your profile changes were saved.');
      },
      error: (err: unknown) => {
        // Optional safety: only dismiss if we actually received an id
        if (loadingToastId) {
          this.toast.dismiss(loadingToastId);
        }

        this.error = this.formatHttpError('Could not save your profile', err);
        this.saving = false;
        this.loading = false;

        // Friendly error toast
        this.toast.error(this.error);
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
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'Could not load your profile. Please sign in and try again.';
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  signOut(): void {
    this.auth.logout().subscribe({
      next: () => this.router.navigate(['/']),
      error: () => this.router.navigate(['/']),
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
