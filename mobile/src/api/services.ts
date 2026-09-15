import { api, uploadFile } from './client';
import { AuthResponse, ChatResponse, LeaveBalance, RegisterPayload, RequestDetail, RequestSummary, User } from '../types';

export const AuthApi = {
  login: (email: string, password: string) =>
    api<AuthResponse>('/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) }),
  register: (payload: RegisterPayload) =>
    api<AuthResponse>('/auth/register', { method: 'POST', body: JSON.stringify(payload) }),
};

export const UserApi = {
  /** Current user's profile — also used to validate a stored token at startup. */
  me: () => api<User>('/users/me'),
};

export const ChatApi = {
  send: (message: string, conversationId?: string) =>
    api<ChatResponse>('/ai/chat', {
      method: 'POST',
      body: JSON.stringify({ message, conversationId: conversationId ?? null }),
    }),
  ask: (question: string) =>
    api<{ answer: string; citations: { docRef?: string }[] }>('/ai/ask', {
      method: 'POST',
      body: JSON.stringify({ question }),
    }),
};

export const RequestApi = {
  mine: () => api<RequestSummary[]>('/requests/mine'),
  detail: (id: string) => api<RequestDetail>(`/requests/${id}`),
  /** Update an editable request's fields (DRAFT or CHANGES_REQUESTED). */
  update: (id: string, structuredData: Record<string, unknown>) =>
    api<RequestDetail>(`/requests/${id}`, { method: 'PUT', body: JSON.stringify({ structuredData }) }),
  /** Re-submit a request after the manager asked for changes. */
  submit: (id: string) => api<RequestDetail>(`/requests/${id}/submit`, { method: 'POST' }),
  uploadAttachment: (
    id: string,
    file: { uri: string; name: string; type?: string; file?: File },
    label?: string,
  ) => uploadFile<{ id: string; filename: string }>(`/requests/${id}/attachments`, file, label),
};

export const LeaveApi = {
  me: () => api<LeaveBalance>('/leave-balances/me'),
};
