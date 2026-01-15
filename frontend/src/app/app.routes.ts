import { Routes } from '@angular/router';
import { AdminDashboard } from './features/admin/pages/admin-dashboard/admin-dashboard';
import { ReservationLookup } from './features/admin/pages/reservation-lookup/reservation-lookup';
import { RoomManagement } from './features/admin/pages/room-management/room-management';
import { ForbiddenPage } from './features/landing/pages/forbidden/forbidden';
import { LandingPage } from './features/landing/pages/landing-page/landing-page';
import { NotFoundPage } from './features/landing/pages/not-found/not-found';

import { SystemSettingsPage } from './features/admin/pages/system-settings/system-settings';
import { adminGuard } from './features/auth/guards/admin.guard';
import { GuestDashboard } from './features/guest/pages/guest-dashboard/guest-dashboard';
import { PaymentPage } from './features/guest/pages/payment-page/payment-page';
import { UserProfilePage } from './features/users/pages/user-profile/user-profile-page';

import { PaymentTransactionsPage } from './features/admin/pages/payment-transactions/payment-transactions-page';

export const routes: Routes = [
  { path: '', component: LandingPage },
  { path: 'my-bookings', component: GuestDashboard },
  { path: 'payment/:id', component: PaymentPage },
  { path: 'admin-dashboard', component: AdminDashboard, canActivate: [adminGuard] },
  { path: 'admin/reservations', component: ReservationLookup, canActivate: [adminGuard] },
  { path: 'admin/rooms', component: RoomManagement, canActivate: [adminGuard] },
  { path: 'admin/system-settings', component: SystemSettingsPage, canActivate: [adminGuard] },
  { path: 'admin/reservations', component: ReservationLookup, canActivate: [adminGuard] },
  {
    path: 'payment-transactions',
    component: PaymentTransactionsPage,
    canActivate: [adminGuard],
  },
  { path: 'profile-settings', component: UserProfilePage },
  { path: 'forbidden', component: ForbiddenPage },
  { path: 'error', component: NotFoundPage },
  { path: '**', component: NotFoundPage },
];
