import { Component, OnInit } from "@angular/core";
import { HttpClient } from "@angular/common/http";
interface Account { id: string; ownerUserId: string; accountNumberMasked: string; accountType: string; currency: string; status: string; balance: number | string; createdAt: string; }
@Component({ selector: "app-accounts", template: `
<section class="card"><h2>Comptes</h2><app-alert [message]="error" type="error"></app-alert><app-loading [show]="loading"></app-loading>
<table *ngIf="!loading && accounts.length"><thead><tr><th>Numéro</th><th>Type</th><th>Devise</th><th>Statut</th><th>Solde</th><th>Créé</th></tr></thead>
<tbody><tr *ngFor="let a of accounts"><td>{{a.accountNumberMasked}}</td><td>{{a.accountType}}</td><td>{{a.currency}}</td><td>{{a.status}}</td><td>{{a.balance}}</td><td>{{a.createdAt | date:'short'}}</td></tr></tbody></table>
<p *ngIf="!loading && !accounts.length">Aucun compte.</p></section>` })
export class AccountsComponent implements OnInit {
  accounts: Account[] = []; loading = false; error = "";
  constructor(private readonly http: HttpClient) {}
  ngOnInit(): void { this.loading = true; this.http.get<Account[]>("/accounts").subscribe({ next: a => { this.accounts = a; this.loading = false; }, error: () => { this.http.get<Account[]>("/admin/accounts").subscribe({ next: a => { this.accounts = a; this.loading = false; }, error: () => { this.error = "Impossible de charger les comptes."; this.loading = false; } }); } }); }
}