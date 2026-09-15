import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { EmployeeService } from '../../core/employee.service';
import { RequestService } from '../../core/request.service';
import { LabelsService } from '../../core/labels.service';
import { EmployeeOverview, RequestSummary } from '../../core/models';
import { IconComponent } from '../../shared/icon.component';

const ROLE_LABEL: Record<string, string> = {
  EMPLOYEE: 'Employé', MANAGER: 'Responsable', ADMIN: 'Administrateur',
};

@Component({
  selector: 'app-employees',
  standalone: true,
  imports: [CommonModule, IconComponent],
  template: `
    <header class="mb-6">
      <h1 class="text-2xl font-bold tracking-tight text-ink-900">Employés</h1>
      <p class="mt-1 text-sm text-ink-500">Annuaire des agents, solde de congés et demandes.</p>
    </header>

    <section class="card overflow-hidden">
      @if (loading()) {
        <div class="space-y-3 p-5">
          @for (i of [1,2,3,4,5]; track i) { <div class="skeleton h-12 w-full"></div> }
        </div>
      } @else {
        <div class="overflow-x-auto">
          <table class="w-full text-left text-sm">
            <thead class="text-xs uppercase tracking-wide text-ink-400">
              <tr class="border-b border-ink-100">
                <th class="px-5 py-3 font-medium">Employé</th>
                <th class="px-5 py-3 font-medium">Service</th>
                <th class="px-5 py-3 font-medium">Rôle</th>
                <th class="px-5 py-3 font-medium">Congé restant</th>
                <th class="px-5 py-3 font-medium">Demandes</th>
                <th class="px-5 py-3"></th>
              </tr>
            </thead>
            <tbody>
              @for (e of employees(); track e.id) {
                <tr (click)="toggle(e.id)" class="cursor-pointer border-b border-ink-50 transition hover:bg-ink-50">
                  <td class="px-5 py-3">
                    <div class="flex items-center gap-3">
                      <div class="grid h-9 w-9 place-items-center rounded-full bg-brand-50 text-xs font-semibold text-brand-700">{{ initials(e) }}</div>
                      <div>
                        <div class="font-medium text-ink-900">{{ e.fullName }}</div>
                        <div class="text-xs text-ink-400">{{ e.email }}</div>
                      </div>
                    </div>
                  </td>
                  <td class="px-5 py-3 text-ink-600">{{ e.department || '—' }}</td>
                  <td class="px-5 py-3"><span class="badge bg-ink-100 text-ink-600 ring-ink-200">{{ roleLabel(e.role) }}</span></td>
                  <td class="px-5 py-3">
                    <span class="font-semibold text-ink-900">{{ round(e.remainingLeave) }}</span>
                    <span class="text-xs text-ink-400"> / {{ round(e.entitledLeave) }} j</span>
                  </td>
                  <td class="px-5 py-3">
                    <span class="badge bg-brand-50 text-brand-700 ring-brand-200">{{ requestsOf(e.id).length }}</span>
                  </td>
                  <td class="px-5 py-3 text-right text-ink-400">
                    <app-icon [name]="expanded() === e.id ? 'x' : 'arrow-right'" [size]="16" />
                  </td>
                </tr>
                @if (expanded() === e.id) {
                  <tr class="bg-ink-50/60">
                    <td colspan="6" class="px-5 py-4">
                      @if (requestsOf(e.id).length === 0) {
                        <p class="text-sm text-ink-400">Aucune demande pour cet employé.</p>
                      } @else {
                        <div class="space-y-2">
                          @for (r of requestsOf(e.id); track r.id) {
                            <button (click)="openRequest(r); $event.stopPropagation()"
                                    class="flex w-full items-center justify-between rounded-lg border border-ink-200 bg-white px-4 py-2.5 text-left hover:border-brand-300">
                              <span class="flex items-center gap-2 text-ink-700">
                                <app-icon [name]="labels.typeIconName(r.type)" [size]="16" />
                                <span class="font-mono text-xs">{{ r.reference }}</span>
                                <span class="text-sm">{{ r.title }}</span>
                              </span>
                              <span class="badge" [ngClass]="labels.statusClass(r.status)">{{ labels.status(r.status) }}</span>
                            </button>
                          }
                        </div>
                      }
                    </td>
                  </tr>
                }
              }
            </tbody>
          </table>
        </div>
      }
    </section>
  `,
})
export class EmployeesComponent {
  private employeeService = inject(EmployeeService);
  private requestService = inject(RequestService);
  private router = inject(Router);
  labels = inject(LabelsService);

  employees = signal<EmployeeOverview[]>([]);
  private requests = signal<RequestSummary[]>([]);
  loading = signal(true);
  expanded = signal<string | null>(null);

  private byRequester = computed(() => {
    const map = new Map<string, RequestSummary[]>();
    for (const r of this.requests()) {
      const list = map.get(r.requesterId) ?? [];
      list.push(r);
      map.set(r.requesterId, list);
    }
    return map;
  });

  constructor() {
    forkJoin({ emps: this.employeeService.directory(), reqs: this.requestService.all() }).subscribe({
      next: ({ emps, reqs }) => {
        this.employees.set(emps);
        this.requests.set(reqs);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  requestsOf(id: string): RequestSummary[] {
    return this.byRequester().get(id) ?? [];
  }

  toggle(id: string) {
    this.expanded.set(this.expanded() === id ? null : id);
  }

  openRequest(r: RequestSummary) {
    this.router.navigate(['/demandes', r.id]);
  }

  initials(e: EmployeeOverview) {
    return (e.fullName ?? '').split(' ').map((p) => p[0]).slice(0, 2).join('').toUpperCase();
  }

  roleLabel(role: string) {
    return ROLE_LABEL[role] ?? role;
  }

  round(v: number) {
    return Math.round(v);
  }
}
