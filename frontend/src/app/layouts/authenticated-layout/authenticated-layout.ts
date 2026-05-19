import {Component, computed, inject} from '@angular/core';
import {RouterModule, RouterOutlet} from '@angular/router';
import {AuthService} from '../../shared/auth.service';
import {AuthenticatedHeader} from '../../shared/ui/authenticated-header/authenticated-header';
import {Footer} from '../../shared/ui/footer/footer';

/**
 * Shell component for all authenticated routes (Dashboard, Shipments, etc.).
 * Provides the main application sidebar and top navigation.
 */
@Component({
  selector: 'app-authenticated-layout',
  standalone: true,
  imports: [RouterOutlet, RouterModule, AuthenticatedHeader, Footer],
  templateUrl: './authenticated-layout.html',
  styleUrl: './authenticated-layout.css'
})
export class AuthenticatedLayout {
  public authService = inject(AuthService);

  // Expose role checks to the template as reactive computed signals
  isClient = computed(() => this.authService.hasRole('ROLE_CLIENT'));
  isStaff = computed(() => this.authService.hasRole('ROLE_CLERK') || this.authService.hasRole('ROLE_COURIER') || this.authService.hasRole('ROLE_ADMIN'));
  isCourier = computed(() => this.authService.hasRole('ROLE_COURIER'));
  isAdmin = computed(() => this.authService.hasRole('ROLE_ADMIN'));

  logout(): void {
    this.authService.logout();
  }
}
