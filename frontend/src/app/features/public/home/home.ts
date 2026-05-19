import {Component, ElementRef, inject, OnInit, ViewChild} from '@angular/core';
import {Router, RouterModule} from '@angular/router';
import {AuthService} from '../../../shared/auth.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterModule],
  templateUrl: './home.html',
  styleUrl: './home.css'
})
export class Home implements OnInit {
  private router = inject(Router);
  private authService = inject(AuthService);

  @ViewChild('trackingInput') trackingInput!: ElementRef<HTMLInputElement>;

  ngOnInit(): void {
    if (this.authService.isLoggedIn()) {
      const decodedToken = this.authService.getDecodedToken();
      if (decodedToken) {
        if (decodedToken.role === 'ROLE_CLIENT') {
           this.router.navigate(['/app']);
        } else if (decodedToken.role === 'ROLE_ADMIN') {
            // TODO: Redirect to admin dashboard
            this.router.navigate(['/app/shipments']);
        } else {
           this.router.navigate(['/app/shipments']);
        }
      }
    }
  }

  /**
   * Reads the tracking number from the input and navigates to the tracking route.
   */
  onTrackSubmit(): void {
    const trackingNumber = this.trackingInput.nativeElement.value.trim();
    if (trackingNumber) {
      this.router.navigate(['/track', trackingNumber]);
    }
  }
}
