import { Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { AuthService } from './features/auth/services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements OnInit {
  protected readonly title = signal('reserveone-client');
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  ngOnInit(): void {
    this.auth.bootstrapCsrf().subscribe({
      next: () => {},
      error: () => {}, // don't block the app if backend isn't running yet
    });

    // Populate session-backed auth state (JSESSIONID cookie) after page load/redirect.
    this.auth.refreshMe().subscribe({
      next: () => {
        const me = this.auth.meSignal();
        console.log('refreshMe() result:', me);
        console.log('isAdmin:', this.auth.isAdmin());
        console.log('Current route:', this.router.url);
        // Check if user is admin and not already on admin-dashboard
        if (me && this.auth.isAdmin() && this.router.url !== '/admin-dashboard') {
          console.log('Redirecting to /admin-dashboard');
          this.router.navigate(['/admin-dashboard']);
        }
      },
      error: () => {},
    });
  }
}
