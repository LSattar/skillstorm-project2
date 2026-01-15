import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Header } from '../../../../shared/header/header';
import { AuthService } from '../../../auth/services/auth.service';

import {
  PaymentTransaction,
  PaymentTransactionStatus,
  PaymentTransactionsService,
} from '../../services/payment-transactions.service';

@Component({
  selector: 'app-payment-transactions-page',
  standalone: true,
  imports: [CommonModule, FormsModule, Header],
  templateUrl: './payment-transactions-page.html',
  styleUrls: ['./payment-transactions-page.css'],
})
export class PaymentTransactionsPage implements OnInit {
  // Retry handler for error state: show skeleton for 1s, then show error again
  retryLoadTransactions(): void {
    this.loading = true;
    this.skeletonStopped = false;
    setTimeout(() => {
      this.skeletonStopped = true;
      this.loading = false;
    }, 1000);
  }
  skeletonStopped = false;
  // KPI filter state
  activeKpi: 'ALL' | 'PAID' | 'PENDING' | 'FAILED' = 'ALL';

  // Date preset logic
  setDatePreset(preset: 'today' | 'last7' | 'last30' | 'month') {
    const now = new Date();
    let from = '',
      to = '';
    if (preset === 'today') {
      from = to = now.toISOString().slice(0, 10);
    } else if (preset === 'last7') {
      const d = new Date(now);
      d.setDate(d.getDate() - 6);
      from = d.toISOString().slice(0, 10);
      to = now.toISOString().slice(0, 10);
    } else if (preset === 'last30') {
      const d = new Date(now);
      d.setDate(d.getDate() - 29);
      from = d.toISOString().slice(0, 10);
      to = now.toISOString().slice(0, 10);
    } else if (preset === 'month') {
      from = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().slice(0, 10);
      to = now.toISOString().slice(0, 10);
    }
    this.fromDate = from;
    this.toDate = to;
    this.onSearch();
  }

  // KPI card click handler
  onKpiClick(type: 'ALL' | 'PAID' | 'PENDING' | 'FAILED') {
    if (this.activeKpi === type) {
      // Clicking active KPI resets to ALL
      this.activeKpi = 'ALL';
      this.status = '';
    } else {
      this.activeKpi = type;
      this.status = type === 'ALL' ? '' : type;
    }
    this.onSearch();
  }

  // Override onClearFilters to reset KPI and dates
  onClearFilters(): void {
    this.query = '';
    this.status = '';
    this.fromDate = '';
    this.toDate = '';
    this.page = 0;
    this.activeKpi = 'ALL';
    this.loadTransactions();
  }

  // For skeleton loading rows
  get skeletonRows(): number[] {
    return Array.from({ length: 5 }, (_, i) => i);
  }
  isProfileOpen = false;
  protected readonly auth = inject(AuthService);

  // Header bindings

  // Admin header bindings (copied from admin-dashboard)
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

  // Admin header actions
  toggleNav() {
    this.isNavOpen = !this.isNavOpen;
  }
  closeNav() {
    this.isNavOpen = false;
  }
  openBooking() {}
  openSignIn() {}
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

  closeProfile() {
    this.isProfileOpen = false;
    document.body.style.overflow = '';
  }
  private readonly api = inject(PaymentTransactionsService);
  private readonly router = inject(Router);

  // Header bindings
  isNavOpen = false;
  today = new Date();

  trackById(index: number, tx: PaymentTransaction) {
    return tx.id;
  }

  // Filters/search/sort/pagination
  query = '';
  status: PaymentTransactionStatus | '' = '';
  fromDate = '';
  toDate = '';
  page = 0;
  size = 20;
  sort = 'createdAt,desc';

  loading = false;
  error: string | null = null;
  transactions: PaymentTransaction[] = [];
  total = 0;
  paidCount = 0;
  pendingCount = 0;
  failedCount = 0;
  summaryRevenue = 0;

  selected: PaymentTransaction | null = null;
  showDetailsModal = false;

  ngOnInit(): void {
    this.loadTransactions();
  }

  loadTransactions(): void {
    this.loading = true;
    this.skeletonStopped = false;
    this.error = null;
    const skeletonTimeout = setTimeout(() => {
      this.skeletonStopped = true;
    }, 1000);
    setTimeout(() => {
      this.api
        .getTransactions({
          query: this.query,
          status: this.status,
          from: this.fromDate,
          to: this.toDate,
          page: this.page,
          size: this.size,
          sort: this.sort,
        })
        .subscribe({
          next: (result) => {
            this.transactions = result.content || [];
            this.total = result.totalElements || 0;
            this.paidCount = this.transactions.filter((t) => t.status === 'PAID').length;
            this.pendingCount = this.transactions.filter((t) => t.status === 'PENDING').length;
            this.failedCount = this.transactions.filter((t) => t.status === 'FAILED').length;
            this.summaryRevenue = this.transactions
              .filter((t) => t.status === 'PAID')
              .reduce((sum, t) => sum + (t.total || 0), 0);
            this.loading = false;
            this.skeletonStopped = true;
            clearTimeout(skeletonTimeout);
          },
          error: () => {
            this.error = 'Failed to load payment transactions.';
            this.loading = false;
            this.skeletonStopped = true;
            clearTimeout(skeletonTimeout);
          },
        });
    }, 2000);
  }

  onSearch(): void {
    this.page = 0;
    this.loadTransactions();
  }

  onPageChange(newPage: number): void {
    this.page = newPage;
    this.loadTransactions();
  }

  openDetailsModal(tx: PaymentTransaction): void {
    this.selected = tx;
    this.showDetailsModal = true;
    document.body.style.overflow = 'hidden';
  }

  closeDetailsModal(): void {
    this.showDetailsModal = false;
    this.selected = null;
    document.body.style.overflow = '';
  }

  formatCurrency(amount: number, currency = 'USD'): string {
    const safeAmount = Number.isFinite(amount) ? amount : 0;
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency,
    }).format(safeAmount);
  }

  formatDate(dateString: string): string {
    if (!dateString) return '—';
    const d = new Date(dateString);
    if (!Number.isFinite(d.getTime())) return '—';
    return d.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  }

  computeNights(checkIn: string, checkOut: string): number {
    const start = new Date(checkIn);
    const end = new Date(checkOut);
    if (!Number.isFinite(start.getTime()) || !Number.isFinite(end.getTime())) return 0;
    return Math.max(1, Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)));
  }

  getStatusClass(status: PaymentTransactionStatus): string {
    // Reuse badge classes from reservation pages
    const statusMap: Record<string, string> = {
      PAID: 'status-confirmed',
      PENDING: 'status-pending',
      FAILED: 'status-cancelled',
      REFUNDED: 'status-checked-out',
    };
    return statusMap[status] || 'status-default';
  }
}
