import { CommonModule } from '@angular/common';
import {
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  Output,
  SimpleChanges,
  inject,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  RoleResponse,
  SystemSettingsService,
  UserAdminView,
} from '../../services/system-settings.service';

@Component({
  selector: 'app-system-settings-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './system-settings-modal.html',
  styleUrls: ['./system-settings-modal.css'],
})
export class SystemSettingsModal {
  private readonly api = inject(SystemSettingsService);
  private readonly cdr = inject(ChangeDetectorRef);

  @Input() open = false;
  @Output() closed = new EventEmitter<void>();

  loading = false;
  error = '';

  // Search
  query = '';
  users: UserAdminView[] = [];
  selected: UserAdminView | null = null;

  // Roles
  roles: RoleResponse[] = [];
  roleToAdd = '';

  // Create user
  creating = false;
  createForm = { firstName: '', lastName: '', email: '' };

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['open']?.currentValue === true) {
      this.bootstrap();
    }
  }

  close(): void {
    this.closed.emit();
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

    this.api.searchUsers(this.query, 25).subscribe({
      next: (users) => {
        this.users = Array.isArray(users) ? users : [];
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
      },
      error: () => {
        this.error = 'Failed to remove role.';
        this.loading = false;
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
      },
      error: () => {
        this.error = 'Failed to add role.';
        this.loading = false;
      },
    });
  }

  deleteSelected(): void {
    if (!this.selected?.userId) return;
    if (!confirm('Delete this user? This cannot be undone.')) return;

    this.loading = true;
    this.api.deleteUser(this.selected.userId).subscribe({
      next: () => {
        const id = this.selected?.userId;
        this.users = this.users.filter((u) => u.userId !== id);
        this.selected = this.users[0] ?? null;
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to delete user.';
        this.loading = false;
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
      },
      error: () => {
        this.error = 'Failed to create user.';
        this.creating = false;
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
