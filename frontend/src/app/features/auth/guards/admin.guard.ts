import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const adminGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const me = authService.meSignal();
  if (!me) {
    // User not authenticated, redirect to home
    router.navigate(['/']);
    return false;
  }

  // Check if user is an admin
  const normalizeRole = (r: string): string => r.trim().toUpperCase();
  
  const dbRoleNames = (me.dbRoles ?? [])
    .filter((r): r is string => typeof r === 'string')
    .map(normalizeRole);

  const authorities = [...(me.roles ?? []), ...(me.principalAuthorities ?? [])];
  
  const roleNames = authorities
    .filter((a): a is string => typeof a === 'string')
    .filter((a) => a.startsWith('ROLE_'))
    .map((a) => a.substring('ROLE_'.length));

  const allRoleNames = Array.from(new Set([...dbRoleNames, ...roleNames.map(normalizeRole)]));

  const isAdmin = allRoleNames.includes('ADMIN');

  if (!isAdmin) {
    // User is not an admin, redirect to 403 page
    router.navigate(['/forbidden']);
    return false;
  }

  return true;
};
