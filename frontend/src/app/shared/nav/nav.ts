import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-nav',
  standalone: true,
  templateUrl: './nav.html',
  styleUrl: './nav.css',
})
export class Nav {
  @Input() isOpen = false;

  @Output() toggle = new EventEmitter<void>();
  @Output() close = new EventEmitter<void>();
}
