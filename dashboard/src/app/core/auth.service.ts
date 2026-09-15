import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthResponse, Role, User } from './models';

const TOKEN_KEY = 'adminai.token';
const REFRESH_KEY = 'adminai.refresh';
const USER_KEY = 'adminai.user';
const APPROVER_ROLES: Role[] = ['MANAGER', 'ADMIN'];

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  readonly currentUser = signal<User | null>(this.loadUser());
  readonly isAuthenticated = computed(() => !!this.currentUser());
  readonly isApprover = computed(() => {
    const role = this.currentUser()?.role;
    return !!role && APPROVER_ROLES.includes(role);
  });
  readonly isAdmin = computed(() => this.currentUser()?.role === 'ADMIN');

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${environment.apiBase}/auth/login`, { email, password })
      .pipe(tap((res) => this.setSession(res)));
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem(USER_KEY);
    this.currentUser.set(null);
    this.router.navigate(['/connexion']);
  }

  token(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  private setSession(res: AuthResponse): void {
    localStorage.setItem(TOKEN_KEY, res.accessToken);
    localStorage.setItem(REFRESH_KEY, res.refreshToken);
    localStorage.setItem(USER_KEY, JSON.stringify(res.user));
    this.currentUser.set(res.user);
  }

  private loadUser(): User | null {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as User;
    } catch {
      return null;
    }
  }
}
