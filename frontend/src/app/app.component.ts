import { Component } from '@angular/core';
import { Account } from './account.model';
import { AccountService } from './account.service';
import { Beneficiary } from './beneficiary.model';
import { BeneficiaryService } from './beneficiary.service';
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
  beneficiaries: Beneficiary[] = [];
  beneficiaryName = '';
  beneficiaryReference = '';
  errorMessage = '';
  isLoading = false;

  constructor(
    private readonly auth: AuthService,
    private readonly accountService: AccountService,
    private readonly beneficiaryService: BeneficiaryService
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
        this.loadBeneficiaries();
      },
      error: () => {
        this.isLoading = false;
        this.accounts = [];
        this.beneficiaries = [];
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

  loadBeneficiaries(): void {
    if (!this.authenticated) {
      return;
    }
    this.errorMessage = '';
    this.beneficiaryService.getBeneficiaries().subscribe({
      next: (beneficiaries) => {
        this.beneficiaries = beneficiaries;
      },
      error: () => {
        this.beneficiaries = [];
        this.errorMessage = 'Impossible de charger les beneficiaires.';
      }
    });
  }

  createBeneficiary(): void {
    if (!this.authenticated || !this.beneficiaryName.trim() || !this.beneficiaryReference.trim()) {
      return;
    }
    this.errorMessage = '';
    this.beneficiaryService.createBeneficiary({
      displayName: this.beneficiaryName.trim(),
      destinationAccountReference: this.beneficiaryReference.trim()
    }).subscribe({
      next: () => {
        this.beneficiaryName = '';
        this.beneficiaryReference = '';
        this.loadBeneficiaries();
      },
      error: () => {
        this.errorMessage = "Impossible d'ajouter le beneficiaire.";
      }
    });
  }

  updateBeneficiaryStatus(beneficiary: Beneficiary): void {
    this.errorMessage = '';
    this.beneficiaryService.updateBeneficiary(beneficiary.id, {
      status: beneficiary.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    }).subscribe({
      next: () => this.loadBeneficiaries(),
      error: () => {
        this.errorMessage = 'Impossible de modifier le beneficiaire.';
      }
    });
  }

  deleteBeneficiary(beneficiary: Beneficiary): void {
    this.errorMessage = '';
    this.beneficiaryService.deleteBeneficiary(beneficiary.id).subscribe({
      next: () => this.loadBeneficiaries(),
      error: () => {
        this.errorMessage = 'Impossible de supprimer le beneficiaire.';
      }
    });
  }

  logout(): void {
    this.auth.logout();
    this.email = '';
    this.password = '';
    this.accounts = [];
    this.beneficiaries = [];
    this.beneficiaryName = '';
    this.beneficiaryReference = '';
    this.errorMessage = '';
    this.isLoading = false;
  }
}
