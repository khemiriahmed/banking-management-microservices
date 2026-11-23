import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CreateTransferRequest, Transfer, TransferPage } from './transfer.model';

@Injectable({ providedIn: 'root' })
export class TransferService {
  constructor(private readonly http: HttpClient) {}

  getTransfers(): Observable<TransferPage> {
    return this.http.get<TransferPage>('/transfers?page=0&size=20');
  }

  createTransfer(
    request: CreateTransferRequest,
    idempotencyKey: string
  ): Observable<Transfer> {
    return this.http.post<Transfer>('/transfers', request, {
      headers: { 'Idempotency-Key': idempotencyKey }
    });
  }
}
