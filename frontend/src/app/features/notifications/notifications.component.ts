import { Component, OnInit } from "@angular/core";
import { HttpClient } from "@angular/common/http";

interface NotificationItem { id: string; eventType: string; title: string; message: string; read: boolean; occurredAt: string; createdAt: string; }
interface NotificationPage { content: NotificationItem[]; }

@Component({
  selector: "app-notifications",
  template: `
  <section class="card"><h2>Notifications</h2><app-alert [message]="error" type="error"></app-alert><app-loading [show]="loading"></app-loading>
    <div *ngFor="let n of items" class="notif" [class.unread]="!n.read">
      <div><strong>{{n.title}}</strong> <small>{{n.occurredAt | date:'short'}}</small><div>{{n.message}}</div><small>{{n.eventType}}</small></div>
      <button *ngIf="!n.read" (click)="read(n)">Marquer lue</button>
    </div>
    <p *ngIf="!loading && !items.length">Aucune notification.</p>
  </section>`
})
export class NotificationsComponent implements OnInit {
  items: NotificationItem[] = []; loading = false; error = "";
  constructor(private readonly http: HttpClient) {}
  ngOnInit(): void { this.load(); }
  load(): void { this.loading = true; this.http.get<NotificationPage>("/notifications?page=0&size=20").subscribe({ next: (p) => { this.items = p.content; this.loading = false; }, error: () => { this.error = "Chargement impossible."; this.loading = false; } }); }
  read(n: NotificationItem): void { this.http.patch<NotificationItem>(`/notifications/${n.id}/read`, {}).subscribe({ next: (u) => n.read = u.read, error: () => this.error = "Action impossible." }); }
}