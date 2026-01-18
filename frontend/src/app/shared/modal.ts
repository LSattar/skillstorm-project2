import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-modal',
  standalone: true,
  template: `
    <div class="modal-backdrop" (click)="onBackdropClick($event)"></div>
    <div class="modal-content" role="dialog" aria-modal="true">
      <button class="modal-close" (click)="close.emit()" aria-label="Close">&times;</button>
      <ng-content></ng-content>
    </div>
  `,
  styleUrls: ['./modal.css'],
})
export class ModalComponent {
  @Input() open = false;
  @Output() close = new EventEmitter<void>();

  onBackdropClick(event: MouseEvent) {
    if ((event.target as HTMLElement).classList.contains('modal-backdrop')) {
      this.close.emit();
    }
  }
}
