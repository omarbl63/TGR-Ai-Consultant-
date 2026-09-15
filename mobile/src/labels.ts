import { ComplianceStatus, Recommendation, RequestStatus, RequestType, RiskLevel } from './types';

export const typeLabel: Record<RequestType, string> = {
  LEAVE: 'Congé',
  MISSION_ORDER: 'Ordre de mission',
  EXPENSE: 'Remboursement de frais',
};

/** Ionicons names per request type. */
export const typeIcon: Record<RequestType, string> = {
  LEAVE: 'calendar-outline',
  MISSION_ORDER: 'airplane-outline',
  EXPENSE: 'receipt-outline',
};

export const statusLabel: Record<RequestStatus, string> = {
  DRAFT: 'Brouillon',
  SUBMITTED: 'Soumise',
  UNDER_REVIEW: 'En cours d’examen',
  APPROVED: 'Approuvée',
  REJECTED: 'Rejetée',
  CHANGES_REQUESTED: 'Modifications demandées',
};

export const statusBadge: Record<RequestStatus, string> = {
  DRAFT: 'bg-ink-100 text-ink-600',
  SUBMITTED: 'bg-brand-50 text-brand-700',
  UNDER_REVIEW: 'bg-amber-50 text-amber-700',
  APPROVED: 'bg-emerald-50 text-emerald-700',
  REJECTED: 'bg-rose-50 text-rose-700',
  CHANGES_REQUESTED: 'bg-orange-50 text-orange-700',
};

export const recoLabel: Record<Recommendation, string> = {
  APPROVE: 'Approbation',
  REJECT: 'Rejet',
  REQUEST_CHANGES: 'Modifications',
};

export const recoBadge: Record<Recommendation, string> = {
  APPROVE: 'bg-emerald-50 text-emerald-700',
  REJECT: 'bg-rose-50 text-rose-700',
  REQUEST_CHANGES: 'bg-amber-50 text-amber-700',
};

export const riskLabel: Record<RiskLevel, string> = {
  LOW: 'Faible',
  MEDIUM: 'Moyen',
  HIGH: 'Élevé',
};

export const riskBadge: Record<RiskLevel, string> = {
  LOW: 'bg-emerald-50 text-emerald-700',
  MEDIUM: 'bg-amber-50 text-amber-700',
  HIGH: 'bg-rose-50 text-rose-700',
};

export const complianceLabel: Record<ComplianceStatus, string> = {
  COMPLIANT: 'Conforme',
  COMPLIANT_WITH_RESERVATION: 'Conforme avec réserve',
  INCOMPLETE: 'Incomplète',
  NON_COMPLIANT: 'Non conforme',
};

export const fieldLabel: Record<string, string> = {
  leaveType: 'Type de congé', startDate: 'Date de début', endDate: 'Date de fin',
  days: 'Nombre de jours', reason: 'Motif', destination: 'Destination', country: 'Pays',
  purpose: 'Objet', departureDate: 'Date de départ', returnDate: 'Date de retour',
  transport: 'Transport', needsAdvance: 'Avance', amount: 'Montant', currency: 'Devise',
  category: 'Catégorie', description: 'Description', missionReference: 'Référence mission',
};

export function fieldName(key: string): string {
  return fieldLabel[key] ?? key;
}

export function formatDate(iso?: string): string {
  if (!iso) return '—';
  try {
    return new Date(iso).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' });
  } catch {
    return iso;
  }
}
