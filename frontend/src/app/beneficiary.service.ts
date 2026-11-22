import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Beneficiary,
  CreateBeneficiaryRequest,
  UpdateBeneficiaryRequest
} from './beneficiary.model';

@Injectable({ providedIn: 'root' })
export class BeneficiaryService {
  constructor(private readonly http: HttpClient) {}

  getBeneficiaries(): Observable<Beneficiary[]> {
    return this.http.get<Beneficiary[]>('/beneficiaries');
  }

  createBeneficiary(request: CreateBeneficiaryRequest): Observable<Beneficiary> {
    return this.http.post<Beneficiary>('/beneficiaries', request);
  }

  updateBeneficiary(
    id: string,
    request: UpdateBeneficiaryRequest
  ): Observable<Beneficiary> {
    return this.http.put<Beneficiary>(`/beneficiaries/${id}`, request);
  }

  deleteBeneficiary(id: string): Observable<void> {
    return this.http.delete<void>(`/beneficiaries/${id}`);
  }
}
