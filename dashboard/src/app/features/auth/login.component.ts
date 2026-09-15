import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="min-h-screen grid lg:grid-cols-2">
      <!-- Brand panel -->
      <div class="relative hidden lg:flex flex-col justify-between overflow-hidden bg-ink-900 p-12 text-white">
        <div class="absolute inset-0 opacity-50"
             style="background:
               radial-gradient(60rem 60rem at 20% -10%, rgba(240,127,23,.45), transparent 55%),
               radial-gradient(50rem 50rem at 90% 110%, rgba(217,108,12,.35), transparent 55%);"></div>
        <div class="relative flex items-center gap-3">
          <div class="rounded-xl bg-white p-2 shadow-pop">
            <img src="tgr-logo.png" alt="Trésorerie Générale du Royaume" class="h-9 w-auto" />
          </div>
          <span class="text-lg font-semibold tracking-tight">Trésorerie Générale du Royaume</span>
        </div>
        <div class="relative max-w-md">
          <h1 class="text-3xl font-bold leading-tight">L’assistant administratif intelligent</h1>
          <p class="mt-4 text-ink-300">
            Les demandes sont analysées, validées au regard des règlements et expliquées par l’IA.
            La décision finale reste entre vos mains.
          </p>
          <div class="mt-8 flex flex-wrap gap-2 text-xs text-ink-300">
            <span class="rounded-full bg-white/10 px-3 py-1">Analyse de conformité</span>
            <span class="rounded-full bg-white/10 px-3 py-1">RAG · citations</span>
            <span class="rounded-full bg-white/10 px-3 py-1">Décision humaine</span>
          </div>
        </div>
        <p class="relative text-xs text-ink-400">Simulation inspirée de la Trésorerie Générale du Royaume.</p>
      </div>

      <!-- Form panel -->
      <div class="flex items-center justify-center p-6 sm:p-12">
        <div class="w-full max-w-sm animate-slide-up">
          <div class="mb-8 lg:hidden flex items-center gap-3">
            <img src="tgr-logo.png" alt="Trésorerie Générale du Royaume" class="h-10 w-auto" />
            <span class="text-lg font-semibold">Trésorerie Générale du Royaume</span>
          </div>

          <h2 class="text-2xl font-bold text-ink-900">Connexion</h2>
          <p class="mt-1 text-sm text-ink-500">Accédez à votre espace d’approbation.</p>

          <form class="mt-8 space-y-4" (ngSubmit)="submit()">
            <div>
              <label class="label" for="email">Adresse e-mail</label>
              <input id="email" name="email" type="email" class="input" placeholder="prenom.nom@adminai.ma"
                     [(ngModel)]="email" autocomplete="username" required />
            </div>
            <div>
              <label class="label" for="password">Mot de passe</label>
              <input id="password" name="password" type="password" class="input" placeholder="••••••••"
                     [(ngModel)]="password" autocomplete="current-password" required />
            </div>

            @if (error()) {
              <div class="rounded-lg bg-rose-50 px-3.5 py-2.5 text-sm text-rose-700 ring-1 ring-inset ring-rose-200">
                {{ error() }}
              </div>
            }

            <button type="submit" class="btn-primary w-full" [disabled]="loading()">
              @if (loading()) {
                <svg class="h-4 w-4 animate-spin" viewBox="0 0 24 24" fill="none">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"></path>
                </svg>
                Connexion…
              } @else { Se connecter }
            </button>
          </form>

          <div class="mt-8 rounded-xl border border-ink-200 bg-ink-50/60 p-4">
            <p class="text-xs font-medium text-ink-500">Comptes de démonstration (mot de passe : Password123!)</p>
            <div class="mt-2 grid gap-1 text-xs text-ink-600">
              <button class="text-left hover:text-brand-700" (click)="fill('manager@adminai.ma')">👔 manager&#64;adminai.ma — Responsable</button>
              <button class="text-left hover:text-brand-700" (click)="fill('directeur@adminai.ma')">👔 directeur&#64;adminai.ma — Responsable</button>
              <button class="text-left hover:text-brand-700" (click)="fill('admin@adminai.ma')">⚙️ admin&#64;adminai.ma — Administrateur</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class LoginComponent {
  private auth = inject(AuthService);
  private router = inject(Router);

  email = 'manager@adminai.ma';
  password = 'Password123!';
  loading = signal(false);
  error = signal<string | null>(null);

  fill(email: string) {
    this.email = email;
    this.password = 'Password123!';
  }

  submit() {
    if (!this.email || !this.password) return;
    this.loading.set(true);
    this.error.set(null);
    this.auth.login(this.email, this.password).subscribe({
      next: () => this.router.navigate(['/tableau-de-bord']),
      error: (err) => {
        this.error.set(err?.error?.message ?? 'Échec de la connexion. Veuillez réessayer.');
        this.loading.set(false);
      },
    });
  }
}
