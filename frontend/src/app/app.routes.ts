import { Routes } from '@angular/router';
import { NotFoundPage } from './features/landing/pages/not-found/not-found';
import { LandingPage } from './features/landing/pages/landing-page/landing-page';

export const routes: Routes = [
  {
    path: '',
    component: LandingPage
  },
  {
    path: 'error',
    component: NotFoundPage
  },
  {
    path: '**',
    component: NotFoundPage
  }
];
