import { Routes } from '@angular/router';
import { LandingPage } from './features/landing/pages/landing-page/landing-page';
import { NotFoundPage } from './features/landing/pages/not-found/not-found';
import { ForbiddenPage } from './features/landing/pages/forbidden/forbidden';
import { AdminDashboard } from './features/admin/pages/admin-dashboard/admin-dashboard';
import { ReservationLookup } from './features/admin/pages/reservation-lookup/reservation-lookup';
import { RoomManagement } from './features/admin/pages/room-management/room-management';
import { adminGuard } from './features/auth/guards/admin.guard';

export const routes: Routes = [
  { path: '', component: LandingPage },
  { path: 'admin-dashboard', component: AdminDashboard, canActivate: [adminGuard] },
  { path: 'admin/reservations', component: ReservationLookup, canActivate: [adminGuard] },
  { path: 'admin/rooms', component: RoomManagement, canActivate: [adminGuard] },
  { path: 'forbidden', component: ForbiddenPage },
  { path: 'error', component: NotFoundPage },
  { path: '**', component: NotFoundPage },
];
