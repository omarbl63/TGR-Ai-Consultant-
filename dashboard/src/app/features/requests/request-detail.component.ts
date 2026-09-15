import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { CommonModule, KeyValue } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { RequestService } from '../../core/request.service';
import { AiService } from '../../core/ai.service';
import { AuthService } from '../../core/auth.service';
import { LabelsService } from '../../core/labels.service';
import { AskResponse, Attachment, DecisionAction, RequestDetail } from '../../core/models';
import { IconComponent } from '../../shared/icon.component';

@Component({
  selector: 'app-request-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, IconComponent],
  template: `
    @if (loading()) {
      <div class="space-y-4">
        <div class="skeleton h-8 w-64"></div>
        <div class="skeleton h-48 w-full"></div>
        <div class="skeleton h-64 w-full"></div>
      </div>
    } @else if (error()) {
      <div class="card p-8 text-center text-sm text-rose-600">{{ error() }}</div>
    } @else {
      @if (detail(); as d) {
      <!-- Breadcrumb + header -->
      <a routerLink="/demandes" class="mb-4 inline-flex items-center gap-1.5 text-sm text-ink-500 hover:text-ink-800">
        <app-icon name="back" [size]="16" /> Retour aux demandes
      </a>

      <div class="card mb-6 p-6">
        <div class="flex flex-wrap items-start justify-between gap-4">
          <div class="flex items-start gap-4">
            <div class="grid h-12 w-12 place-items-center rounded-xl bg-brand-50 text-brand-600">
              <app-icon [name]="labels.typeIconName(d.type)" [size]="24" />
            </div>
            <div>
              <div class="flex items-center gap-3">
                <h1 class="text-xl font-bold text-ink-900">{{ d.title }}</h1>
                <span class="badge" [ngClass]="labels.statusClass(d.status)">{{ labels.status(d.status) }}</span>
              </div>
              <div class="mt-1 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-ink-500">
                <span class="font-mono text-xs">{{ d.reference }}</span>
                <span>{{ labels.type(d.type) }}</span>
                <span class="inline-flex items-center gap-1"><app-icon name="users" [size]="15" /> {{ d.requesterName }}<span *ngIf="d.requesterDepartment"> · {{ d.requesterDepartment }}</span></span>
                <span class="inline-flex items-center gap-1"><app-icon name="calendar" [size]="15" /> {{ d.createdAt | date: 'dd MMM yyyy, HH:mm' }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="grid gap-6 lg:grid-cols-3">
        <!-- Main column -->
        <div class="space-y-6 lg:col-span-2">
          <!-- Structured data -->
          <div class="card p-6">
            <h2 class="mb-4 text-sm font-semibold uppercase tracking-wide text-ink-400">Détails de la demande</h2>
            @if (dataEntries(d).length === 0) {
              <p class="text-sm text-ink-400">Aucune donnée structurée.</p>
            } @else {
              <dl class="grid gap-x-8 gap-y-4 sm:grid-cols-2">
                @for (kv of dataEntries(d); track kv.key) {
                  <div>
                    <dt class="text-xs text-ink-400">{{ labels.field(kv.key) }}</dt>
                    <dd class="mt-0.5 text-sm font-medium text-ink-800">{{ format(kv.value) }}</dd>
                  </div>
                }
              </dl>
            }
          </div>

          <!-- Attachments -->
          <div class="card p-6">
            <h2 class="mb-4 text-sm font-semibold uppercase tracking-wide text-ink-400">Pièces jointes fournies</h2>
            @if (d.attachments.length === 0) {
              <p class="text-sm text-ink-400">Aucune pièce jointe fournie par l'employé.</p>
            } @else {
              <div class="space-y-2">
                @for (a of d.attachments; track a.id) {
                  <div class="flex items-center justify-between rounded-lg border border-ink-200 p-3">
                    <div class="flex min-w-0 items-center gap-3">
                      <span class="text-ink-400"><app-icon name="paperclip" [size]="18" /></span>
                      <div class="min-w-0">
                        <div class="truncate text-sm font-medium text-ink-800">{{ a.documentLabel || a.filename }}</div>
                        <div class="truncate text-xs text-ink-400">{{ a.filename }} · {{ kb(a.sizeBytes) }} Ko</div>
                      </div>
                    </div>
                    <button class="btn-outline shrink-0 text-xs" (click)="downloadAttachment(a)">
                      <app-icon name="download" [size]="15" /> Télécharger
                    </button>
                  </div>
                }
              </div>
            }
          </div>

          <!-- AI analysis -->
          <div class="card overflow-hidden">
            <div class="flex items-center justify-between border-b border-ink-100 bg-gradient-to-r from-brand-50/70 to-transparent px-6 py-4">
              <div class="flex items-center gap-2">
                <span class="grid h-8 w-8 place-items-center rounded-lg bg-brand-600 text-white"><app-icon name="sparkles" [size]="18" /></span>
                <div>
                  <h2 class="text-sm font-semibold text-ink-900">Analyse de l’IA</h2>
                  <p class="text-xs text-ink-400">Analyse de conformité — la décision revient au responsable</p>
                </div>
              </div>
              @if (isApprover()) {
                <button class="btn-ghost text-xs" (click)="analyze()" [disabled]="analyzing()">
                  {{ analyzing() ? 'Analyse…' : (d.analysis ? 'Réanalyser' : 'Analyser') }}
                </button>
              }
            </div>

            @if (!d.analysis) {
              <div class="px-6 py-10 text-center text-sm text-ink-400">
                Aucune analyse disponible pour l’instant.
              </div>
            } @else {
              <div class="space-y-6 p-6">
                <!-- Compliance banner (the AI no longer emits an approve/reject recommendation) -->
                <div class="flex flex-wrap items-center gap-3 rounded-xl bg-ink-50 p-4 ring-1 ring-inset ring-ink-200">
                  <app-icon name="check-circle" [size]="22" />
                  <div>
                    <div class="text-xs uppercase tracking-wide text-ink-400">Conformité</div>
                    <div class="text-base font-semibold text-ink-800">{{ labels.compliance(d.analysis.complianceStatus) }}</div>
                  </div>
                </div>

                <!-- Summary -->
                @if (d.analysis.summary) {
                  <div>
                    <h3 class="mb-1 text-xs font-semibold uppercase tracking-wide text-ink-400">Résumé</h3>
                    <p class="text-sm leading-relaxed text-ink-700">{{ d.analysis.summary }}</p>
                  </div>
                }

                <!-- Missing / anomalies -->
                <div class="grid gap-4 sm:grid-cols-3">
                  <div class="rounded-lg bg-ink-50 p-3">
                    <div class="text-xs font-semibold text-ink-500">Infos manquantes</div>
                    @if (d.analysis.missingInfo.length) {
                      <ul class="mt-1.5 space-y-1 text-sm text-ink-700">
                        @for (m of d.analysis.missingInfo; track m) { <li class="flex gap-1.5"><span class="text-amber-500">•</span>{{ m }}</li> }
                      </ul>
                    } @else { <p class="mt-1 text-sm text-emerald-600">Aucune</p> }
                  </div>
                  <div class="rounded-lg bg-ink-50 p-3">
                    <div class="text-xs font-semibold text-ink-500">Pièces manquantes</div>
                    @if (d.analysis.missingDocuments.length) {
                      <ul class="mt-1.5 space-y-1 text-sm text-ink-700">
                        @for (m of d.analysis.missingDocuments; track m) { <li class="flex gap-1.5"><span class="text-amber-500">•</span>{{ m }}</li> }
                      </ul>
                    } @else { <p class="mt-1 text-sm text-emerald-600">Aucune</p> }
                  </div>
                  <div class="rounded-lg bg-ink-50 p-3">
                    <div class="text-xs font-semibold text-ink-500">Anomalies</div>
                    @if (d.analysis.anomalies.length) {
                      <ul class="mt-1.5 space-y-1 text-sm text-ink-700">
                        @for (m of d.analysis.anomalies; track m) { <li class="flex gap-1.5"><span class="text-rose-500">•</span>{{ m }}</li> }
                      </ul>
                    } @else { <p class="mt-1 text-sm text-emerald-600">Aucune</p> }
                  </div>
                </div>

                <!-- Reasoning -->
                @if (d.analysis.reasoning) {
                  <div>
                    <h3 class="mb-1 text-xs font-semibold uppercase tracking-wide text-ink-400">Raisonnement</h3>
                    <p class="whitespace-pre-line text-sm leading-relaxed text-ink-700">{{ d.analysis.reasoning }}</p>
                  </div>
                }

                <!-- Citations -->
                @if (d.analysis.citations.length) {
                  <div>
                    <h3 class="mb-2 text-xs font-semibold uppercase tracking-wide text-ink-400">Textes réglementaires cités</h3>
                    <div class="space-y-2">
                      @for (c of d.analysis.citations; track $index) {
                        <div class="rounded-lg border border-ink-200 p-3">
                          <div class="flex items-center justify-between">
                            <span class="text-xs font-semibold text-brand-700">{{ c.docRef || 'Référence' }} <span class="font-normal text-ink-500">— {{ c.title }}</span></span>
                            <span class="text-[11px] text-ink-400">pertinence {{ (c.score || 0).toFixed(2) }}</span>
                          </div>
                          <p class="mt-1 line-clamp-2 text-xs text-ink-500">{{ c.excerpt }}</p>
                        </div>
                      }
                    </div>
                  </div>
                }
                <p class="text-right text-[11px] text-ink-300">Généré par {{ d.analysis.model }}</p>
              </div>
            }
          </div>
        </div>

        <!-- Side column -->
        <div class="space-y-6">
          <!-- Decision -->
          <div class="card p-6">
            <h2 class="mb-4 text-sm font-semibold uppercase tracking-wide text-ink-400">Décision</h2>
            @if (canDecide(d)) {
              <textarea class="input mb-3 min-h-20 resize-none" placeholder="Commentaire (facultatif)…" [(ngModel)]="comment"></textarea>
              @if (actionError()) { <p class="mb-2 text-xs text-rose-600">{{ actionError() }}</p> }
              <div class="grid gap-2">
                <button class="btn-success" (click)="decide('APPROVE')" [disabled]="acting()"><app-icon name="check" [size]="18" /> Approuver</button>
                <div class="grid grid-cols-2 gap-2">
                  <button class="btn-warning" (click)="decide('REQUEST_CHANGES')" [disabled]="acting()"><app-icon name="pencil" [size]="16" /> Modifier</button>
                  <button class="btn-danger" (click)="decide('REJECT')" [disabled]="acting()"><app-icon name="x" [size]="16" /> Rejeter</button>
                </div>
              </div>
              <p class="mt-3 text-[11px] leading-relaxed text-ink-400">
                L’analyse de l’IA est consultative. En validant, vous prenez la décision administrative finale.
              </p>
            } @else {
              <div class="rounded-lg p-4 ring-1 ring-inset" [ngClass]="labels.statusClass(d.status)">
                <div class="text-sm font-semibold">{{ labels.status(d.status) }}</div>
                @if (d.decidedByName) {
                  <div class="mt-1 text-xs opacity-80">Par {{ d.decidedByName }}<span *ngIf="d.decidedAt"> · {{ d.decidedAt | date: 'dd MMM yyyy, HH:mm' }}</span></div>
                }
              </div>
            }
          </div>

          <!-- Ask the AI -->
          @if (isApprover()) {
            <div class="card p-6">
              <h2 class="mb-1 flex items-center gap-2 text-sm font-semibold text-ink-900">
                <span class="grid h-6 w-6 place-items-center rounded-md bg-brand-600 text-white"><app-icon name="sparkles" [size]="14" /></span>
                Interroger l’IA
              </h2>
              <p class="mb-3 text-xs text-ink-400">Ex : « Cette demande est-elle conforme ? »</p>
              <div class="flex gap-2">
                <input class="input" placeholder="Votre question…" [(ngModel)]="question" (keyup.enter)="ask()" />
                <button class="btn-primary shrink-0" (click)="ask()" [disabled]="asking() || !question.trim()"><app-icon name="arrow-right" [size]="16" /></button>
              </div>
              @if (asking()) { <div class="mt-3 skeleton h-16 w-full"></div> }
              @if (answer(); as a) {
                <div class="mt-3 animate-slide-up rounded-lg bg-ink-50 p-3">
                  <p class="whitespace-pre-line text-sm leading-relaxed text-ink-700">{{ a.answer }}</p>
                  @if (a.citations.length) {
                    <div class="mt-2 flex flex-wrap gap-1">
                      @for (c of a.citations; track $index) {
                        <span class="badge bg-white text-brand-700 ring-brand-200">{{ c.docRef }}</span>
                      }
                    </div>
                  }
                </div>
              }
            </div>
          }

          <!-- Timeline -->
          <div class="card p-6">
            <h2 class="mb-4 text-sm font-semibold uppercase tracking-wide text-ink-400">Chronologie</h2>
            <ol class="relative space-y-5 border-l border-ink-200 pl-5">
              @for (e of d.timeline; track e.id) {
                <li class="relative">
                  <span class="absolute -left-[26px] top-0.5 grid h-4 w-4 place-items-center rounded-full ring-4 ring-white" [ngClass]="eventDot(e.type)"></span>
                  <div class="text-sm font-medium text-ink-800">{{ labels.event(e.type) }}</div>
                  <div class="text-xs text-ink-400">{{ e.actorName }} · {{ e.createdAt | date: 'dd MMM yyyy, HH:mm' }}</div>
                  @if (e.comment) { <div class="mt-1 rounded-md bg-ink-50 px-2 py-1 text-xs text-ink-600">“{{ e.comment }}”</div> }
                </li>
              }
            </ol>
          </div>
        </div>
      </div>
      }
    }
  `,
})
export class RequestDetailComponent implements OnInit {
  @Input() id?: string;

  private requests = inject(RequestService);
  private ai = inject(AiService);
  private auth = inject(AuthService);
  private router = inject(Router);
  labels = inject(LabelsService);

  detail = signal<RequestDetail | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);

  comment = '';
  acting = signal(false);
  actionError = signal<string | null>(null);
  analyzing = signal(false);

  question = '';
  asking = signal(false);
  answer = signal<AskResponse | null>(null);

  isApprover = this.auth.isApprover;

  ngOnInit() {
    if (this.id) this.load(this.id);
  }

  private load(id: string) {
    this.loading.set(true);
    this.requests.detail(id).subscribe({
      next: (d) => { this.detail.set(d); this.loading.set(false); },
      error: () => { this.error.set('Demande introuvable ou accès refusé.'); this.loading.set(false); },
    });
  }

  canDecide(d: RequestDetail) {
    return this.isApprover() && (d.status === 'SUBMITTED' || d.status === 'UNDER_REVIEW');
  }

  decide(action: DecisionAction) {
    const d = this.detail();
    if (!d) return;
    this.acting.set(true);
    this.actionError.set(null);
    this.requests.decide(d.id, action, this.comment.trim() || undefined).subscribe({
      next: (updated) => { this.detail.set(updated); this.comment = ''; this.acting.set(false); },
      error: (err) => { this.actionError.set(err?.error?.message ?? 'Action impossible.'); this.acting.set(false); },
    });
  }

  analyze() {
    const d = this.detail();
    if (!d) return;
    this.analyzing.set(true);
    this.ai.analyze(d.id).subscribe({
      next: () => { this.analyzing.set(false); this.load(d.id); },
      error: () => this.analyzing.set(false),
    });
  }

  ask() {
    const d = this.detail();
    if (!d || !this.question.trim()) return;
    this.asking.set(true);
    this.answer.set(null);
    this.ai.askAboutRequest(d.id, this.question.trim()).subscribe({
      next: (res) => { this.answer.set(res); this.asking.set(false); },
      error: () => { this.asking.set(false); },
    });
  }

  dataEntries(d: RequestDetail): KeyValue<string, unknown>[] {
    return Object.entries(d.structuredData ?? {}).map(([key, value]) => ({ key, value }));
  }

  format(v: unknown): string {
    if (v === null || v === undefined || v === '') return '—';
    if (typeof v === 'boolean') return v ? 'Oui' : 'Non';
    return String(v);
  }

  kb(bytes: number): number {
    return Math.max(1, Math.round(bytes / 1024));
  }

  downloadAttachment(a: Attachment) {
    const d = this.detail();
    if (!d) return;
    this.requests.downloadAttachment(d.id, a.id).subscribe((blob) => {
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = a.filename;
      link.click();
      URL.revokeObjectURL(url);
    });
  }

  eventDot(type: string) {
    if (type === 'APPROVED') return 'bg-emerald-500';
    if (type === 'REJECTED') return 'bg-rose-500';
    if (type === 'CHANGES_REQUESTED') return 'bg-amber-500';
    if (type === 'AI_ANALYZED') return 'bg-brand-500';
    return 'bg-ink-300';
  }
}
