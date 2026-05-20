import {Routes} from '@angular/router';
import {PublicLayout} from './layouts/public-layout/public-layout';
import {Home} from './features/public/home/home';
import {Login} from './features/auth/login/login';
import {Register} from './features/auth/register/register';
import {Tracking} from './features/public/tracking/tracking';
import {AuthenticatedLayout} from './layouts/authenticated-layout/authenticated-layout';
import {ClientDashboard} from './features/client/dashboard/client-dashboard';
import {ClientRegistration} from './features/shipments/client-registration/client-registration';
import {ClerkRegistration} from './features/shipments/clerk-registration/clerk-registration';
import {ShipmentList} from './features/shipments/shipment-list/shipment-list';
import {authGuard} from './shared/auth.guard';
import {ShipmentDetails} from './features/shipments/shipment-details/shipment-details';
import {CourierDashboard} from './features/courier/dashboard/courier-dashboard';
import {ShipmentEdit} from './features/shipments/shipment-edit/shipment-edit';

export const routes: Routes = [
  {
    // Zone A: The Public Portal (Unauthenticated)
    path: '',
    component: PublicLayout,
    children: [
      { path: '', component: Home },
      { path: 'login', component: Login },
      { path: 'register', component: Register },
    ]
  },
  {
    // Zone B: The Protected Shell (Authenticated)
    path: 'app',
    component: AuthenticatedLayout,
    canActivate: [authGuard],
    children: [
      { path: '', component: ClientDashboard },
      { path: 'send-package', component: ClientRegistration },
      { path: 'register-shipment', component: ClerkRegistration },
      { path: 'shipments', component: ShipmentList },
      { path: 'shipments/:id', component: ShipmentDetails },
      { path: 'shipments/:id/edit', component: ShipmentEdit },
      { path: 'my-tasks', component: CourierDashboard },
      // TODO: Add a route for the admin dashboard, e.g., { path: 'admin', component: AdminDashboardComponent }
    ]
  },
  { path: 'track/:trackingNumber', component: Tracking }
  ,
  { path: '**', redirectTo: '' }
];
