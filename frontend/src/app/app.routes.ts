import { Routes } from '@angular/router';
import { AdminDashboard } from './features/admin/pages/admin-dashboard/admin-dashboard';
import { ReservationLookup } from './features/admin/pages/reservation-lookup/reservation-lookup';
import { ForbiddenPage } from './features/landing/pages/forbidden/forbidden';
import { LandingPage } from './features/landing/pages/landing-page/landing-page';
import { NotFoundPage } from './features/landing/pages/not-found/not-found';

import { SystemSettingsPage } from './features/admin/pages/system-settings/system-settings';
import { adminGuard } from './features/auth/guards/admin.guard';

import { PaymentTransactionsPage } from './features/admin/pages/payment-transactions/payment-transactions-page';

export const routes: Routes = [
  { path: '', component: LandingPage },
  { path: 'admin-dashboard', component: AdminDashboard, canActivate: [adminGuard] },
  { path: 'admin/system-settings', component: SystemSettingsPage, canActivate: [adminGuard] },
  { path: 'admin/reservations', component: ReservationLookup, canActivate: [adminGuard] },
  {
    path: 'admin/payment-transactions',
    component: PaymentTransactionsPage,
    canActivate: [adminGuard],
  },
  { path: 'forbidden', component: ForbiddenPage },
  { path: 'error', component: NotFoundPage },
  { path: '**', component: NotFoundPage },
];
