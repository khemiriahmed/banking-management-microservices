import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AuditPage, UserPage } from '../models/admin.model';

@Injectable({ providedIn: 'root' })
export class AdminService {
  constructor(private readonly http: HttpClient) {}
  users(page = 0, size = 20): Observable<UserPage> { return this.http.get<UserPage>(`/admin/users?page=${page}&size=${size}`); }
  audit(page = 0, size = 20): Observable<AuditPage> { return this.http.get<AuditPage>(`/admin/audit?page=${page}&size=${size}`); }
}
