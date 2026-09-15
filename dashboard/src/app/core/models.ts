// Shared API types mirroring the backend DTOs.

export type Role = 'EMPLOYEE' | 'MANAGER' | 'ADMIN';

export interface User {
  id: string;
  email: string;
  fullName: string;
  role: Role;
  department?: string;
  jobTitle?: string;
  active: boolean;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export type RequestType = 'LEAVE' | 'MISSION_ORDER' | 'EXPENSE';
export type RequestStatus =
  | 'DRAFT' | 'SUBMITTED' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED' | 'CHANGES_REQUESTED';
export type Recommendation = 'APPROVE' | 'REJECT' | 'REQUEST_CHANGES';
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH';
export type ComplianceStatus =
  | 'COMPLIANT' | 'COMPLIANT_WITH_RESERVATION' | 'INCOMPLETE' | 'NON_COMPLIANT';
export type EventType =
  | 'CREATED' | 'SUBMITTED' | 'AI_ANALYZED' | 'APPROVED' | 'REJECTED' | 'CHANGES_REQUESTED' | 'COMMENTED';
export type DecisionAction = 'APPROVE' | 'REJECT' | 'REQUEST_CHANGES';

export interface RequestSummary {
  id: string;
  reference: string;
  type: RequestType;
  status: RequestStatus;
  title: string;
  requesterId: string;
  requesterName?: string;
  hasAnalysis: boolean;
  recommendation?: Recommendation;
  riskLevel?: RiskLevel;
  confidenceScore?: number;
  createdAt: string;
  submittedAt?: string;
}

export interface Citation {
  docRef?: string;
  title?: string;
  source?: string;
  chunkIndex?: number;
  score?: number;
  excerpt?: string;
}

export interface AiAnalysis {
  summary?: string;
  confidenceScore?: number;
  riskScore?: number;
  riskLevel?: RiskLevel;
  complianceStatus?: ComplianceStatus;
  recommendation?: Recommendation;
  reasoning?: string;
  missingInfo: string[];
  missingDocuments: string[];
  anomalies: string[];
  model?: string;
  createdAt: string;
  citations: Citation[];
}

export interface RequestEvent {
  id: string;
  type: EventType;
  actorId?: string;
  actorName?: string;
  comment?: string;
  createdAt: string;
}

export interface Attachment {
  id: string;
  filename: string;
  contentType?: string;
  sizeBytes: number;
  documentLabel?: string;
  createdAt: string;
}

export interface RequestDetail {
  id: string;
  reference: string;
  type: RequestType;
  status: RequestStatus;
  title: string;
  requesterId: string;
  requesterName?: string;
  requesterDepartment?: string;
  conversationId?: string;
  structuredData: Record<string, unknown>;
  createdAt: string;
  submittedAt?: string;
  decidedAt?: string;
  decidedBy?: string;
  decidedByName?: string;
  analysis?: AiAnalysis;
  timeline: RequestEvent[];
  attachments: Attachment[];
}

export interface RequestStats {
  pending: number;
  approved: number;
  rejected: number;
  changesRequested: number;
  total: number;
}

export interface AppNotification {
  id: string;
  type: string;
  title: string;
  message?: string;
  requestId?: string;
  read: boolean;
  createdAt: string;
}

export interface AskResponse {
  answer: string;
  citations: Citation[];
}

export interface EmployeeOverview {
  id: string;
  fullName: string;
  email: string;
  role: Role;
  department?: string;
  jobTitle?: string;
  entitledLeave: number;
  usedLeave: number;
  remainingLeave: number;
}
