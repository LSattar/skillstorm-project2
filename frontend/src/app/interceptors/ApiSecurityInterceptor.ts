import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

function getCookie(name: string): string | null {
  const cookies = document.cookie ? document.cookie.split('; ') : [];
  for (const c of cookies) {
    const [k, ...rest] = c.split('=');
    if (k === name) return decodeURIComponent(rest.join('='));
  }
  return null;
}

function isApiRequest(url: string): boolean {
  try {
    const u = new URL(url, window.location.origin);
    return u.pathname.startsWith('/api/');
  } catch {
    return url.startsWith('/api/');
  }
}

@Injectable()
export class ApiSecurityInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if (!isApiRequest(req.url)) return next.handle(req);

    const xsrf = getCookie('XSRF-TOKEN');

    const updated = req.clone({
      withCredentials: true,
      ...(xsrf ? { setHeaders: { 'X-XSRF-TOKEN': xsrf } } : {}),
    });

    return next.handle(updated);
  }
}
