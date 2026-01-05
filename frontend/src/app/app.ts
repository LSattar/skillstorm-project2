import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthService } from './features/auth/services/auth.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements OnInit {
  protected readonly title = signal('reserveone-client');
  private readonly auth = inject(AuthService);

  ngOnInit(): void {
    this.auth.bootstrapCsrf().subscribe({
      next: () => {},
      error: () => {}, // don't block the app if backend isn't running yet
    });
  }
}
