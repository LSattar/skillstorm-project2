import { CommonModule } from '@angular/common';
import { Component, HostListener, computed, inject } from '@angular/core';
import { Router } from '@angular/router';

import { Header } from '../../../shared/header/header'; // <-- adjust path if needed
import { AuthService } from '../../auth/services/auth.service'; // <-- adjust path if needed
import { CarouselComponent } from '../components/carousel';

@Component({
  selector: 'app-rooms-page',
  standalone: true,
  imports: [CommonModule, CarouselComponent, Header],
  templateUrl: './rooms-page.html',
  styleUrls: ['./rooms-page.css'],
})
export class RoomsPage {
  constructor() {
    // Preload all room images for instant display
    const allRoomImages = [...this.kingRoomImages, ...this.queenRoomImages, ...this.sofaRoomImages];
    allRoomImages.forEach((src) => {
      const img = new window.Image();
      img.src = src;
    });
  }
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  // ----- Header state (minimal) -----
  isNavOpen = false;

  isAuthenticated = computed(() => !!this.auth.meSignal());
  roleLabel = computed(() => (this.auth.isAdmin() ? 'Admin' : 'Guest'));
  userLabel(): string {
    const me = this.auth.meSignal();
    // Use firstName + lastName if available, else fallback to email
    if (me) {
      if (me.firstName || me.lastName) {
        return `${me.firstName ?? ''} ${me.lastName ?? ''}`.trim();
      }
      return me.email ?? '';
    }
    return '';
  }
  userEmail = computed(() => this.auth.meSignal()?.email ?? ''); // adjust field if needed
  showSystemSettings = computed(() => this.auth.isAdmin());

  toggleNav() {
    this.isNavOpen = !this.isNavOpen;
  }

  closeNav() {
    this.isNavOpen = false;
  }

  openBooking() {
    // Rooms page doesn't have the booking modal, so just route home and let landing open it (optional)
    this.router.navigate(['/']);
  }

  openSignIn() {
    // If you have a sign-in modal only on landing, route home (optional)
    this.router.navigate(['/']);
  }

  signOut() {
    this.auth.logout?.(); // if your AuthService has logout(); otherwise call your existing method
    this.router.navigate(['/']);
  }

  openProfile() {
    this.router.navigate(['/profile-settings']);
  }

  openSystemSettings() {
    this.router.navigate(['/admin/system-settings']);
  }

  // IMPORTANT: this is what your header emits
  openSystemSettingsFromHeader() {
    this.openSystemSettings();
  }

  // ----- Rooms page modal logic (keep your existing code) -----
  showDetailsModal = false;
  detailsModalRoom = '';

  kingRoomImages = ['/images/rooms/king-bed-room.jpg', '/images/rooms/bathroom.jpg'];
  queenRoomImages = ['/images/rooms/double-queen-room.jpg', '/images/rooms/bathroom.jpg'];
  sofaRoomImages = [
    '/images/rooms/sofa-room.jpg',
    '/images/rooms/sofa-room2.jpg',
    '/images/rooms/bathroom.jpg',
  ];

  showBathroomModal = false;
  bathroomModalRoom = '';

  openDetailsModal(room: string) {
    this.detailsModalRoom = room;
    this.showDetailsModal = true;
    this.showBathroomModal = false;
    this.lockScroll();
  }

  closeDetailsModal() {
    this.showDetailsModal = false;
    this.detailsModalRoom = '';
    this.unlockScrollIfNoModal();
  }

  openBathroomModal(room: string) {
    this.bathroomModalRoom = room;
    this.showBathroomModal = true;
    this.showDetailsModal = false;
    this.lockScroll();
  }

  closeBathroomModal() {
    this.showBathroomModal = false;
    this.bathroomModalRoom = '';
    this.unlockScrollIfNoModal();
  }

  onOverlayClick(type: 'details' | 'bathroom') {
    if (type === 'details') this.closeDetailsModal();
    else this.closeBathroomModal();
  }

  @HostListener('document:keydown.escape', [])
  onEscape() {
    if (this.showDetailsModal) this.closeDetailsModal();
    if (this.showBathroomModal) this.closeBathroomModal();
  }

  lockScroll() {
    document.body.style.overflow = 'hidden';
  }

  unlockScrollIfNoModal() {
    if (!this.showDetailsModal && !this.showBathroomModal) {
      document.body.style.overflow = '';
    }
  }

  get normalizedDetailsModalRoom(): string {
    return (this.detailsModalRoom || '').trim().toLowerCase();
  }
}
