import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-nav',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './nav.html',
  styleUrl: './nav.css',
})
export class Nav {
  @Input() isOpen = false;
  @Input() isAuthenticated = false;
  @Input() isAdmin = false;

  @Output() toggle = new EventEmitter<void>();
  @Output() close = new EventEmitter<void>();
}
