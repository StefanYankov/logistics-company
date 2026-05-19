import {Component, computed, inject} from '@angular/core';
import {RouterModule} from '@angular/router';
import {AuthService} from '../../auth.service';

@Component({
  selector: 'app-authenticated-header',
  standalone: true,
  imports: [RouterModule],
  templateUrl: './authenticated-header.html',
  styleUrl: './authenticated-header.css'
})
export class AuthenticatedHeader {
  public authService = inject(AuthService);

  isClient = computed(() => this.authService.hasRole('ROLE_CLIENT'));
  isStaff = computed(() => this.authService.hasRole('ROLE_CLERK') || this.authService.hasRole('ROLE_COURIER') || this.authService.hasRole('ROLE_ADMIN'));
  isCourier = computed(() => this.authService.hasRole('ROLE_COURIER'));
  isAdmin = computed(() => this.authService.hasRole('ROLE_ADMIN'));

  logout(): void {
    this.authService.logout();
  }
}
