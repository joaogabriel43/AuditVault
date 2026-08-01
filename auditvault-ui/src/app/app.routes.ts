import { Routes } from '@angular/router';
import { LoginComponent } from './components/login/login.component';
import { AuditDashboardComponent } from './components/audit-dashboard.component';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'dashboard', component: AuditDashboardComponent, canActivate: [authGuard] },
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' }
];
