import { Component, OnInit } from "@angular/core";
import { HttpClient } from "@angular/common/http";
interface Beneficiary { id: string; displayName: string; destinationAccountReference: string; status: string; }
@Component({ selector: "app-beneficiaries", template: `
<section class="card"><h2>Bénéficiaires</h2><app-alert [message]="error" type="error"></app-alert><app-alert [message]="success" type="success"></app-alert>
<form class="inline" (ngSubmit)="create()"><input name="displayName" [(ngModel)]="form.displayName" placeholder="Nom" required><input name="destination" [(ngModel)]="form.destinationAccountReference" placeholder="Référence compte destination" required><button type="submit">Ajouter</button></form>
<app-loading [show]="loading"></app-loading>
<table *ngIf="!loading && items.length"><thead><tr><th>Nom</th><th>Destination</th><th>Statut</th><th></th></tr></thead><tbody>
<tr *ngFor="let b of items"><td>{{b.displayName}}</td><td>{{b.destinationAccountReference}}</td><td>{{b.status}}</td><td><button class="danger" (click)="remove(b.id)">Supprimer</button></td></tr></tbody></table>
<p *ngIf="!loading && !items.length">Aucun bénéficiaire.</p></section>` })
export class BeneficiariesComponent implements OnInit {
  items: Beneficiary[] = []; form = { displayName: "", destinationAccountReference: "" }; loading = false; error = ""; success = "";
  constructor(private readonly http: HttpClient) {}
  ngOnInit(): void { this.load(); }
  load(): void { this.loading = true; this.http.get<Beneficiary[]>("/beneficiaries").subscribe({ next: i => { this.items = i; this.loading = false; }, error: () => { this.error = "Chargement impossible."; this.loading = false; } }); }
  create(): void { this.error = ""; this.success = ""; this.http.post("/beneficiaries", this.form).subscribe({ next: () => { this.success = "Bénéficiaire ajouté."; this.form = { displayName: "", destinationAccountReference: "" }; this.load(); }, error: () => this.error = "Création impossible." }); }
  remove(id: string): void { if (!confirm("Supprimer ce bénéficiaire ?")) return; this.http.delete(`/beneficiaries/${id}`).subscribe({ next: () => this.load(), error: () => this.error = "Suppression impossible." }); }
}