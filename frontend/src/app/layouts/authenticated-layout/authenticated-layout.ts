import { Component, inject, computed } from '@angular/core';
import { RouterModule, RouterOutlet } from '@angular/router';
import { AuthService } from '../../shared/auth.service';

/**
 * Shell component for all authenticated routes (Dashboard, Shipments, etc.).
 * Provides the main application sidebar and top navigation.
 */
@Component({
  selector: 'app-authenticated-layout',
  standalone: true,
  imports: [RouterOutlet, RouterModule],
  templateUrl: './authenticated-layout.html',
  styleUrl: './authenticated-layout.css'
})
export class AuthenticatedLayout {
  private authService = inject(AuthService);

  // Expose role checks to the template as reactive computed signals
  isClient = computed(() => this.authService.hasRole('ROLE_CLIENT'));
  isStaff = computed(() => this.authService.hasRole('ROLE_CLERK') || this.authService.hasRole('ROLE_COURIER') || this.authService.hasRole('ROLE_ADMIN'));

  logout(): void {
    this.authService.logout();
  }
}
