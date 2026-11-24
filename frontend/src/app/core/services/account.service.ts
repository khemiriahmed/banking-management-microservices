import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Account } from '../models/account.model';

@Injectable({ providedIn: 'root' })
export class AccountService {
  constructor(private readonly http: HttpClient) {}
  getAccounts(): Observable<Account[]> { return this.http.get<Account[]>('/accounts'); }
  getAdminAccounts(): Observable<Account[]> { return this.http.get<Account[]>('/admin/accounts'); }
}
