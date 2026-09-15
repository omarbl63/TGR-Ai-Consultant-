import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { AiAnalysis, AskResponse } from './models';

@Injectable({ providedIn: 'root' })
export class AiService {
  private http = inject(HttpClient);
  private base = `${environment.apiBase}/ai`;

  /** (Re)run the AI compliance analysis for a request. */
  analyze(requestId: string): Observable<AiAnalysis> {
    return this.http.post<AiAnalysis>(`${this.base}/requests/${requestId}/analyze`, {});
  }

  /** Ask the AI about a specific request (grounded in its analysis + regulations). */
  askAboutRequest(requestId: string, question: string): Observable<AskResponse> {
    return this.http.post<AskResponse>(`${this.base}/requests/${requestId}/ask`, { question });
  }

  /** General administrative RAG Q&A. */
  ask(question: string): Observable<AskResponse> {
    return this.http.post<AskResponse>(`${this.base}/ask`, { question });
  }
}
