# Logistics Company Frontend

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 19.x and serves as the presentation layer for the Logistics Company System.

## Architecture

This application is built using modern Angular practices:
*   **Standalone Components:** We do not use `NgModules`. All components, directives, and pipes are standalone.
*   **Signals:** State management and reactivity are heavily reliant on Angular Signals (`signal()`, `computed()`, `effect()`) for fine-grained updates and zoneless compatibility.
*   **Reactive Forms:** Complex forms (like shipment registration) use strongly typed `FormBuilder`.
*   **OpenAPI Generation:** The entire data access layer (`src/app/api`) is auto-generated from the Spring Boot backend's Swagger/OpenAPI specification.
*   **Role-Based Feature Modules:** Components are organized by user role (`features/client`, `features/courier`) to enhance modularity and maintainability.
*   **Semantic Layouts:** The application uses distinct layout components (`PublicLayout`, `AuthenticatedLayout`) and smart header components (`PublicHeader`, `AuthenticatedHeader`) to provide tailored UI experiences based on authentication state and user role.

## Setup & OpenAPI Generation

Before running the application, you **must** generate the API client. If the backend API contract changes, you must re-run this step.

1. Ensure your Spring Boot backend is running locally on `http://localhost:8080`.
2. Run the generator script:
   ```bash
   npm run generate-api
   ```
   *This command uses `@openapitools/openapi-generator-cli` to fetch the `/v3/api-docs` JSON from the backend and builds the strongly-typed TypeScript services and DTOs into the `src/app/api` folder.*

## Development server

To start a local development server, run:

```bash
npm start
```
*(This is an alias for `ng serve`)*

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files. The application is configured to proxy API requests to `http://localhost:8080/api`.

## Running unit tests

To execute unit tests with the [Vitest](https://vitest.dev/) test runner (which is significantly faster than Karma/Jasmine and natively supports ESM), use the following command:

```bash
npm test
```

*Note: We heavily utilize `async/await` and `fixture.whenStable()` in our component tests to accurately simulate Angular's lifecycle and avoid `ExpressionChangedAfterItHasBeenCheckedError`.*

## Building

To build the project run:

```bash
npm run build
```

This will compile your project and store the build artifacts in the `dist/` directory.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component features/my-new-feature
```
