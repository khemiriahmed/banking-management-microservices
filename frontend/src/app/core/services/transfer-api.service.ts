import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CreateTransferRequest, Transfer, TransferPage } from '../models/transfer.model';

@Injectable({ providedIn: 'root' })
export class TransferService {
  constructor(private readonly http: HttpClient) {}
  getTransfers(page = 0, size = 20): Observable<TransferPage> {
    return this.http.get<TransferPage>(`/transfers?page=${page}&size=${size}`);
  }
  createTransfer(request: CreateTransferRequest, idempotencyKey: string): Observable<Transfer> {
    return this.http.post<Transfer>('/transfers', request, { headers: { 'Idempotency-Key': idempotencyKey } });
  }
}