import { Component, OnInit } from "@angular/core";
import { HttpClient } from "@angular/common/http";
interface Account { accountNumberMasked: string; accountType: string; status: string; balance: number | string; currency: string; }
interface TransactionRecord { id: string; }
@Component({ selector: "app-dashboard", template: `
<section class="card"><h2>Tableau de bord</h2><app-alert [message]="error" type="error"></app-alert><app-loading [show]="loading"></app-loading>
<div class="stats" *ngIf="!loading"><div class="stat"><strong>{{accounts.length}}</strong><span>Comptes</span></div><div class="stat"><strong>{{txCount}}</strong><span>Transactions récentes</span></div></div>
<h3>Comptes</h3><table *ngIf="accounts.length"><thead><tr><th>Numéro</th><th>Type</th><th>Statut</th><th>Solde</th></tr></thead><tbody>
<tr *ngFor="let a of accounts"><td>{{a.accountNumberMasked}}</td><td>{{a.accountType}}</td><td>{{a.status}}</td><td>{{a.balance}} {{a.currency}}</td></tr></tbody></table>
<p *ngIf="!loading && !accounts.length">Aucun compte.</p></section>` })
export class DashboardComponent implements OnInit {
  accounts: Account[] = []; txCount = 0; loading = false; error = "";
  constructor(private readonly http: HttpClient) {}
  ngOnInit(): void { this.loading = true; this.http.get<Account[]>("/accounts").subscribe({ next: a => { this.accounts = a; this.http.get<{content:TransactionRecord[]}>("/transactions?page=0&size=5").subscribe({ next: p => { this.txCount = p.content.length; this.loading = false; }, error: () => { this.loading = false; } }); }, error: () => { this.error = "Chargement impossible."; this.loading = false; } }); }
}