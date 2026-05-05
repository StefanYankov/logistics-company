import { ApplicationConfig, provideZonelessChangeDetection, Provider } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { routes } from './app.routes';
import { authInterceptor } from './shared/auth.interceptor';

// Import the auto-generated configuration factory and token from OpenAPI
import { Configuration, ConfigurationParameters } from './api/configuration';
import { BASE_PATH } from './api/variables';

/**
 * Factory function to provide global configuration to all generated OpenAPI services.
 * This ensures the API clients know the correct backend URL or use relative paths.
 */
export function apiConfigFactory(): Configuration {
  const params: ConfigurationParameters = {
    // Overriding the basePath.
    // By setting it to an empty string, the generated services will use relative paths (e.g., '/api/...').
    // This allows our 'proxy.conf.json' to correctly intercept the calls during local development,
    // and works perfectly in production if the frontend and backend are hosted on the same domain.
    basePath: ''
  };
  return new Configuration(params);
}

/**
 * Global application configuration.
 * Sets up core providers like routing and HTTP client.
 */
export const appConfig: ApplicationConfig = {
  providers: [
    provideZonelessChangeDetection(),
    provideRouter(routes),
    // Register the modern HttpClient and attach our custom JWT interceptor
    provideHttpClient(
      withInterceptors([authInterceptor])
    ),

    // --- OpenAPI Configuration Providers ---
    // 1. Provide the generated Configuration object to all API services
    { provide: Configuration, useFactory: apiConfigFactory },
    // 2. Explicitly override the BASE_PATH injection token used internally by the generated code
    { provide: BASE_PATH, useValue: '' }
  ]
};
