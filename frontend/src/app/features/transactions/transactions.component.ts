import { Component, OnInit } from "@angular/core";
import { HttpClient, HttpParams } from "@angular/common/http";

interface TransactionRecord { id: string; externalReference: string; transferId: string; type: string; amount: number; currency: string; occurredAt: string; status: string; }
interface TransactionPage { content: TransactionRecord[]; }

@Component({
  selector: "app-transactions",
  template: `
  <section class="card"><h2>Transactions</h2><app-alert [message]="error" type="error"></app-alert>
    <form class="inline" (ngSubmit)="load()"><input name="accountId" [(ngModel)]="accountId" placeholder="Account ID"><input name="from" type="datetime-local" [(ngModel)]="from"><input name="to" type="datetime-local" [(ngModel)]="to"><button type="submit">Filtrer</button></form>
    <app-loading [show]="loading"></app-loading>
    <table *ngIf="!loading && items.length"><thead><tr><th>Date</th><th>Type</th><th>Montant</th><th>Statut</th><th>Référence</th></tr></thead><tbody>
    <tr *ngFor="let t of items"><td>{{t.occurredAt | date:'short'}}</td><td>{{t.type}}</td><td>{{t.amount}} {{t.currency}}</td><td>{{t.status}}</td><td>{{t.externalReference}}</td></tr></tbody></table>
    <p *ngIf="!loading && !items.length">Aucune transaction.</p>
  </section>`
})
export class TransactionsComponent implements OnInit {
  items: TransactionRecord[] = []; accountId = ""; from = ""; to = ""; loading = false; error = "";
  constructor(private readonly http: HttpClient) {}
  ngOnInit(): void { this.load(); }
  load(): void {
    this.loading = true; this.error = "";
    let params = new HttpParams();
    if (this.accountId) params = params.set("accountId", this.accountId);
    if (this.from) params = params.set("from", new Date(this.from).toISOString());
    if (this.to) params = params.set("to", new Date(this.to).toISOString());
    params = params.set("page", "0");
    this.http.get<TransactionPage>("/transactions", { params }).subscribe({
      next: (p) => { this.items = p.content; this.loading = false; },
      error: () => { this.error = "Chargement impossible."; this.loading = false; }
    });
  }
}