import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { BeneficiaryService } from './beneficiary.service';

describe('BeneficiaryService', () => {
  let service: BeneficiaryService;
  let httpController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule]
    });
    service = TestBed.inject(BeneficiaryService);
    httpController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpController.verify();
  });

  it('loads beneficiaries from the gateway', () => {
    service.getBeneficiaries().subscribe((beneficiaries) => {
      expect(beneficiaries.length).toBe(1);
    });

    const request = httpController.expectOne('/beneficiaries');
    expect(request.request.method).toBe('GET');
    request.flush([{
      id: 'd0e1f2a3-4b5c-4d6e-8f90-1a2b3c4d5e6f',
      ownerUserId: '7d4f0c8e-0b1e-4f6a-9e7a-1b6f0f8d6c22',
      displayName: 'Alice',
      destinationAccountReference: 'FR76 **** **** **** 1234',
      status: 'ACTIVE',
      createdAt: '2026-01-01T10:00:00Z'
    }]);
  });

  it('creates a beneficiary with a masked reference', () => {
    service.createBeneficiary({
      displayName: 'Alice',
      destinationAccountReference: 'FR76 **** **** **** 1234'
    }).subscribe((beneficiary) => {
      expect(beneficiary.displayName).toBe('Alice');
    });

    const request = httpController.expectOne('/beneficiaries');
    expect(request.request.method).toBe('POST');
    expect(request.request.body.destinationAccountReference).toBe('FR76 **** **** **** 1234');
    request.flush({
      id: 'd0e1f2a3-4b5c-4d6e-8f90-1a2b3c4d5e6f',
      ownerUserId: '7d4f0c8e-0b1e-4f6a-9e7a-1b6f0f8d6c22',
      displayName: 'Alice',
      destinationAccountReference: 'FR76 **** **** **** 1234',
      status: 'ACTIVE',
      createdAt: '2026-01-01T10:00:00Z'
    });
  });

  it('deletes a beneficiary by id', () => {
    service.deleteBeneficiary('d0e1f2a3-4b5c-4d6e-8f90-1a2b3c4d5e6f').subscribe();

    const request = httpController.expectOne('/beneficiaries/d0e1f2a3-4b5c-4d6e-8f90-1a2b3c4d5e6f');
    expect(request.request.method).toBe('DELETE');
    request.flush(null);
  });
});
