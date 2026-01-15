import { CommonModule } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Header } from '../../../../shared/header/header';
import { AuthService } from '../../../auth/services/auth.service';

import { RoomSearchModal } from '../../components/room-search-modal/room-search-modal';
import {
  RoomSearchResults,
  SearchResultsData,
} from '../../components/room-search-results/room-search-results';
import { RoomResponse, RoomSearchParams } from '../../services/room-search.service';

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [CommonModule, FormsModule, Header, Footer, UserProfileModal, SystemSettingsModal, RoomSearchModal, RoomSearchResults],
  templateUrl: './landing-page.html',
  styleUrls: ['./landing-page.css'],
})
export class LandingPage {
  // Amenities carousel logic
  amenitiesImages = [
    '/images/ammenities/exercise.jpg',
    '/images/ammenities/food.jpg',
    '/images/ammenities/food2.jpg',
    '/images/ammenities/pool.jpg',
    '/images/ammenities/spa.jpg',
  ];

  amenitiesCaptions = [
    'Forest Fitness & Exercise',
    'Organic Forest Cuisine',
    'Gourmet Wellness Dining',
    'Heated Pool Sanctuary',
    'Forest Spa Retreat',
  ];

  activeAmenity = 0;

  prevAmenity() {
    this.activeAmenity =
      (this.activeAmenity - 1 + this.amenitiesImages.length) % this.amenitiesImages.length;
  }

  nextAmenity() {
    this.activeAmenity = (this.activeAmenity + 1) % this.amenitiesImages.length;
  }
  protected readonly auth = inject(AuthService);

  protected readonly isAuthenticated = this.auth.isAuthenticated;
  protected readonly roleLabel = this.auth.primaryRoleLabel;
  protected readonly userLabel = computed(() => {
    const me = this.auth.meSignal();
    if (!me) return '';
    const first = me.firstName?.trim();
    if (first) return first;
    return me.email || '';
  });

  protected readonly userEmail = computed(() => this.auth.meSignal()?.email ?? '');
  protected readonly showSystemSettings = this.auth.isAdmin;

  isNavOpen = false;
  isBookingOpen = false;
  isResultsOpen = false;
  isSignInOpen = false;
  isProfileOpen = false;
  isSystemSettingsOpen = false;
  profileSaveError = '';
  searchResultsData?: SearchResultsData;

  toggleNav() {
    this.isNavOpen = !this.isNavOpen;
  }

  closeNav() {
    this.isNavOpen = false;
  }

  today = new Date();

  // Booking modal
  openBooking() {
    this.isBookingOpen = true;
  }

  closeBooking() {
    this.isBookingOpen = false;
  }

  onSearchResults(data: { rooms: RoomResponse[]; searchParams: RoomSearchParams }) {
    // Find the hotel name for display
    // We'll need to fetch it or pass it from the search modal
    // For now, we'll just use the search params
    this.searchResultsData = {
      rooms: data.rooms,
      searchParams: data.searchParams,
    };
    this.isResultsOpen = true;
  }

  closeResults() {
    this.isResultsOpen = false;
    this.searchResultsData = undefined;
  }

  onModifySearch() {
    this.isResultsOpen = false;
    this.isBookingOpen = true;
  }

  onBookingComplete() {
    // Show success message or navigate to confirmation page
    alert('Booking confirmed! Thank you for your reservation.');
  }

  onSignInRequired() {
    // Close any open modals and show sign-in modal
    this.isBookingOpen = false;
    this.isResultsOpen = false;
    this.isSignInOpen = true;
  }

  openSignIn() {
    this.isSignInOpen = true;
  }

  closeSignIn() {
    this.isSignInOpen = false;
  }

  continueWithGoogle() {
    this.isSignInOpen = false;
    this.auth.startGoogleLogin();
  }

  openProfile() {
    this.isProfileOpen = true;
  }

  closeProfile() {
    this.isProfileOpen = false;
    this.profileSaveError = '';
  }

  openSystemSettings() {
    this.isSystemSettingsOpen = true;
  }

  closeSystemSettings() {
    this.isSystemSettingsOpen = false;
  }

  signOut() {
    this.auth.logout().subscribe({
      next: () => {},
      error: () => {},
    });
  }
}
