import { Component, ElementRef, inject, ViewChild } from '@angular/core';
import { Router, RouterModule } from '@angular/router';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterModule],
  templateUrl: './home.html',
  styleUrl: './home.css'
})
export class Home {
  private router = inject(Router);

  // Use ViewChild to easily grab the value from the template input
  @ViewChild('trackingInput') trackingInput!: ElementRef<HTMLInputElement>;

  /**
   * Reads the tracking number from the input and navigates to the tracking route.
   */
  onTrackSubmit(): void {
    const trackingNumber = this.trackingInput.nativeElement.value.trim();
    if (trackingNumber) {
      // Programmatic navigation to the new route
      this.router.navigate(['/track', trackingNumber]);
    }
  }
}
