import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { TransactionPage } from '../models/transaction.model';

@Injectable({ providedIn: 'root' })
export class TransactionService {
  constructor(private readonly http: HttpClient) {}
  getTransactions(filters: { accountId?: string; from?: string; to?: string; page?: number } = {}): Observable<TransactionPage> {
    let params = new HttpParams();
    if (filters.accountId) params = params.set('accountId', filters.accountId);
    if (filters.from) params = params.set('from', filters.from);
    if (filters.to) params = params.set('to', filters.to);
    if (filters.page !== undefined) params = params.set('page', String(filters.page));
    return this.http.get<TransactionPage>('/transactions', { params });
  }
}
