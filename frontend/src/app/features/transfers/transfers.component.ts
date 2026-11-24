import { Component, OnInit } from "@angular/core";
import { HttpClient } from "@angular/common/http";

interface Account { id: string; accountNumberMasked: string; currency: string; balance: number | string; }
interface Beneficiary { id: string; displayName: string; destinationAccountReference: string; }
interface Transfer { id: string; amount: number; currency: string; status: string; failureReason: string | null; createdAt: string; }

@Component({
  selector: "app-transfers",
  template: `
  <section class="card"><h2>Virements</h2><app-alert [message]="error" type="error"></app-alert><app-alert [message]="success" type="success"></app-alert>
    <form (ngSubmit)="submit()">
      <label>Compte source <select name="source" [(ngModel)]="form.sourceAccountId" required><option value="">Sélectionner</option><option *ngFor="let a of accounts" [value]="a.id">{{a.accountNumberMasked}} - {{a.balance}} {{a.currency}}</option></select></label>
      <label>Bénéficiaire <select name="beneficiary" [(ngModel)]="form.beneficiaryId" required><option value="">Sélectionner</option><option *ngFor="let b of beneficiaries" [value]="b.id">{{b.displayName}} ({{b.destinationAccountReference}})</option></select></label>
      <label>Montant <input name="amount" type="number" min="0.01" step="0.01" [(ngModel)]="form.amount" required></label>
      <label>Devise <input name="currency" [(ngModel)]="form.currency" required></label>
      <label>Motif <input name="reason" [(ngModel)]="form.reason"></label>
      <button type="submit" [disabled]="loading">Envoyer</button>
    </form>
    <h3>Historique</h3><app-loading [show]="loadingList"></app-loading>
    <table *ngIf="!loadingList && transfers.length"><thead><tr><th>Date</th><th>Montant</th><th>Statut</th><th>Raison</th></tr></thead><tbody>
    <tr *ngFor="let t of transfers"><td>{{t.createdAt | date:'short'}}</td><td>{{t.amount}} {{t.currency}}</td><td>{{t.status}}</td><td>{{t.failureReason || '-'}}</td></tr></tbody></table>
  </section>`
})
export class TransfersComponent implements OnInit {
  accounts: Account[] = []; beneficiaries: Beneficiary[] = []; transfers: Transfer[] = [];
  form = { sourceAccountId: "", beneficiaryId: "", amount: 0, currency: "MAD", reason: "" };
  loading = false; loadingList = false; error = ""; success = "";
  constructor(private readonly http: HttpClient) {}
  ngOnInit(): void {
    this.http.get<Account[]>("/accounts").subscribe({ next: (a) => (this.accounts = a) });
    this.http.get<Beneficiary[]>("/beneficiaries").subscribe({ next: (b) => (this.beneficiaries = b) });
    this.load();
  }
  load(): void {
    this.loadingList = true;
    this.http.get<{ content: Transfer[] }>("/transfers?page=0&size=20").subscribe({
      next: (p) => { this.transfers = p.content; this.loadingList = false; },
      error: () => { this.loadingList = false; }
    });
  }
  submit(): void {
    this.loading = true; this.error = ""; this.success = "";
    const key = (globalThis.crypto && "randomUUID" in globalThis.crypto) ? globalThis.crypto.randomUUID() : `${Date.now()}-${Math.random()}`;
    this.http.post<Transfer>("/transfers", this.form, { headers: { "Idempotency-Key": key } }).subscribe({
      next: () => { this.loading = false; this.success = "Virement créé."; this.load(); },
      error: (err: any) => { this.loading = false; this.error = err?.error?.message || "Virement impossible."; }
    });
  }
}