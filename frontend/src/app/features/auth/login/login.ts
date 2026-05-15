import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../shared/auth.service';

/**
 * Component handling user authentication.
 */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  loginForm = this.fb.group({
    username: ['', Validators.required],
    password: ['', Validators.required]
  });

  errorMessage: string | null = null;
  isLoading = false;

  /**
   * Handles the form submission event.
   */
  onSubmit(): void {
    if (this.loginForm.valid) {
      this.isLoading = true;
      this.errorMessage = null;

      const credentials = {
        username: this.loginForm.value.username!,
        password: this.loginForm.value.password!
      };

      this.authService.login(credentials).subscribe({
        next: () => {
          this.isLoading = false;
          // The authService automatically stores the token.
          // Route the user to the authenticated area.
          const token = this.authService.getDecodedToken();

          if (token && token.role === 'ROLE_CLIENT') {
             this.router.navigate(['/app/send-package']);
          } else {
             this.router.navigate(['/app/shipments']); // Default for clerks/admins/couriers
          }
        },
        error: (err) => {
          this.isLoading = false;
          if (err.status === 401) {
            this.errorMessage = 'Invalid username or password.';
          } else {
            this.errorMessage = 'An error occurred during login.';
          }
        }
      });
    }
  }
}
