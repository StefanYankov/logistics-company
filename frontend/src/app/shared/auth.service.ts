import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { Router } from '@angular/router';
import { LoginRequestDto } from '../api/model/loginRequestDto';
import { LoginResponseDto } from '../api/model/loginResponseDto';

export interface DecodedToken {
  sub: string; // The username
  userId: string; // The unique UUID of the user
  role: string; // e.g., 'ROLE_CLIENT', 'ROLE_ADMIN'
  iat: number; // Issued at timestamp
  exp: number; // Expiration timestamp
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly TOKEN_KEY = 'logistics_jwt';

  // Modern dependency injection
  private http = inject(HttpClient);
  private router = inject(Router);

  /**
   * Sends login credentials to the backend and stores the JWT upon success.
   * @param credentials The user's username and password.
   * @returns An Observable of the login response.
   */
  login(credentials: LoginRequestDto): Observable<LoginResponseDto> {
    return this.http.post<LoginResponseDto>('/api/auth/login', credentials).pipe(
      tap(response => {
        if (response.token) {
          this.storeToken(response.token);
        }
      })
    );
  }

  /**
   * Removes the JWT from storage and navigates the user to the login page.
   */
  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    this.router.navigate(['/login']);
  }

  /**
   * Retrieves the raw JWT from local storage.
   * @returns The token string or null if not present.
   */
  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  /**
   * Decodes the payload section of the stored JWT.
   * A JWT has 3 parts separated by dots: Header.Payload.Signature.
   * This method extracts the Payload (index 1), decodes the Base64Url string, and parses the JSON.
   *
   * @returns The DecodedToken object containing claims (like userId and role), or null if invalid.
   */
  getDecodedToken(): DecodedToken | null {
    const token = this.getToken();
    if (!token) {
      return null;
    }

    try {
      const payloadBase64 = token.split('.')[1];
      // Convert Base64Url to standard Base64
      const base64 = payloadBase64.replace(/-/g, '+').replace(/_/g, '/');
      // Decode the string using browser's built-in atob function
      const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
          return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
      }).join(''));

      return JSON.parse(jsonPayload);
    } catch (e) {
      console.error('Error decoding JWT token', e);
      return null;
    }
  }

  /**
   * Checks if a user is currently authenticated by verifying the presence of a token.
   * @returns True if a token exists, false otherwise.
   */
  isLoggedIn(): boolean {
    return this.getToken() !== null;
  }

  /**
   * Stores the JWT in local storage.
   * @param token The token string to store.
   */
  private storeToken(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
  }
}
