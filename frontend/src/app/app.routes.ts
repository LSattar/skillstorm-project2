import { Routes } from '@angular/router';
import { LandingPage } from './features/landing/pages/landing-page/landing-page';
import { NotFoundPage } from './features/landing/pages/not-found/not-found';
import { AdminDashboard } from './features/admin/pages/admin-dashboard/admin-dashboard';

export const routes: Routes = [
  { path: '', component: LandingPage },
  { path: 'admin-dashboard', component: AdminDashboard },
  { path: 'error', component: NotFoundPage },
  { path: '**', component: NotFoundPage },
];
