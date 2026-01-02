import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Nav } from '../nav/nav';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [Nav],
  templateUrl: './header.html',
  styleUrl: './header.css',
})
export class Header {
  @Input() isNavOpen = false;

  @Output() toggleNav = new EventEmitter<void>();
  @Output() closeNav = new EventEmitter<void>();
  @Output() openBooking = new EventEmitter<void>();
}
