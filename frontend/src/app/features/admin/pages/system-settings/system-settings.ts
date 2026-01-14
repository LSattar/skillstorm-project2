import { CommonModule, DOCUMENT } from '@angular/common';
import { ChangeDetectorRef, Component, OnDestroy, OnInit, computed, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { Header } from '../../../../shared/header/header';
import { AuthService } from '../../../auth/services/auth.service';

import {
  RoleResponse,
  SystemSettingsService,
  UserAdminView,
} from '../../services/system-settings.service';

@Component({
  selector: 'app-system-settings-page',
  standalone: true,
  imports: [CommonModule, FormsModule, Header],
  templateUrl: './system-settings.html',
  styleUrls: ['./system-settings.css'],
})
export class SystemSettingsPage implements OnInit, OnDestroy {
  private readonly document = inject(DOCUMENT);

  // Scroll lock listeners
  private removeWheel?: () => void;
  private removeTouch?: () => void;
  private removeKeydown?: () => void;

  private readonly api = inject(SystemSettingsService);
  private readonly cdr = inject(ChangeDetectorRef);
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  // Header bindings for <app-header>
  isNavOpen = false;
  toggleNav() {}
  closeNav() {}
  openBooking() {}
  openSignIn() {}
  goToPaymentTransactions() {
    throw new Error('Method not implemented.');
  }
  signOut() {
    this.auth.logout().subscribe({
      next: () => {
        localStorage.clear();
        sessionStorage.clear();
        this.router.navigate(['/']);
      },
      error: () => {
        localStorage.clear();
        sessionStorage.clear();
        this.router.navigate(['/']);
      },
    });
  }
  openProfile() {
    this.router.navigate(['/profile-settings']);
  }
  goToSystemSettings() {}

  isAuthenticated = this.auth.isAuthenticated;
  roleLabel = this.auth.primaryRoleLabel;
  userLabel = computed(() => {
    const me = this.auth.meSignal();
    if (!me) return '';
    const first = me.firstName?.trim();
    if (first) return first;
    return me.email || '';
  });
  userEmail = computed(() => this.auth.meSignal()?.email ?? '');
  showSystemSettings = this.auth.isAdmin;

  loading = false;
  error = '';

  query = '';
  statusFilter: 'ACTIVE' | 'INACTIVE' | 'ALL' = 'ACTIVE';
  users: UserAdminView[] = [];
  selected: UserAdminView | null = null;

  roles: RoleResponse[] = [];
  roleToAdd = '';

  creating = false;
  createForm = { firstName: '', lastName: '', email: '' };

  ngOnInit(): void {
    // 1) CSS lock
    this.document.body.classList.add('page-lock');

    // 2) Hard block scroll input (macOS bounce killer)
    this.installHardScrollBlock();

    this.bootstrap();
  }

  ngOnDestroy(): void {
    this.uninstallHardScrollBlock();
    this.document.body.classList.remove('page-lock');
  }

  private installHardScrollBlock(): void {
    const doc = this.document;

    const block = (e: Event) => {
      if (doc.body.classList.contains('page-lock')) {
        e.preventDefault();
      }
    };

    const blockKeys = (e: KeyboardEvent) => {
      if (!doc.body.classList.contains('page-lock')) return;

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

    doc.addEventListener('wheel', block, { passive: false });
    doc.addEventListener('touchmove', block, { passive: false });
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

  bootstrap(): void {
    this.error = '';
    this.loading = true;
    this.selected = null;
    this.users = [];
    this.cdr.markForCheck();

    this.api.getRoles().subscribe({
      next: (roles) => {
        this.roles = Array.isArray(roles) ? roles : [];
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load roles.';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  search(): void {
    this.error = '';
    this.loading = true;
    this.cdr.markForCheck();

    this.api.searchUsers(this.query, this.statusFilter, 0, 25).subscribe({
      next: (result) => {
        if (Array.isArray(result)) {
          this.users = result;
        } else if (Array.isArray((result as any)?.content)) {
          this.users = (result as any).content;
        } else {
          this.users = [];
        }
        this.selected = this.users[0] ?? null;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Search failed.';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  selectUser(u: UserAdminView): void {
    this.selected = u;
    this.roleToAdd = '';
  }

  removeRole(roleName: string): void {
    if (!this.selected?.userId) return;
    this.loading = true;
    this.api.updateUserRoles(this.selected.userId, { remove: [roleName] }).subscribe({
      next: (updated) => {
        this.patchLocalUser(updated);
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'Failed to remove role.';
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  addRole(): void {
    const roleName = (this.roleToAdd || '').trim();
    if (!roleName || !this.selected?.userId) return;

    this.loading = true;
    this.api.updateUserRoles(this.selected.userId, { add: [roleName] }).subscribe({
      next: (updated) => {
        this.patchLocalUser(updated);
        this.roleToAdd = '';
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'Failed to add role.';
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  deactivateSelected(): void {
    if (!this.selected?.userId) return;
    if (this.selected.userId === this.auth.meSignal()?.localUserId) {
      this.error = 'You cannot deactivate your own account.';
      return;
    }
    if (!confirm('Deactivate this user?')) return;

    this.loading = true;
    this.api.deactivateUser(this.selected.userId).subscribe({
      next: () => {
        this.selected = { ...this.selected!, status: 'INACTIVE' };
        this.users = this.users.map((u) =>
          u.userId === this.selected?.userId ? { ...u, status: 'INACTIVE' } : u
        );
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'Failed to deactivate user.';
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  activateSelected(): void {
    if (!this.selected?.userId) return;
    if (!confirm('Activate this user?')) return;

    this.loading = true;
    this.api.activateUser(this.selected.userId).subscribe({
      next: () => {
        this.selected = { ...this.selected!, status: 'ACTIVE' };
        this.users = this.users.map((u) =>
          u.userId === this.selected?.userId ? { ...u, status: 'ACTIVE' } : u
        );
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'Failed to activate user.';
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  createUser(): void {
    this.error = '';
    this.creating = true;

    this.api.createUser({ ...this.createForm }).subscribe({
      next: (created) => {
        this.users = [created, ...this.users];
        this.selected = created;
        this.createForm = { firstName: '', lastName: '', email: '' };
        this.creating = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'Failed to create user.';
        this.creating = false;
        this.cdr.detectChanges();
      },
    });
  }

  private patchLocalUser(updated: UserAdminView): void {
    this.selected = updated;
    this.users = this.users.map((u) => (u.userId === updated.userId ? updated : u));
  }

  get selectedRoles(): string[] {
    return (this.selected?.roles ?? []).filter((r): r is string => typeof r === 'string').sort();
  }

  get availableRoleNames(): string[] {
    const existing = new Set(this.selectedRoles.map((r) => r.trim().toUpperCase()));
    return (this.roles ?? [])
      .map((r) => r?.name)
      .filter((n): n is string => typeof n === 'string' && n.trim().length > 0)
      .filter((n) => !existing.has(n.trim().toUpperCase()))
      .sort();
  }
}
