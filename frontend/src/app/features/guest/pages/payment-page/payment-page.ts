import { CommonModule } from '@angular/common';
import { Component, OnInit, ViewChild, computed, inject } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { StripeCardElementOptions, StripeElementsOptions } from '@stripe/stripe-js';
import { StripeCardComponent, StripeService } from 'ngx-stripe';
import { Footer } from '../../../../shared/footer/footer';
import { Header } from '../../../../shared/header/header';
import {
  ReservationResponse,
  ReservationService,
} from '../../../admin/services/reservation.service';
import { AuthService } from '../../../auth/services/auth.service';
import { PaymentService } from '../../services/payment.service';

@Component({
  selector: 'app-payment-page',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, Header, Footer, StripeCardComponent],
  templateUrl: './payment-page.html',
  styleUrl: './payment-page.css',
})
export class PaymentPage implements OnInit {
  @ViewChild(StripeCardComponent) card!: StripeCardComponent;

  protected readonly auth = inject(AuthService);
  protected readonly reservationService = inject(ReservationService);
  protected readonly paymentService = inject(PaymentService);
  protected readonly stripeService = inject(StripeService);
  protected readonly router = inject(Router);
  protected readonly route = inject(ActivatedRoute);
  protected readonly fb = inject(FormBuilder);

  protected readonly isAuthenticated = this.auth.isAuthenticated;
  protected readonly roleLabel = this.auth.primaryRoleLabel;
  protected readonly userLabel = computed(() => {
    const me = this.auth.meSignal();
    if (!me) return '';
    const first = me.firstName?.trim();
    if (first) return first;
    return me.email || '';
  });

  reservation: ReservationResponse | null = null;
  loading = false;
  error: string | null = null;
  paymentProcessing = false;

  paymentForm!: FormGroup;

  cardOptions: StripeCardElementOptions = {
    style: {
      base: {
        iconColor: '#344e41',
        color: '#344e41',
        fontWeight: '400',
        fontFamily: '"Helvetica Neue", Helvetica, sans-serif',
        fontSize: '16px',
        '::placeholder': {
          color: '#a3b18a',
        },
      },
      invalid: {
        iconColor: '#ddaeb9',
        color: '#ddaeb9',
      },
    },
  };

  elementsOptions: StripeElementsOptions = {
    locale: 'en',
  };

  isNavOpen = false;
  showProfileModal = false;

  ngOnInit(): void {
    const reservationId = this.route.snapshot.paramMap.get('id');
    if (!reservationId) {
      this.error = 'Reservation ID is required';
      return;
    }

    this.paymentForm = this.fb.group({
      name: ['', [Validators.required]],
    });

    this.loadReservation(reservationId);
  }

  private loadReservation(id: string): void {
    this.loading = true;
    this.error = null;

    this.reservationService.getReservationById(id).subscribe({
      next: (reservation) => {
        this.reservation = reservation;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        if (err.error?.detail) {
          this.error = err.error.detail;
        } else if (err.error?.message) {
          this.error = err.error.message;
        } else {
          this.error = 'Failed to load reservation. Please try again.';
        }
      },
    });
  }

  formatCurrency(amount: number, currency: string = 'USD'): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency,
    }).format(amount);
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  }

  processPayment(): void {
    if (!this.reservation) {
      this.error = 'Reservation not found';
      return;
    }

    if (this.paymentForm.invalid) {
      this.error = 'Please fill in all required fields';
      return;
    }

    this.paymentProcessing = true;
    this.error = null;

    const name = this.paymentForm.get('name')?.value;

    // Step 1: Create payment intent on backend
    this.paymentService
      .createPaymentIntent({
        reservationId: this.reservation.reservationId,
        amount: this.reservation.totalAmount,
        currency: this.reservation.currency,
      })
      .subscribe({
        next: (intentResponse) => {
          // Step 2: Create payment method and confirm payment with Stripe
          this.stripeService
            .createPaymentMethod({
              type: 'card',
              card: this.card.element,
              billing_details: {
                name: name,
              },
            })
            .subscribe((result) => {
              if (result.error) {
                this.paymentProcessing = false;
                this.error = result.error.message || 'Payment method creation failed';
                return;
              }

              if (!result.paymentMethod) {
                this.paymentProcessing = false;
                this.error = 'Failed to create payment method';
                return;
              }

              // Step 3: Confirm payment with Stripe
              this.stripeService
                .confirmCardPayment(intentResponse.clientSecret, {
                  payment_method: result.paymentMethod.id,
                })
                .subscribe((confirmResult) => {
                  if (confirmResult.error) {
                    this.paymentProcessing = false;
                    this.error = confirmResult.error.message || 'Payment confirmation failed';
                    return;
                  }

                  if (!confirmResult.paymentIntent) {
                    this.paymentProcessing = false;
                    this.error = 'Payment confirmation failed';
                    return;
                  }

                  // Step 4: Confirm payment with backend
                  this.paymentService
                    .confirmPayment(confirmResult.paymentIntent.id, this.reservation!.reservationId)
                    .subscribe({
                      next: () => {
                        // Step 5: Redirect to success page or back to bookings
                        this.router.navigate(['/my-bookings'], {
                          queryParams: { payment: 'success' },
                        });
                      },
                      error: (err) => {
                        this.paymentProcessing = false;
                        if (err.error?.detail) {
                          this.error = err.error.detail;
                        } else if (err.error?.message) {
                          this.error = err.error.message;
                        } else {
                          this.error = 'Payment confirmation failed. Please contact support.';
                        }
                      },
                    });
                });
            });
        },
        error: (err) => {
          this.paymentProcessing = false;
          if (err.error?.detail) {
            this.error = err.error.detail;
          } else if (err.error?.message) {
            this.error = err.error.message;
          } else {
            this.error = 'Failed to create payment intent. Please try again.';
          }
        },
      });
  }

  toggleNav(): void {
    this.isNavOpen = !this.isNavOpen;
  }

  closeNav(): void {
    this.isNavOpen = false;
  }

  openBooking(): void {
    this.router.navigate(['/']);
  }

  openSignIn(): void {
    this.auth.startGoogleLogin();
  }

  signOut(): void {
    this.auth.logout().subscribe(() => {
      this.router.navigate(['/']);
    });
  }

  openProfile(): void {
    this.showProfileModal = true;
  }

  closeProfile(): void {
    this.showProfileModal = false;
  }

  openSystemSettings(): void {
    // Not applicable for payment page
  }

  goBack(): void {
    this.router.navigate(['/my-bookings']);
  }
}
