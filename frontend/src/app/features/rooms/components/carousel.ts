import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-carousel',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="carousel-container">
      <button class="carousel-btn prev" (click)="prev()">&#8592;</button>
      <div class="carousel-images">
        <img
          *ngFor="let img of images; let i = index"
          [src]="img"
          [class.active]="i === activeIndex"
          class="carousel-image"
        />
      </div>
      <button class="carousel-btn next" (click)="next()">&#8594;</button>
    </div>
  `,
  styleUrls: ['./carousel.css'],
})
export class CarouselComponent {
  @Input() images: string[] = [];
  activeIndex = 0;

  next() {
    this.activeIndex = (this.activeIndex + 1) % this.images.length;
  }

  prev() {
    this.activeIndex = (this.activeIndex - 1 + this.images.length) % this.images.length;
  }
}
