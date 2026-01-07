import { CommonModule } from '@angular/common';
import {
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  inject,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import { AuthService } from '../../../auth/services/auth.service';
import { UserProfileService, UserProfileUpdate } from './../../../users/user-profile.service';

@Component({
  selector: 'app-user-profile-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './user-profile-modal.html',
  styleUrls: ['./user-profile-modal.css'],
})
export class UserProfileModal implements OnChanges {
  private readonly userProfile = inject(UserProfileService);
  private readonly auth = inject(AuthService);
  private readonly cdr = inject(ChangeDetectorRef);

  @Input() open = false;

  @Output() closed = new EventEmitter<void>();
  @Output() saved = new EventEmitter<void>();

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

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['open']?.currentValue === true) {
      this.loadProfile();
    }
  }

  close(): void {
    this.closed.emit();
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      event.preventDefault();
      this.close();
    }
  }

  save(): void {
    this.saving = true;
    this.error = '';

    this.userProfile
      .updateMe(this.profile)
      .pipe(
        finalize(() => {
          this.saving = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: () => {
          // Keep header labels in sync (backend may now return DB fields)
          this.auth.refreshMe().subscribe();
          this.saved.emit();
          this.cdr.markForCheck();
        },
        error: () => {
          this.error = 'Could not save your profile. Please try again.';
          this.cdr.markForCheck();
        },
      });
  }

  private loadProfile(): void {
    this.loading = true;
    this.error = '';

    this.userProfile
      .getMe()
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (me) => {
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
          this.cdr.markForCheck();
        },
        error: () => {
          this.error = 'Could not load your profile. Please sign in and try again.';
          this.cdr.markForCheck();
        },
      });
  }
}
