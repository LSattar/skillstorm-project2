import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export type ToastKind = 'success' | 'error' | 'warning' | 'info' | 'loading';

export interface Toast {
  id: string;
  kind: ToastKind;
  title?: string;
  message: string;
  durationMs?: number; // undefined = default by kind, 0 = sticky
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private readonly _toasts$ = new BehaviorSubject<Toast[]>([]);
  readonly toasts$ = this._toasts$.asObservable();

  private nextId(): string {
    return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
  }

  show(toast: Omit<Toast, 'id'>): string {
    const id = this.nextId();
    const created: Toast = { id, ...toast };
    this._toasts$.next([created, ...this._toasts$.value]);
    return id;
  }

  dismiss(id: string): void {
    this._toasts$.next(this._toasts$.value.filter((t) => t.id !== id));
  }

  clear(): void {
    this._toasts$.next([]);
  }

  // Convenience helpers

  success(message: string, title = 'Saved', durationMs = 2500): string {
    return this.show({ kind: 'success', title, message, durationMs });
  }

  error(message: string, title = 'Something went wrong', durationMs = 4500): string {
    return this.show({ kind: 'error', title, message, durationMs });
  }

  warning(message: string, title = 'Heads up', durationMs = 3500): string {
    return this.show({ kind: 'warning', title, message, durationMs });
  }

  info(message: string, title = 'Info', durationMs = 3000): string {
    return this.show({ kind: 'info', title, message, durationMs });
  }

  loading(message: string, title = 'Working…'): string {
    // sticky until dismissed
    return this.show({ kind: 'loading', title, message, durationMs: 0 });
  }
}
