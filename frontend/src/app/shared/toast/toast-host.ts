import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { Subscription } from 'rxjs';
import { Toast, ToastService } from '../services/toast.service';

@Component({
  selector: 'app-toast-host',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './toast-host.html',
  styleUrls: ['./toast-host.css'],
})
export class ToastHostComponent implements OnInit, OnDestroy {
  private readonly toastService = inject(ToastService);
  private sub?: Subscription;

  toasts: Toast[] = [];

  // Per-toast timers so hover can pause/resume
  private timeouts = new Map<string, any>();
  private startedAt = new Map<string, number>();
  private remaining = new Map<string, number>();

  ngOnInit(): void {
    this.sub = this.toastService.toasts$.subscribe((t) => {
      this.toasts = t;
      // Start timers for newly added toasts
      for (const toast of t) {
        if (!this.timeouts.has(toast.id)) {
          this.scheduleAutoDismiss(toast);
        }
      }
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
    for (const id of this.timeouts.keys()) {
      clearTimeout(this.timeouts.get(id));
    }
    this.timeouts.clear();
    this.startedAt.clear();
    this.remaining.clear();
  }

  dismiss(id: string): void {
    this.clearTimer(id);
    this.toastService.dismiss(id);
  }

  onMouseEnter(id: string): void {
    // Pause timer
    const timeout = this.timeouts.get(id);
    if (!timeout) return;

    clearTimeout(timeout);
    this.timeouts.delete(id);

    const start = this.startedAt.get(id);
    const rem = this.remaining.get(id);
    if (start != null && rem != null) {
      const elapsed = Date.now() - start;
      this.remaining.set(id, Math.max(0, rem - elapsed));
    }
  }

  onMouseLeave(id: string): void {
    // Resume timer
    const rem = this.remaining.get(id);
    if (rem == null || rem <= 0) return;

    this.startedAt.set(id, Date.now());
    const timeout = setTimeout(() => this.dismiss(id), rem);
    this.timeouts.set(id, timeout);
  }

  private scheduleAutoDismiss(toast: Toast): void {
    const duration = this.getDuration(toast);
    if (duration <= 0) return; // sticky

    this.remaining.set(toast.id, duration);
    this.startedAt.set(toast.id, Date.now());

    const timeout = setTimeout(() => this.dismiss(toast.id), duration);
    this.timeouts.set(toast.id, timeout);
  }

  private getDuration(toast: Toast): number {
    if (toast.durationMs != null) return toast.durationMs;

    // sensible defaults by kind
    switch (toast.kind) {
      case 'success':
        return 2500;
      case 'info':
        return 3000;
      case 'warning':
        return 3500;
      case 'error':
        return 4500;
      case 'loading':
        return 0; // sticky
      default:
        return 3000;
    }
  }

  private clearTimer(id: string): void {
    const timeout = this.timeouts.get(id);
    if (timeout) clearTimeout(timeout);
    this.timeouts.delete(id);
    this.startedAt.delete(id);
    this.remaining.delete(id);
  }

  iconFor(kind: Toast['kind']): string {
    switch (kind) {
      case 'success':
        return '✓';
      case 'error':
        return '!';
      case 'warning':
        return '!';
      case 'info':
        return 'i';
      case 'loading':
        return '';
      default:
        return '';
    }
  }

  trackById(_: number, t: Toast): string {
    return t.id;
  }

  ariaLive(kind: Toast['kind']): 'polite' | 'assertive' {
    return kind === 'error' ? 'assertive' : 'polite';
  }
}
