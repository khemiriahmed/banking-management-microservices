import { Component } from '@angular/core';
import { Account } from './account.model';
import { AccountService } from './account.service';
import { AuthService } from './auth.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  email = '';
  password = '';
  accounts: Account[] = [];
  errorMessage = '';
  isLoading = false;

  constructor(
    private readonly auth: AuthService,
    private readonly accountService: AccountService
  ) {}

  get authenticated(): boolean {
    return this.auth.isAuthenticated();
  }

  login(): void {
    this.errorMessage = '';
    this.isLoading = true;
    this.auth.login(this.email, this.password).subscribe({
      next: () => {
        this.isLoading = false;
        this.password = '';
        this.loadAccounts();
      },
      error: () => {
        this.isLoading = false;
        this.accounts = [];
        this.errorMessage = 'Connexion impossible : verifiez vos identifiants.';
      }
    });
  }

  loadAccounts(): void {
    if (!this.authenticated) {
      return;
    }
    this.errorMessage = '';
    this.isLoading = true;
    this.accountService.getAccounts().subscribe({
      next: (accounts) => {
        this.accounts = accounts;
        this.isLoading = false;
      },
      error: () => {
        this.accounts = [];
        this.isLoading = false;
        this.errorMessage = 'Impossible de charger les comptes.';
      }
    });
  }

  logout(): void {
    this.auth.logout();
    this.email = '';
    this.password = '';
    this.accounts = [];
    this.errorMessage = '';
    this.isLoading = false;
  }
}
