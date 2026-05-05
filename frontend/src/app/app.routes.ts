import { Routes } from '@angular/router';
import { PublicLayout } from './layouts/public-layout/public-layout';
import { Home } from './features/public/home/home';
import { Login } from './features/auth/login/login';
import { Register } from './features/auth/register/register';
import { Tracking } from './features/public/tracking/tracking';
import { AuthenticatedLayout } from './layouts/authenticated-layout/authenticated-layout';
import { Dashboard } from './features/dashboard/dashboard';
import { RegisterShipment } from './features/shepments/register-shipment/register-shipment';
import { ShipmentList } from './features/shepments/shipment-list/shipment-list';
import { authGuard } from './shared/auth.guard';

export const routes: Routes = [
  {
    // Zone A: The Public Portal (Unauthenticated)
    path: '',
    component: PublicLayout,
    children: [
      { path: '', component: Home },
      { path: 'login', component: Login },
      { path: 'register', component: Register },
      // Route parameter :trackingNumber allows dynamic lookups
      { path: 'track/:trackingNumber', component: Tracking }
    ]
  },
  {
    // Zone B: The Protected Shell (Authenticated)
    path: 'app',
    component: AuthenticatedLayout,
    canActivate: [authGuard],
    children: [
      { path: '', component: Dashboard },
      { path: 'register-shipment', component: RegisterShipment },
      { path: 'shipments', component: ShipmentList }
    ]
  },
  { path: '**', redirectTo: '' }
];
