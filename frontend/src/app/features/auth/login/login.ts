import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../shared/auth.service';
import { LoginRequestDto } from '../../../api';

/**
 * Component handling user authentication.
 */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  private authService = inject(AuthService);
  private router = inject(Router);

  loginForm = inject(FormBuilder).group({
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

      const credentials: LoginRequestDto = this.loginForm.value as LoginRequestDto;

      this.authService.login(credentials).subscribe({
        next: () => {
          // Success: Navigate to the main authenticated area
          this.router.navigate(['/app']);
        },
        error: (err) => {
          this.isLoading = false;
          // Map standard HTTP status codes from the backend to user-friendly messages
          if (err.status === 401) {
             this.errorMessage = 'Invalid username or password.';
          } else {
             this.errorMessage = 'An unexpected server error occurred. Please try again later.';
          }
        }
      });
    } else {
      // If the user clicks submit without filling out the form, trigger validation feedback
      this.loginForm.markAllAsTouched();
    }
  }
}
