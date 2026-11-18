import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'vehicles' },
  {
    path: 'vehicles/:vehicleId/documents',
    loadChildren: () =>
      import('./vehicle-documents/vehicle-documents.routes').then((m) => m.vehicleDocumentsRoutes),
  },
  {
    path: 'vehicles',
    loadChildren: () => import('./vehicles/vehicles.routes').then((m) => m.vehiclesRoutes),
  },
  {
    path: 'customers',
    loadChildren: () => import('./customers/customers.routes').then((m) => m.customersRoutes),
  },
];
