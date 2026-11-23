import { Component } from '@angular/core';
import { Account } from './account.model';
import { AccountService } from './account.service';
import { Beneficiary } from './beneficiary.model';
import { BeneficiaryService } from './beneficiary.service';
import { Transfer } from './transfer.model';
import { TransferService } from './transfer.service';
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
  transfers: Transfer[] = [];
  selectedSourceAccountId = '';
  selectedBeneficiaryId = '';
  transferAmount: number | null = null;
  transferCurrency = 'EUR';
  transferReason = '';
  errorMessage = '';
  isLoading = false;

  constructor(
    private readonly auth: AuthService,
    private readonly accountService: AccountService,
    private readonly beneficiaryService: BeneficiaryService,
    private readonly transferService: TransferService
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
        this.loadTransfers();
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
        if (!this.selectedSourceAccountId && accounts.length > 0) {
          this.selectedSourceAccountId = accounts[0].id;
          this.transferCurrency = accounts[0].currency;
        }
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
        if (!this.selectedBeneficiaryId && beneficiaries.length > 0) {
          this.selectedBeneficiaryId = beneficiaries[0].id;
        }
      },
      error: () => {
        this.beneficiaries = [];
        this.errorMessage = 'Impossible de charger les beneficiaires.';
      }
    });
  }

  loadTransfers(): void {
    if (!this.authenticated) {
      return;
    }
    this.errorMessage = '';
    this.transferService.getTransfers().subscribe({
      next: (page) => {
        this.transfers = page.content;
      },
      error: () => {
        this.transfers = [];
        this.errorMessage = 'Impossible de charger les virements.';
      }
    });
  }

  createTransfer(): void {
    if (
      !this.authenticated ||
      !this.selectedSourceAccountId ||
      !this.selectedBeneficiaryId ||
      !this.transferAmount ||
      this.transferAmount <= 0
    ) {
      this.errorMessage = 'Renseignez un compte, un beneficiaire et un montant valide.';
      return;
    }
    this.errorMessage = '';
    this.transferService.createTransfer({
      sourceAccountId: this.selectedSourceAccountId,
      beneficiaryId: this.selectedBeneficiaryId,
      amount: this.transferAmount,
      currency: this.transferCurrency,
      reason: this.transferReason.trim() || undefined
    }, this.newIdempotencyKey()).subscribe({
      next: () => {
        this.transferAmount = null;
        this.transferReason = '';
        this.loadTransfers();
      },
      error: () => {
        this.errorMessage = 'Impossible de creer le virement.';
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
    this.transfers = [];
    this.selectedSourceAccountId = '';
    this.selectedBeneficiaryId = '';
    this.transferAmount = null;
    this.transferCurrency = 'EUR';
    this.transferReason = '';
    this.errorMessage = '';
    this.isLoading = false;
  }

  private newIdempotencyKey(): string {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
      return crypto.randomUUID();
    }
    return `transfer-${Date.now()}-${Math.random().toString(16).slice(2)}`;
  }
}
