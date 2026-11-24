import { Component } from "@angular/core";
import { Router } from "@angular/router";
import { HttpClient } from "@angular/common/http";

@Component({ selector: "app-register", template: `
<section class="card narrow"><h2>Inscription</h2><app-alert [message]="error" type="error"></app-alert><app-alert [message]="success" type="success"></app-alert>
<form (ngSubmit)="submit()"><label>Email <input name="email" type="email" [(ngModel)]="email" required></label><label>Mot de passe <input name="password" type="password" [(ngModel)]="password" required></label><button type="submit" [disabled]="loading">{{ loading ? 'Création...' : 'Créer le compte' }}</button></form>
<p><a routerLink="/login">Déjà inscrit ?</a></p></section>` })
export class RegisterComponent {
  email = ""; password = ""; error = ""; success = ""; loading = false;
  constructor(private readonly http: HttpClient, private readonly router: Router) {}
  submit(): void {
    this.loading = true; this.error = ""; this.success = "";
    this.http.post("/auth/register", { email: this.email, password: this.password }).subscribe({
      next: () => { this.loading = false; this.success = "Compte créé. Connectez-vous."; setTimeout(() => this.router.navigate(["/login"]), 500); },
      error: (err: any) => { this.loading = false; this.error = err?.status === 409 ? "Email déjà utilisé." : "Inscription impossible."; }
    });
  }
}