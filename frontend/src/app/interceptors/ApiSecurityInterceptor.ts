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
  // Handles:
  //  - "/api/..."
  //  - "https://domain/api/..."
  //  - "http://localhost:4200/api/..."
  try {
    const u = url.startsWith('http') ? new URL(url) : new URL(url, window.location.origin);
    return u.pathname.startsWith('/api/');
  } catch {
    return url.startsWith('/api/');
  }
}

@Injectable()
export class ApiSecurityInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if (!isApiRequest(req.url)) {
      return next.handle(req);
    }

    const xsrf = getCookie('XSRF-TOKEN');

    // IMPORTANT: don't accidentally wipe other headers.
    // setHeaders merges; it does not replace the whole header set.
    const updated = req.clone({
      withCredentials: true,
      ...(xsrf ? { setHeaders: { 'X-XSRF-TOKEN': xsrf } } : {}),
    });

    return next.handle(updated);
  }
}
