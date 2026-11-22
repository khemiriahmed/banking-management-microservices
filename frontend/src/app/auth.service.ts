import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { LoginResponse } from './login-response.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly tokenKey = 'banking-access-token';

  constructor(private readonly http: HttpClient) {}

  login(email: string, password: string): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>('/auth/login', { email, password })
      .pipe(tap((response) => localStorage.setItem(this.tokenKey, response.accessToken)));
  }

  get token(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  isAuthenticated(): boolean {
    return this.token !== null;
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
  }
}
