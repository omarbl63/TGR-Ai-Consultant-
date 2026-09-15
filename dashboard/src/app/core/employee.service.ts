import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { EmployeeOverview } from './models';

@Injectable({ providedIn: 'root' })
export class EmployeeService {
  private http = inject(HttpClient);

  directory(): Observable<EmployeeOverview[]> {
    return this.http.get<EmployeeOverview[]>(`${environment.apiBase}/users`);
  }
}
