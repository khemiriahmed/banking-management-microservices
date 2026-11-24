import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard } from './core/guards/auth.guard';
import { RoleGuard } from './core/guards/role.guard';
import { AccountsComponent } from './features/accounts/accounts.component';
import { AdminComponent } from './features/admin/admin.component';
import { ForbiddenComponent } from './features/auth/forbidden.component';
import { LoginComponent } from './features/auth/login.component';
import { RegisterComponent } from './features/auth/register.component';
import { BeneficiariesComponent } from './features/beneficiaries/beneficiaries.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { NotificationsComponent } from './features/notifications/notifications.component';
import { TransactionsComponent } from './features/transactions/transactions.component';
import { TransfersComponent } from './features/transfers/transfers.component';

const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'forbidden', component: ForbiddenComponent },
  { path: 'dashboard', component: DashboardComponent, canActivate: [AuthGuard, RoleGuard], data: { roles: ['CLIENT'] } },
  { path: 'accounts', component: AccountsComponent, canActivate: [AuthGuard] },
  { path: 'beneficiaries', component: BeneficiariesComponent, canActivate: [AuthGuard, RoleGuard], data: { roles: ['CLIENT'] } },
  { path: 'transfers', component: TransfersComponent, canActivate: [AuthGuard, RoleGuard], data: { roles: ['CLIENT'] } },
  { path: 'transactions', component: TransactionsComponent, canActivate: [AuthGuard, RoleGuard], data: { roles: ['CLIENT'] } },
  { path: 'notifications', component: NotificationsComponent, canActivate: [AuthGuard] },
  { path: 'admin', component: AdminComponent, canActivate: [AuthGuard, RoleGuard], data: { roles: ['ADMIN'] } },
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  { path: '**', redirectTo: 'login' }
];

@NgModule({ imports: [RouterModule.forRoot(routes)], exports: [RouterModule] })
export class AppRoutingModule {}
