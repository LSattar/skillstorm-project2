import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

function getCookie(name: string): string | null {
  const match = document.cookie.match(new RegExp('(^|; )' + name + '=([^;]*)'));
  return match ? match[2] : null; // do not decode
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

    return next.handle(
      req.clone({
        withCredentials: true,
        ...(xsrf ? { setHeaders: { 'X-XSRF-TOKEN': xsrf } } : {}),
      })
    );
  }
}
