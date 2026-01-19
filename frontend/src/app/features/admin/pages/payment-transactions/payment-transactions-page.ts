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

import { Subscription, interval } from 'rxjs';
import { ReportsService } from '../../services/reports.service';

type KpiFilter = 'ALL' | 'SUCCEEDED' | 'PROCESSING' | 'FAILED';

@Component({
  selector: 'app-payment-transactions-page',
  standalone: true,
  imports: [CommonModule, FormsModule, Header],
  templateUrl: './payment-transactions-page.html',
  styleUrls: ['./payment-transactions-page.css'],
})
export class PaymentTransactionsPage implements OnInit {
  private pollingSubscription: Subscription | null = null;
  private readonly api = inject(PaymentTransactionsService);
  private readonly router = inject(Router);
  protected readonly auth = inject(AuthService);
  private readonly reportsService = inject(ReportsService);

  // Header bindings
  isNavOpen = false;

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
    return first || me.email || '';
  }
  get userEmail() {
    return this.auth.meSignal()?.email ?? '';
  }

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

  // KPI filter state
  activeKpi: KpiFilter = 'ALL';

  // Filters/search/pagination
  query = '';
  status: PaymentTransactionStatus | '' = '';
  fromDate = '';
  toDate = '';
  page = 0;
  size = 20;
  sort = 'createdAt,desc';

  // Data state
  loading = false;
  error: string | null = null;
  transactions: PaymentTransaction[] = [];
  total = 0;

  // KPI values (computed from current page results)
  paidCount = 0;
  pendingCount = 0;
  failedCount = 0;
  summaryRevenue = 0;

  // Skeleton state
  skeletonStopped = false;
  get skeletonRows(): number[] {
    return Array.from({ length: 5 }, (_, i) => i);
  }

  // Modal state (if you have a modal in the template)
  selected: PaymentTransaction | null = null;
  showDetailsModal = false;

  // Report download state
  reportLoading = false;
  reportError: string | null = null;

  ngOnInit(): void {
    this.loadTransactions();
    this.startPolling();
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }

  public trackById(index: number, tx: PaymentTransaction) {
    return tx.id;
  }

  public retryLoadTransactions(): void {
    this.loadTransactions();
  }

  // Date preset logic
  public setDatePreset(preset: 'today' | 'last7' | 'last30' | 'month') {
    const now = new Date();
    let from = '';
    let to = '';

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
  public onKpiClick(type: KpiFilter) {
    if (this.activeKpi === type) {
      this.activeKpi = 'ALL';
      this.status = '';
    } else {
      this.activeKpi = type;
      this.status = type === 'ALL' ? '' : type;
    }
    this.onSearch();
  }

  public onClearFilters(): void {
    this.query = '';
    this.status = '';
    this.fromDate = '';
    this.toDate = '';
    this.page = 0;
    this.activeKpi = 'ALL';
    this.loadTransactions();
  }

  public onSearch(): void {
    this.page = 0;
    this.loadTransactions();
  }

  public onPageChange(newPage: number): void {
    this.page = newPage;
    this.loadTransactions();
  }

  loadTransactions(): void {
    this.loading = true;
    this.skeletonStopped = false;
    this.error = null;

    const skeletonTimeout = setTimeout(() => {
      this.skeletonStopped = true;
    }, 1000);

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

          // KPIs are computed from the current page results
          this.paidCount = this.transactions.filter((t) => t.status === 'SUCCEEDED').length;
          this.pendingCount = this.transactions.filter((t) => t.status === 'PROCESSING').length;
          this.failedCount = this.transactions.filter((t) => t.status === 'FAILED').length;

          this.summaryRevenue = this.transactions
            .filter((t) => t.status === 'SUCCEEDED')
            .reduce((sum, t) => sum + (t.amount ?? 0), 0);

          this.loading = false;
          this.skeletonStopped = true;
          clearTimeout(skeletonTimeout);

          // If no more PROCESSING transactions, stop polling
          if (this.pendingCount === 0) {
            this.stopPolling();
          } else {
            this.startPolling();
          }
        },
        error: () => {
          this.error = 'Failed to load payment transactions.';
          this.loading = false;
          this.skeletonStopped = true;
          clearTimeout(skeletonTimeout);
        },
      });
  }

  // Polling logic: refresh every 5 seconds if any PROCESSING transactions
  private startPolling(): void {
    if (this.pollingSubscription) return;
    this.pollingSubscription = interval(5000).subscribe(() => {
      // Only poll if there are PROCESSING transactions
      if (this.pendingCount > 0) {
        this.loadTransactions();
      } else {
        this.stopPolling();
      }
    });
  }

  private stopPolling(): void {
    if (this.pollingSubscription) {
      this.pollingSubscription.unsubscribe();
      this.pollingSubscription = null;
    }
  }

  public openDetailsModal(tx: PaymentTransaction): void {
    this.selected = tx;
    this.showDetailsModal = true;
    document.body.style.overflow = 'hidden';
  }

  public closeDetailsModal(): void {
    this.showDetailsModal = false;
    this.selected = null;
    document.body.style.overflow = '';
  }

  public formatCurrency(amount: number, currency = 'USD'): string {
    const safeAmount = Number.isFinite(amount) ? amount : 0;
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency,
    }).format(safeAmount);
  }

  public formatDate(dateString: string): string {
    if (!dateString) return '—';
    const d = new Date(dateString);
    if (!Number.isFinite(d.getTime())) return '—';
    return d.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  }

  public computeNights(checkIn: string, checkOut: string): number {
    const start = new Date(checkIn);
    const end = new Date(checkOut);
    if (!Number.isFinite(start.getTime()) || !Number.isFinite(end.getTime())) return 0;
    return Math.max(1, Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)));
  }

  public getStatusClass(status: PaymentTransactionStatus): string {
    const statusMap: Record<PaymentTransactionStatus, string> = {
      SUCCEEDED: 'status-confirmed',
      PROCESSING: 'status-pending',
      FAILED: 'status-cancelled',
      REFUNDED: 'status-checked-out',
      CANCELLED: 'status-default',
    };
    return statusMap[status] || 'status-default';
  }

  // Bottom-of-page button calls this
  public downloadAnnualPaymentReport(): void {
    if (this.reportLoading) return;

    this.reportLoading = true;
    this.reportError = null;

    // Rolling past 12 months
    const toDate = new Date();
    const fromDate = new Date();
    fromDate.setFullYear(toDate.getFullYear() - 1);

    const from = this.formatIsoDate(fromDate);
    const to = this.formatIsoDate(toDate);

    this.reportsService.generatePaymentTransactionReport(from, to).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `payment-transactions-report-${from}_to_${to}.csv`;
        document.body.appendChild(a);
        a.click();
        a.remove();
        window.URL.revokeObjectURL(url);

        this.reportLoading = false;
      },
      error: (err) => {
        console.error('Failed to generate annual payment report:', err);
        this.reportError = 'Failed to generate report. Please try again.';
        this.reportLoading = false;
      },
    });
  }

  private formatIsoDate(d: Date): string {
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
