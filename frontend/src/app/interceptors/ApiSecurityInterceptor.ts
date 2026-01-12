import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

function getCookie(name: string): string | null {
  const cookies = document.cookie ? document.cookie.split('; ') : [];
  for (const c of cookies) {
    const [k, ...rest] = c.split('=');
    if (k === name) {
      return decodeURIComponent(rest.join('='));
    }
  }
  return null;
}

@Injectable()
export class ApiSecurityInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Only apply to API calls
    if (!req.url.startsWith('/api/')) {
      return next.handle(req);
    }

    const xsrf = getCookie('XSRF-TOKEN');

    return next.handle(
      req.clone({
        withCredentials: true,
        setHeaders: xsrf ? { 'X-XSRF-TOKEN': xsrf } : {},
      })
    );
  }
}
