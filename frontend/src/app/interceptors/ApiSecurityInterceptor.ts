import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

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

    return next.handle(
      req.clone({
        withCredentials: true,
      })
    );
  }
}
