import { Component, OnDestroy, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { RequestService } from '../../core/request.service';
import { LabelsService } from '../../core/labels.service';
import { RequestStatus, RequestSummary } from '../../core/models';
import { IconComponent } from '../../shared/icon.component';

@Component({
  selector: 'app-requests-list',
  standalone: true,
  imports: [CommonModule, IconComponent],
  template: `
    <header class="mb-6 flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 class="text-2xl font-bold tracking-tight text-ink-900">Demandes</h1>
        <p class="mt-1 text-sm text-ink-500">Toutes les demandes administratives.</p>
      </div>
      <div class="flex flex-wrap gap-1 rounded-lg border border-ink-200 bg-white p-1">
        @for (f of filters; track f.value) {
          <button (click)="filter.set(f.value)"
                  class="rounded-md px-3 py-1.5 text-sm transition"
                  [class]="filter() === f.value ? 'bg-brand-600 text-white' : 'text-ink-600 hover:bg-ink-100'">
            {{ f.label }}
          </button>
        }
      </div>
    </header>

    <section class="card overflow-hidden">
      @if (loading()) {
        <div class="space-y-3 p-5">
          @for (i of [1,2,3,4,5]; track i) { <div class="skeleton h-12 w-full"></div> }
        </div>
      } @else if (visible().length === 0) {
        <div class="py-16 text-center text-sm text-ink-400">Aucune demande dans cette catégorie.</div>
      } @else {
        <div class="overflow-x-auto">
          <table class="w-full text-left text-sm">
            <thead class="text-xs uppercase tracking-wide text-ink-400">
              <tr class="border-b border-ink-100">
                <th class="px-5 py-3 font-medium">Référence</th>
                <th class="px-5 py-3 font-medium">Type</th>
                <th class="px-5 py-3 font-medium">Objet</th>
                <th class="px-5 py-3 font-medium">Demandeur</th>
                <th class="px-5 py-3 font-medium">Statut</th>
                <th class="px-5 py-3 font-medium">Créée le</th>
              </tr>
            </thead>
            <tbody>
              @for (r of visible(); track r.id) {
                <tr (click)="open(r)" class="cursor-pointer border-b border-ink-50 transition hover:bg-ink-50">
                  <td class="px-5 py-3 font-mono text-xs font-medium text-ink-800">{{ r.reference }}</td>
                  <td class="px-5 py-3">
                    <span class="inline-flex items-center gap-2 text-ink-600">
                      <app-icon [name]="labels.typeIconName(r.type)" [size]="17" />{{ labels.type(r.type) }}
                    </span>
                  </td>
                  <td class="px-5 py-3 max-w-xs truncate text-ink-700">{{ r.title }}</td>
                  <td class="px-5 py-3 text-ink-600">{{ r.requesterName }}</td>
                  <td class="px-5 py-3"><span class="badge" [ngClass]="labels.statusClass(r.status)">{{ labels.status(r.status) }}</span></td>
                  <td class="px-5 py-3 text-ink-500">{{ r.createdAt | date: 'dd MMM yyyy' }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </section>
  `,
})
export class RequestsListComponent implements OnDestroy {
  private requests = inject(RequestService);
  private router = inject(Router);
  labels = inject(LabelsService);
  private timer: ReturnType<typeof setInterval> | undefined;

  all = signal<RequestSummary[]>([]);
  loading = signal(true);
  filter = signal<RequestStatus | 'ALL' | 'PENDING'>('ALL');

  filters: { label: string; value: RequestStatus | 'ALL' | 'PENDING' }[] = [
    { label: 'Toutes', value: 'ALL' },
    { label: 'En attente', value: 'PENDING' },
    { label: 'Approuvées', value: 'APPROVED' },
    { label: 'Rejetées', value: 'REJECTED' },
    { label: 'Modifications', value: 'CHANGES_REQUESTED' },
  ];

  visible = computed(() => {
    const f = this.filter();
    const list = this.all();
    if (f === 'ALL') return list;
    if (f === 'PENDING') return list.filter((r) => r.status === 'SUBMITTED' || r.status === 'UNDER_REVIEW');
    return list.filter((r) => r.status === f);
  });

  constructor() {
    this.reload(true);
    // Live refresh: new mobile requests appear without a manual reload.
    this.timer = setInterval(() => this.reload(false), 15000);
  }

  ngOnDestroy() {
    if (this.timer) clearInterval(this.timer);
  }

  private reload(initial: boolean) {
    this.requests.all().subscribe({
      next: (list) => { this.all.set(list); if (initial) this.loading.set(false); },
      error: () => { if (initial) this.loading.set(false); },
    });
  }

  open(r: RequestSummary) {
    this.router.navigate(['/demandes', r.id]);
  }
}
