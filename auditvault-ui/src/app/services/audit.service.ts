import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuditEvent, Page } from '../models/audit.model';

@Injectable({
  providedIn: 'root'
})
export class AuditService {
  private readonly baseUrl = '/api/audit';

  constructor(private http: HttpClient) {}

  getEvents(aggregateId: string, page: number = 0, size: number = 100): Observable<Page<AuditEvent>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<Page<AuditEvent>>(`${this.baseUrl}/events/${aggregateId}`, { params });
  }

  getConsolidatedState(aggregateId: string, targetTime?: string): Observable<any> {
    let params = new HttpParams();
    if (targetTime) {
      params = params.set('targetTime', targetTime);
    }
    return this.http.get<any>(`${this.baseUrl}/state/${aggregateId}`, { params });
  }

  triggerExport(aggregateId: string): Observable<{ jobExecutionId: number; status: string; aggregateId: string }> {
    return this.http.post<{ jobExecutionId: number; status: string; aggregateId: string }>(`${this.baseUrl}/export/${aggregateId}`, {});
  }

  search(query: string, page: number = 0, size: number = 100): Observable<Page<AuditEvent>> {
    const params = new HttpParams()
      .set('query', query)
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<Page<AuditEvent>>(`${this.baseUrl}/search`, { params });
  }

  connectToAuditStream(): Observable<AuditEvent> {
    return new Observable<AuditEvent>((observer) => {
      const eventSource = new EventSource(`${this.baseUrl}/stream`);

      eventSource.addEventListener('audit-event', (event: MessageEvent) => {
        try {
          const auditEvent = JSON.parse(event.data) as AuditEvent;
          observer.next(auditEvent);
        } catch (err) {
          console.error('Failed to parse SSE message', err);
        }
      });

      eventSource.onerror = (error) => {
        console.error('SSE connection error, EventSource will attempt to reconnect natively.', error);
        // We do not call observer.error() here because we want to let native EventSource handle reconnection
      };

      return () => {
        eventSource.close();
      };
    });
  }
}
