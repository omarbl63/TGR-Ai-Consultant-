import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { DecisionAction, RequestDetail, RequestStats, RequestSummary } from './models';

@Injectable({ providedIn: 'root' })
export class RequestService {
  private http = inject(HttpClient);
  private base = `${environment.apiBase}/requests`;

  pending(): Observable<RequestSummary[]> {
    return this.http.get<RequestSummary[]>(`${this.base}/pending`);
  }

  all(): Observable<RequestSummary[]> {
    return this.http.get<RequestSummary[]>(this.base);
  }

  mine(): Observable<RequestSummary[]> {
    return this.http.get<RequestSummary[]>(`${this.base}/mine`);
  }

  stats(): Observable<RequestStats> {
    return this.http.get<RequestStats>(`${this.base}/stats`);
  }

  detail(id: string): Observable<RequestDetail> {
    return this.http.get<RequestDetail>(`${this.base}/${id}`);
  }

  decide(id: string, action: DecisionAction, comment?: string): Observable<RequestDetail> {
    return this.http.post<RequestDetail>(`${this.base}/${id}/decision`, { action, comment });
  }

  downloadAttachment(requestId: string, attachmentId: string): Observable<Blob> {
    return this.http.get(`${this.base}/${requestId}/attachments/${attachmentId}`, { responseType: 'blob' });
  }
}
