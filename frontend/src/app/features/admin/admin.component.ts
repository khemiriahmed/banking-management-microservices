import { Component, OnInit } from "@angular/core";
import { HttpClient } from "@angular/common/http";
interface UserProfile { authUserId: string; email: string; firstName?: string; lastName?: string; }
interface AuditEventItem { occurredAt: string; action: string; eventType: string; actorId: string | null; targetId: string | null; }
@Component({ selector: "app-admin", template: `
<section class="card"><h2>Administration</h2><app-alert [message]="error" type="error"></app-alert><app-loading [show]="loading"></app-loading>
<h3>Utilisateurs</h3><table *ngIf="users.length"><thead><tr><th>ID</th><th>Email</th><th>Nom</th></tr></thead><tbody>
<tr *ngFor="let u of users"><td>{{u.authUserId}}</td><td>{{u.email}}</td><td>{{u.firstName || ''}} {{u.lastName || ''}}</td></tr></tbody></table>
<h3>Audit</h3><table *ngIf="audit.length"><thead><tr><th>Date</th><th>Action</th><th>Type</th><th>Acteur</th><th>Cible</th></tr></thead><tbody>
<tr *ngFor="let a of audit"><td>{{a.occurredAt | date:'short'}}</td><td>{{a.action}}</td><td>{{a.eventType}}</td><td>{{a.actorId || '-'}}</td><td>{{a.targetId || '-'}}</td></tr></tbody></table></section>` })
export class AdminComponent implements OnInit {
  users: UserProfile[] = []; audit: AuditEventItem[] = []; loading = false; error = "";
  constructor(private readonly http: HttpClient) {}
  ngOnInit(): void { this.loading = true; this.http.get<{content:UserProfile[]}>("/admin/users?page=0&size=20").subscribe({ next: u => { this.users = u.content; this.http.get<{content:AuditEventItem[]}>("/admin/audit?page=0&size=20").subscribe({ next: a => { this.audit = a.content; this.loading = false; }, error: () => { this.error = "Audit inaccessible."; this.loading = false; } }); }, error: () => { this.error = "Chargement admin impossible."; this.loading = false; } }); }
}