import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Footer } from '../../../../shared/footer/footer';
import { Header } from '../../../../shared/header/header';
import { AuthService } from '../../../auth/services/auth.service';
import { UserProfileModal } from '../../../users/components/user-profile-modal/user-profile-modal';
import {
  PaymentTransaction,
  PaymentTransactionStatus,
  PaymentTransactionsService,
} from '../../services/payment-transactions.service';

@Component({
  selector: 'app-payment-transactions-page',
  standalone: true,
  imports: [CommonModule, FormsModule, Header, Footer, UserProfileModal],
  templateUrl: './payment-transactions-page.html',
  styleUrls: ['./payment-transactions-page.css'],
})
export class PaymentTransactionsPage implements OnInit {
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
    this.isProfileOpen = true;
    document.body.style.overflow = 'hidden';
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
    this.error = null;
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
        },
        error: () => {
          this.error = 'Failed to load payment transactions.';
          this.loading = false;
        },
      });
  }

  onSearch(): void {
    this.page = 0;
    this.loadTransactions();
  }

  onClearFilters(): void {
    this.query = '';
    this.status = '';
    this.fromDate = '';
    this.toDate = '';
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
