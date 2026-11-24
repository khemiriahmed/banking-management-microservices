import { Component } from "@angular/core";
import { Router } from "@angular/router";
import { HttpClient } from "@angular/common/http";

interface LoginResponse { accessToken: string; roles: string[]; }

@Component({ selector: "app-login", template: `
<section class="card narrow"><h2>Connexion</h2><app-alert [message]="error" type="error"></app-alert>
<form (ngSubmit)="submit()"><label>Email <input name="email" type="email" [(ngModel)]="email" required></label><label>Mot de passe <input name="password" type="password" [(ngModel)]="password" required></label><button type="submit" [disabled]="loading">{{ loading ? 'Connexion...' : 'Se connecter' }}</button></form>
<p><a routerLink="/register">Créer un compte</a></p></section>` })
export class LoginComponent {
  email = ""; password = ""; error = ""; loading = false;
  constructor(private readonly http: HttpClient, private readonly router: Router) {}
  submit(): void {
    this.loading = true; this.error = "";
    this.http.post<LoginResponse>("/auth/login", { email: this.email, password: this.password }).subscribe({
      next: (r) => { localStorage.setItem("banking-access-token", r.accessToken); this.loading = false; this.router.navigate([r.roles?.includes("ADMIN") ? "/admin" : "/dashboard"]); },
      error: () => { this.loading = false; this.error = "Identifiants invalides."; }
    });
  }
}