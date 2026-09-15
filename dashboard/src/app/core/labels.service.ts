import { Injectable } from '@angular/core';
import {
  ComplianceStatus, EventType, Recommendation, RequestStatus, RequestType, RiskLevel,
} from './models';

/**
 * Central French labels and badge styles for enum values, so the UI shows
 * human French text while the API keeps English enum values.
 */
@Injectable({ providedIn: 'root' })
export class LabelsService {
  private static readonly TYPE: Record<RequestType, string> = {
    LEAVE: 'Congé',
    MISSION_ORDER: 'Ordre de mission',
    EXPENSE: 'Remboursement de frais',
  };
  private static readonly TYPE_ICON: Record<RequestType, string> = {
    LEAVE: '🌴',
    MISSION_ORDER: '✈️',
    EXPENSE: '🧾',
  };
  private static readonly STATUS: Record<RequestStatus, string> = {
    DRAFT: 'Brouillon',
    SUBMITTED: 'Soumise',
    UNDER_REVIEW: 'En cours d’examen',
    APPROVED: 'Approuvée',
    REJECTED: 'Rejetée',
    CHANGES_REQUESTED: 'Modifications demandées',
  };
  private static readonly STATUS_CLASS: Record<RequestStatus, string> = {
    DRAFT: 'bg-ink-100 text-ink-600 ring-ink-200',
    SUBMITTED: 'bg-brand-50 text-brand-700 ring-brand-200',
    UNDER_REVIEW: 'bg-amber-50 text-amber-700 ring-amber-200',
    APPROVED: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
    REJECTED: 'bg-rose-50 text-rose-700 ring-rose-200',
    CHANGES_REQUESTED: 'bg-orange-50 text-orange-700 ring-orange-200',
  };
  private static readonly RECO: Record<Recommendation, string> = {
    APPROVE: 'Approbation',
    REJECT: 'Rejet',
    REQUEST_CHANGES: 'Modifications',
  };
  private static readonly RECO_CLASS: Record<Recommendation, string> = {
    APPROVE: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
    REJECT: 'bg-rose-50 text-rose-700 ring-rose-200',
    REQUEST_CHANGES: 'bg-amber-50 text-amber-700 ring-amber-200',
  };
  private static readonly RISK: Record<RiskLevel, string> = {
    LOW: 'Faible',
    MEDIUM: 'Moyen',
    HIGH: 'Élevé',
  };
  private static readonly RISK_CLASS: Record<RiskLevel, string> = {
    LOW: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
    MEDIUM: 'bg-amber-50 text-amber-700 ring-amber-200',
    HIGH: 'bg-rose-50 text-rose-700 ring-rose-200',
  };
  private static readonly COMPLIANCE: Record<ComplianceStatus, string> = {
    COMPLIANT: 'Conforme',
    COMPLIANT_WITH_RESERVATION: 'Conforme avec réserve',
    INCOMPLETE: 'Incomplète',
    NON_COMPLIANT: 'Non conforme',
  };
  private static readonly EVENT: Record<EventType, string> = {
    CREATED: 'Demande créée',
    SUBMITTED: 'Soumise pour validation',
    AI_ANALYZED: 'Analyse IA réalisée',
    APPROVED: 'Approuvée',
    REJECTED: 'Rejetée',
    CHANGES_REQUESTED: 'Modifications demandées',
    COMMENTED: 'Commentaire',
  };
  private static readonly FIELD: Record<string, string> = {
    leaveType: 'Type de congé',
    startDate: 'Date de début',
    endDate: 'Date de fin',
    days: 'Nombre de jours',
    reason: 'Motif',
    destination: 'Destination',
    country: 'Pays',
    purpose: 'Objet de la mission',
    departureDate: 'Date de départ',
    returnDate: 'Date de retour',
    transport: 'Moyen de transport',
    needsAdvance: 'Avance demandée',
    amount: 'Montant',
    currency: 'Devise',
    category: 'Catégorie',
    description: 'Description',
    missionReference: 'Référence de mission',
  };

  type(t: RequestType) { return LabelsService.TYPE[t] ?? t; }
  typeIcon(t: RequestType) { return LabelsService.TYPE_ICON[t] ?? '📄'; }
  status(s: RequestStatus) { return LabelsService.STATUS[s] ?? s; }
  statusClass(s: RequestStatus) { return LabelsService.STATUS_CLASS[s] ?? 'bg-ink-100 text-ink-600 ring-ink-200'; }
  reco(r?: Recommendation) { return r ? LabelsService.RECO[r] : '—'; }
  recoClass(r?: Recommendation) { return r ? LabelsService.RECO_CLASS[r] : 'bg-ink-100 text-ink-600 ring-ink-200'; }
  risk(r?: RiskLevel) { return r ? LabelsService.RISK[r] : '—'; }
  riskClass(r?: RiskLevel) { return r ? LabelsService.RISK_CLASS[r] : 'bg-ink-100 text-ink-600 ring-ink-200'; }
  compliance(c?: ComplianceStatus) { return c ? LabelsService.COMPLIANCE[c] : '—'; }
  event(e: EventType) { return LabelsService.EVENT[e] ?? e; }
  field(key: string) { return LabelsService.FIELD[key] ?? key; }

  typeIconName(t: RequestType): string {
    return t === 'LEAVE' ? 'calendar' : t === 'MISSION_ORDER' ? 'plane' : 'receipt';
  }
  recoIconName(r?: Recommendation): string {
    return r === 'APPROVE' ? 'check-circle' : r === 'REJECT' ? 'x-circle' : 'pencil';
  }
}
