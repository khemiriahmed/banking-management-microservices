import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { LoginResponse } from '../models/auth.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly tokenKey = 'banking-access-token';

  constructor(private readonly http: HttpClient) {}

  login(email: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/auth/login', { email, password }).pipe(
      tap((response) => localStorage.setItem(this.tokenKey, response.accessToken))
    );
  }

  register(payload: { email: string; password: string }): Observable<unknown> {
    return this.http.post('/auth/register', payload);
  }

  get token(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  isAuthenticated(): boolean {
    return !!this.token && !this.isExpired(this.token);
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
  }

  get roles(): string[] {
    const payload = this.payload();
    const roles = payload?.roles;
    return Array.isArray(roles) ? roles : [];
  }

  hasRole(role: string): boolean {
    return this.roles.includes(role);
  }

  hasAnyRole(roles: string[]): boolean {
    return roles.some((role) => this.hasRole(role));
  }

  private payload(): { roles?: string[]; exp?: number } | null {
    const token = this.token;
    if (!token) return null;
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    try {
      const json = atob(parts[1].replace(/-/g, '+').replace(/_/g, '/'));
      return JSON.parse(json);
    } catch {
      return null;
    }
  }

  private isExpired(token: string): boolean {
    const parts = token.split('.');
    if (parts.length !== 3) return true;
    try {
      const payload = JSON.parse(atob(parts[1].replace(/-/g, '+').replace(/_/g, '/')));
      return typeof payload.exp === 'number' ? payload.exp * 1000 <= Date.now() : false;
    } catch {
      return true;
    }
  }
}
