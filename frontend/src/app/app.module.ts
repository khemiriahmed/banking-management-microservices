import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { AuthInterceptor } from './core/interceptors/auth.interceptor';
import { LoadingComponent } from './shared/components/loading.component';
import { AlertComponent } from './shared/components/alert.component';
import { LoginComponent } from './features/auth/login.component';
import { RegisterComponent } from './features/auth/register.component';
import { ForbiddenComponent } from './features/auth/forbidden.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { AccountsComponent } from './features/accounts/accounts.component';
import { BeneficiariesComponent } from './features/beneficiaries/beneficiaries.component';
import { TransfersComponent } from './features/transfers/transfers.component';
import { TransactionsComponent } from './features/transactions/transactions.component';
import { NotificationsComponent } from './features/notifications/notifications.component';
import { AdminComponent } from './features/admin/admin.component';

@NgModule({
  declarations: [AppComponent, LoadingComponent, AlertComponent, LoginComponent, RegisterComponent, ForbiddenComponent, DashboardComponent, AccountsComponent, BeneficiariesComponent, TransfersComponent, TransactionsComponent, NotificationsComponent, AdminComponent],
  imports: [BrowserModule, FormsModule, HttpClientModule, AppRoutingModule],
  providers: [{ provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true }],
  bootstrap: [AppComponent]
})
export class AppModule {}
