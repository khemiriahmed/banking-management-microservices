import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, UrlTree } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({ providedIn: 'root' })
export class RoleGuard implements CanActivate {
  constructor(private readonly auth: AuthService, private readonly router: Router) {}

  canActivate(route: ActivatedRouteSnapshot): boolean | UrlTree {
    const roles = (route.data['roles'] as string[] | undefined) ?? [];
    if (!this.auth.isAuthenticated()) {
      return this.router.parseUrl('/login');
    }
    if (roles.length === 0 || this.auth.hasAnyRole(roles)) {
      return true;
    }
    return this.router.parseUrl('/forbidden');
  }
}
