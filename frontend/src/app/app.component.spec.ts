import { HTTP_INTERCEPTORS } from '@angular/common/http';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { AppComponent } from './app.component';
import { AuthInterceptor } from './auth.interceptor';

describe('AppComponent', () => {
  let component: AppComponent;
  let fixture: ComponentFixture<AppComponent>;
  let httpController: HttpTestingController;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      declarations: [AppComponent],
      imports: [HttpClientTestingModule, FormsModule],
      providers: [
        { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AppComponent);
    component = fixture.componentInstance;
    httpController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
    httpController.verify();
  });

  it('creates the component', () => {
    expect(component).toBeTruthy();
  });

  it('authenticates and loads accounts and beneficiaries with the bearer token', () => {
    component.email = 'client@example.com';
    component.password = 'password';

    component.login();

    const loginRequest = httpController.expectOne('/auth/login');
    expect(loginRequest.request.method).toBe('POST');
    loginRequest.flush({
      accessToken: 'test-token',
      tokenType: 'Bearer',
      expiresIn: 3600,
      roles: ['CLIENT']
    });

    const accountsRequest = httpController.expectOne('/accounts');
    expect(accountsRequest.request.headers.get('Authorization')).toBe('Bearer test-token');
    accountsRequest.flush([
      {
        id: 'e4d4f0c8-0b1e-4f6a-9e7a-1b6f0f8d6c11',
        ownerUserId: '7d4f0c8e-0b1e-4f6a-9e7a-1b6f0f8d6c22',
        accountNumberMasked: '**************1234',
        accountType: 'CHECKING',
        currency: 'EUR',
        status: 'ACTIVE',
        balance: 100.5,
        createdAt: '2026-01-01T10:00:00Z'
      }
    ]);

    const beneficiariesRequest = httpController.expectOne('/beneficiaries');
    expect(beneficiariesRequest.request.method).toBe('GET');
    expect(beneficiariesRequest.request.headers.get('Authorization')).toBe('Bearer test-token');
    beneficiariesRequest.flush([{
      id: 'd0e1f2a3-4b5c-4d6e-8f90-1a2b3c4d5e6f',
      ownerUserId: '7d4f0c8e-0b1e-4f6a-9e7a-1b6f0f8d6c22',
      displayName: 'Alice',
      destinationAccountReference: 'FR76 **** **** **** 1234',
      status: 'ACTIVE',
      createdAt: '2026-01-01T10:00:00Z'
    }]);

    expect(component.authenticated).toBeTrue();
    expect(component.accounts.length).toBe(1);
    expect(component.beneficiaries.length).toBe(1);
  });

  it('creates a beneficiary and refreshes the masked list', () => {
    localStorage.setItem('banking-access-token', 'test-token');
    component.beneficiaryName = 'Alice';
    component.beneficiaryReference = 'FR76 **** **** **** 1234';

    component.createBeneficiary();

    const createRequest = httpController.expectOne('/beneficiaries');
    expect(createRequest.request.method).toBe('POST');
    expect(createRequest.request.body).toEqual({
      displayName: 'Alice',
      destinationAccountReference: 'FR76 **** **** **** 1234'
    });
    createRequest.flush({
      id: 'd0e1f2a3-4b5c-4d6e-8f90-1a2b3c4d5e6f',
      ownerUserId: '7d4f0c8e-0b1e-4f6a-9e7a-1b6f0f8d6c22',
      displayName: 'Alice',
      destinationAccountReference: 'FR76 **** **** **** 1234',
      status: 'ACTIVE',
      createdAt: '2026-01-01T10:00:00Z'
    });

    const listRequest = httpController.expectOne('/beneficiaries');
    expect(listRequest.request.method).toBe('GET');
    listRequest.flush([{
      id: 'd0e1f2a3-4b5c-4d6e-8f90-1a2b3c4d5e6f',
      ownerUserId: '7d4f0c8e-0b1e-4f6a-9e7a-1b6f0f8d6c22',
      displayName: 'Alice',
      destinationAccountReference: 'FR76 **** **** **** 1234',
      status: 'ACTIVE',
      createdAt: '2026-01-01T10:00:00Z'
    }]);

    expect(component.beneficiaries.length).toBe(1);
    expect(component.beneficiaryName).toBe('');
  });

  it('clears the session on logout', () => {
    localStorage.setItem('banking-access-token', 'test-token');

    component.logout();

    expect(component.authenticated).toBeFalse();
    expect(component.accounts.length).toBe(0);
    expect(component.beneficiaries.length).toBe(0);
    expect(localStorage.getItem('banking-access-token')).toBeNull();
  });
});
