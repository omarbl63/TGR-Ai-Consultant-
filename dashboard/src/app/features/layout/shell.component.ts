import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { NotificationService } from '../../core/notification.service';
import { AppNotification } from '../../core/models';
import { IconComponent } from '../../shared/icon.component';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, IconComponent],
  template: `
    <div class="flex min-h-screen bg-ink-50">
      <!-- Sidebar -->
      <aside class="fixed inset-y-0 left-0 z-30 hidden w-64 flex-col border-r border-ink-200 bg-white lg:flex">
        <div class="flex h-16 items-center gap-3 border-b border-ink-200 px-5">
          <img src="tgr-logo.png" alt="Trésorerie Générale du Royaume" class="h-9 w-auto" />
          <div class="border-l border-ink-200 pl-3">
            <div class="text-sm font-semibold leading-tight text-ink-900">Trésorerie Générale du Royaume</div>
            <div class="text-[11px] text-ink-400">Espace d’approbation</div>
          </div>
        </div>

        <nav class="flex-1 space-y-1 px-3 py-4">
          <a routerLink="/tableau-de-bord" routerLinkActive="bg-brand-50 text-brand-700 font-medium"
             class="group flex items-center gap-3 rounded-lg px-3 py-2 text-sm text-ink-600 transition hover:bg-ink-100">
            <app-icon name="grid" [size]="20" /> Tableau de bord
          </a>
          @if (isApprover()) {
            <a routerLink="/demandes" routerLinkActive="bg-brand-50 text-brand-700 font-medium"
               class="group flex items-center gap-3 rounded-lg px-3 py-2 text-sm text-ink-600 transition hover:bg-ink-100">
              <app-icon name="requests" [size]="20" /> Demandes
            </a>
            <a routerLink="/employes" routerLinkActive="bg-brand-50 text-brand-700 font-medium"
               class="group flex items-center gap-3 rounded-lg px-3 py-2 text-sm text-ink-600 transition hover:bg-ink-100">
              <app-icon name="users" [size]="20" /> Employés
            </a>
          }
        </nav>

        <div class="border-t border-ink-200 p-3">
          <div class="flex items-center gap-3 rounded-lg px-2 py-2">
            <div class="grid h-9 w-9 place-items-center rounded-full bg-ink-200 text-sm font-semibold text-ink-700">{{ initials() }}</div>
            <div class="min-w-0 flex-1">
              <div class="truncate text-sm font-medium text-ink-900">{{ user()?.fullName }}</div>
              <div class="truncate text-xs text-ink-400">{{ roleLabel() }}</div>
            </div>
            <button (click)="logout()" title="Déconnexion" class="rounded-lg p-1.5 text-ink-400 hover:bg-ink-100 hover:text-rose-600">
              <app-icon name="logout" [size]="18" />
            </button>
          </div>
        </div>
      </aside>

      <!-- Main -->
      <div class="flex min-h-screen flex-1 flex-col lg:pl-64">
        <header class="sticky top-0 z-20 flex h-16 items-center justify-between border-b border-ink-200 bg-white/80 px-4 backdrop-blur sm:px-8">
          <div class="lg:hidden flex items-center gap-2 font-semibold">
            <img src="tgr-logo.png" alt="TGR" class="h-8 w-auto" /> Trésorerie Générale du Royaume
          </div>
          <div class="hidden text-sm text-ink-400 lg:block">Bonjour, {{ firstName() }}</div>

          <div class="relative flex items-center gap-2">
            <button (click)="toggleNotif()" class="relative rounded-lg p-2 text-ink-500 hover:bg-ink-100">
              <app-icon name="bell" [size]="20" />
              @if (unread() > 0) {
                <span class="absolute right-1 top-1 grid h-4 min-w-4 place-items-center rounded-full bg-rose-500 px-1 text-[10px] font-semibold text-white">{{ unread() }}</span>
              }
            </button>

            @if (notifOpen()) {
              <div class="absolute right-0 top-12 w-80 animate-slide-up rounded-xl border border-ink-200 bg-white shadow-pop">
                <div class="flex items-center justify-between border-b border-ink-100 px-4 py-3">
                  <span class="text-sm font-semibold text-ink-900">Notifications</span>
                  @if (notifications().length > 0) {
                    <button (click)="clearAll()" class="text-xs font-medium text-rose-600 hover:underline">Tout effacer</button>
                  }
                </div>
                <div class="max-h-96 overflow-y-auto">
                  @if (notifications().length === 0) {
                    <div class="px-4 py-8 text-center text-sm text-ink-400">Aucune notification.</div>
                  }
                  @for (n of notifications(); track n.id) {
                    <div class="group flex items-start gap-3 border-b border-ink-50 px-4 py-3 hover:bg-ink-50"
                         [ngClass]="{ 'bg-brand-50/40': !n.read }">
                      <span class="mt-1 h-2 w-2 shrink-0 rounded-full" [ngClass]="n.read ? 'bg-ink-200' : 'bg-brand-500'"></span>
                      <button (click)="openNotif(n)" class="min-w-0 flex-1 text-left">
                        <span class="block truncate text-sm font-medium text-ink-800">{{ n.title }}</span>
                        <span class="block text-xs text-ink-500">{{ n.message }}</span>
                      </button>
                      <button (click)="deleteNotif(n)" title="Supprimer"
                              class="shrink-0 rounded-md p-1 text-ink-300 hover:bg-ink-100 hover:text-rose-600">
                        <app-icon name="trash" [size]="15" />
                      </button>
                    </div>
                  }
                </div>
              </div>
            }
          </div>
        </header>

        <main class="flex-1 px-4 py-6 sm:px-8 sm:py-8">
          <div class="mx-auto max-w-7xl animate-fade-in">
            <router-outlet />
          </div>
        </main>
      </div>
    </div>
  `,
})
export class ShellComponent {
  private auth = inject(AuthService);
  private notif = inject(NotificationService);

  user = this.auth.currentUser;
  isApprover = this.auth.isApprover;
  notifOpen = signal(false);
  notifications = signal<AppNotification[]>([]);
  unread = signal(0);

  constructor() {
    this.refreshUnread();
  }

  firstName = () => this.user()?.fullName?.split(' ')[0] ?? '';
  initials = () =>
    (this.user()?.fullName ?? '')
      .split(' ')
      .map((p) => p[0])
      .slice(0, 2)
      .join('')
      .toUpperCase();
  roleLabel = () => {
    const map: Record<string, string> = { EMPLOYEE: 'Employé', MANAGER: 'Responsable', ADMIN: 'Administrateur' };
    return map[this.user()?.role ?? ''] ?? '';
  };

  toggleNotif() {
    const open = !this.notifOpen();
    this.notifOpen.set(open);
    if (open) this.notif.list().subscribe((list) => this.notifications.set(list));
  }

  openNotif(n: AppNotification) {
    if (!n.read) {
      this.notif.markRead(n.id).subscribe(() => {
        n.read = true;
        this.refreshUnread();
      });
    }
  }

  deleteNotif(n: AppNotification) {
    this.notif.delete(n.id).subscribe(() => {
      this.notifications.set(this.notifications().filter((x) => x.id !== n.id));
      this.refreshUnread();
    });
  }

  clearAll() {
    this.notif.deleteAll().subscribe(() => {
      this.notifications.set([]);
      this.refreshUnread();
    });
  }

  refreshUnread() {
    this.notif.unreadCount().subscribe((r) => this.unread.set(r.count));
  }

  logout() {
    this.auth.logout();
  }
}
