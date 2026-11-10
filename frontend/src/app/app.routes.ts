import { Routes } from '@angular/router';
import { inject } from '@angular/core';
import { MsalGuard } from '@azure/msal-angular';
import { DashboardComponent } from './dashboard/dashboard';
import { LoginComponent } from './login/login';

// Si no hay sesión de Microsoft, el MsalGuard redirige al login de Azure.
const azureGuard = (route: any, state: any) => inject(MsalGuard).canActivate(route, state);

export const routes: Routes = [
  { path: '', component: DashboardComponent, canActivate: [azureGuard] },
  { path: 'login', component: LoginComponent },
  { path: '**', redirectTo: '' }
];