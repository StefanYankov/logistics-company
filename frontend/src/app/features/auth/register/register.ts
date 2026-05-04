import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { catchError, tap } from 'rxjs/operators';
import { of } from 'rxjs';
import { ClientRegistrationDto } from '../../../api';
import { ClientAPIService } from '../../../api';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './register.html',
  styleUrl: './register.css'
})
export class Register {
  private clientApi = inject(ClientAPIService);

  registerForm = inject(FormBuilder).group({
    username: ['', [Validators.required, Validators.minLength(3)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    phoneNumber: ['', Validators.required]
  });

  errorMessage: string | null = null;
  isLoading = false;
  isSuccess = false;

  onSubmit(): void {
    if (this.registerForm.valid) {
      this.isLoading = true;
      this.errorMessage = null;

      const request: ClientRegistrationDto = this.registerForm.value as ClientRegistrationDto;

      this.clientApi.registerClient(request).pipe(
        tap(() => {
          // On success, set a flag to show a success message.
          this.isLoading = false;
          this.isSuccess = true;
        }),
        catchError(err => {
          // On error, handle the ProblemDetail response from the backend.
          this.isLoading = false;
          if (err.status === 409) { // 409 Conflict
            this.errorMessage = err.error.detail || 'Username or email is already taken.';
          } else {
            this.errorMessage = 'An unexpected error occurred. Please try again.';
          }
          // Return an empty observable to complete the stream gracefully.
          return of(null);
        })
      ).subscribe();
    } else {
      this.registerForm.markAllAsTouched();
    }
  }
}
