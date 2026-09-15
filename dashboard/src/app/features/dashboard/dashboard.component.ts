import { Component, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { RequestService } from '../../core/request.service';
import { LabelsService } from '../../core/labels.service';
import { RequestStats, RequestSummary } from '../../core/models';
import { IconComponent } from '../../shared/icon.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, IconComponent],
  template: `
    <header class="mb-6">
      <h1 class="text-2xl font-bold tracking-tight text-ink-900">Tableau de bord</h1>
      <p class="mt-1 text-sm text-ink-500">Vue d’ensemble des demandes et de la file d’attente d’approbation.</p>
    </header>

    <!-- KPI cards -->
    <section class="grid grid-cols-2 gap-4 lg:grid-cols-4">
      @for (kpi of kpis(); track kpi.label) {
        <div class="card p-5 transition hover:shadow-card-hover">
          <div class="flex items-center justify-between">
            <span class="text-sm text-ink-500">{{ kpi.label }}</span>
            <span class="grid h-8 w-8 place-items-center rounded-lg" [ngClass]="kpi.tint">
              <app-icon [name]="kpi.icon" [size]="18" />
            </span>
          </div>
          @if (loadingStats()) {
            <div class="skeleton mt-3 h-8 w-16"></div>
          } @else {
            <div class="mt-2 text-3xl font-bold tracking-tight text-ink-900">{{ kpi.value }}</div>
          }
        </div>
      }
    </section>

    <!-- Pending queue -->
    <section class="card mt-6 overflow-hidden">
      <div class="flex items-center justify-between border-b border-ink-100 px-5 py-4">
        <div>
          <h2 class="text-base font-semibold text-ink-900">File d’attente</h2>
          <p class="text-xs text-ink-400">Demandes en attente de votre décision</p>
        </div>
        <span class="badge bg-brand-50 text-brand-700 ring-brand-200">{{ pending().length }} en attente</span>
      </div>

      @if (loadingList()) {
        <div class="space-y-3 p-5">
          @for (i of [1,2,3,4]; track i) { <div class="skeleton h-12 w-full"></div> }
        </div>
      } @else if (error()) {
        <div class="p-8 text-center text-sm text-rose-600">{{ error() }}</div>
      } @else if (pending().length === 0) {
        <div class="flex flex-col items-center justify-center gap-2 py-16 text-center">
          <span class="text-emerald-500"><app-icon name="check-circle" [size]="40" /></span>
          <p class="text-sm font-medium text-ink-700">Aucune demande en attente</p>
          <p class="text-xs text-ink-400">Tout est à jour. Revenez plus tard.</p>
        </div>
      } @else {
        <div class="overflow-x-auto">
          <table class="w-full text-left text-sm">
            <thead class="text-xs uppercase tracking-wide text-ink-400">
              <tr class="border-b border-ink-100">
                <th class="px-5 py-3 font-medium">Référence</th>
                <th class="px-5 py-3 font-medium">Type</th>
                <th class="px-5 py-3 font-medium">Demandeur</th>
                <th class="px-5 py-3 font-medium">Soumise le</th>
                <th class="px-5 py-3"></th>
              </tr>
            </thead>
            <tbody>
              @for (r of pending(); track r.id) {
                <tr (click)="open(r)" class="cursor-pointer border-b border-ink-50 transition hover:bg-ink-50">
                  <td class="px-5 py-3 font-mono text-xs font-medium text-ink-800">{{ r.reference }}</td>
                  <td class="px-5 py-3">
                    <span class="inline-flex items-center gap-2 text-ink-600">
                      <app-icon [name]="labels.typeIconName(r.type)" [size]="17" />{{ labels.type(r.type) }}
                    </span>
                  </td>
                  <td class="px-5 py-3 text-ink-700">{{ r.requesterName }}</td>
                  <td class="px-5 py-3 text-ink-500">{{ r.submittedAt ? (r.submittedAt | date: 'dd MMM yyyy') : '—' }}</td>
                  <td class="px-5 py-3 text-right">
                    <span class="text-brand-600">Examiner →</span>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </section>
  `,
})
export class DashboardComponent implements OnDestroy {
  private requests = inject(RequestService);
  private router = inject(Router);
  labels = inject(LabelsService);

  stats = signal<RequestStats | null>(null);
  pending = signal<RequestSummary[]>([]);
  loadingStats = signal(true);
  loadingList = signal(true);
  error = signal<string | null>(null);
  private timer: ReturnType<typeof setInterval> | undefined;

  constructor() {
    this.reload(true);
    // Live refresh so newly submitted mobile requests appear without a manual reload.
    this.timer = setInterval(() => this.reload(false), 15000);
  }

  ngOnDestroy() {
    if (this.timer) clearInterval(this.timer);
  }

  private reload(initial: boolean) {
    this.requests.stats().subscribe({
      next: (s) => { this.stats.set(s); if (initial) this.loadingStats.set(false); },
      error: () => { if (initial) this.loadingStats.set(false); },
    });
    this.requests.pending().subscribe({
      next: (list) => { this.pending.set(list); if (initial) this.loadingList.set(false); },
      error: () => { this.error.set('Impossible de charger la file d’attente.'); if (initial) this.loadingList.set(false); },
    });
  }

  kpis = () => {
    const s = this.stats();
    return [
      { label: 'En attente', value: s?.pending ?? 0, icon: 'clock', tint: 'bg-brand-50 text-brand-600' },
      { label: 'Approuvées', value: s?.approved ?? 0, icon: 'check-circle', tint: 'bg-emerald-50 text-emerald-600' },
      { label: 'Rejetées', value: s?.rejected ?? 0, icon: 'x-circle', tint: 'bg-rose-50 text-rose-600' },
      { label: 'Total', value: s?.total ?? 0, icon: 'chart', tint: 'bg-ink-100 text-ink-600' },
    ];
  };

  open(r: RequestSummary) {
    this.router.navigate(['/demandes', r.id]);
  }
}
