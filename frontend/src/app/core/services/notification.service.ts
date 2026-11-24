import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { NotificationItem, NotificationPage } from '../models/notification.model';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  constructor(private readonly http: HttpClient) {}
  list(page = 0, size = 20): Observable<NotificationPage> { return this.http.get<NotificationPage>(`/notifications?page=${page}&size=${size}`); }
  markRead(id: string): Observable<NotificationItem> { return this.http.patch<NotificationItem>(`/notifications/${id}/read`, {}); }
}
