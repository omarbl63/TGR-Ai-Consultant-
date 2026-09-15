export type Role = 'EMPLOYEE' | 'MANAGER' | 'ADMIN';

export interface User {
  id: string;
  email: string;
  fullName: string;
  role: Role;
  department?: string;
  jobTitle?: string;
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

export interface Citation {
  docRef?: string;
  title?: string;
  score?: number;
  excerpt?: string;
}

export interface ChatResponse {
  conversationId: string;
  reply: string;
  intent: string;
  createdRequestId?: string;
  requestReference?: string;
  recommendation?: Recommendation;
  missingInformation: string[];
  citations: Citation[];
  choices?: string[];
  awaitingDocuments?: boolean;
  requiredDocuments?: string[];
}

export interface RequestSummary {
  id: string;
  reference: string;
  type: RequestType;
  status: RequestStatus;
  title: string;
  recommendation?: Recommendation;
  riskLevel?: RiskLevel;
  hasAnalysis: boolean;
  createdAt: string;
  submittedAt?: string;
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
  citations: Citation[];
}

export interface RequestEvent {
  id: string;
  type: EventType;
  actorName?: string;
  comment?: string;
  createdAt: string;
}

export interface RequestDetail {
  id: string;
  reference: string;
  type: RequestType;
  status: RequestStatus;
  title: string;
  structuredData: Record<string, unknown>;
  createdAt: string;
  analysis?: AiAnalysis;
  timeline: RequestEvent[];
}

/** Local chat message model (UI). */
export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  text: string;
  pending?: boolean;
  requestId?: string;
  requestReference?: string;
  recommendation?: Recommendation;
  citations?: Citation[];
  choices?: string[];
}

export interface LeaveBalance {
  userId: string;
  userName?: string;
  department?: string;
  year: number;
  entitledDays: number;
  usedDays: number;
  remainingDays: number;
}

export interface RegisterPayload {
  email: string;
  password: string;
  fullName: string;
  department?: string;
  jobTitle?: string;
}
