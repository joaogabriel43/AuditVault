import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuditService } from './audit.service';
import { Page, AuditEvent } from '../models/audit.model';

describe('AuditService', () => {
  let service: AuditService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AuditService]
    });
    service = TestBed.inject(AuditService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch events with pagination', () => {
    const aggregateId = 'agg-123';
    const mockPage: Page<AuditEvent> = {
      content: [{ id: '1', aggregateId: 'agg-123', eventType: 'CREATED', timestamp: '2023-01-01T00:00:00Z', payload: '{}', obfuscated: false, aggregateType: 'User', userId: 'usr1' }],
      pageable: { pageNumber: 0, pageSize: 100 },
      totalElements: 1,
      totalPages: 1,
      last: true,
      first: true,
      size: 100,
      number: 0,
      empty: false
    };

    service.getEvents(aggregateId).subscribe(page => {
      expect(page.content.length).toBe(1);
      expect(page.content[0].id).toBe('1');
    });

    const req = httpMock.expectOne(`/api/audit/events/${aggregateId}?page=0&size=100`);
    expect(req.request.method).toBe('GET');
    req.flush(mockPage);
  });

  it('should trigger export', () => {
    const aggregateId = 'agg-123';
    const mockResponse = { jobExecutionId: 10, status: 'STARTED', aggregateId };

    service.triggerExport(aggregateId).subscribe(res => {
      expect(res.jobExecutionId).toBe(10);
    });

    const req = httpMock.expectOne(`/api/audit/export/${aggregateId}`);
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);
  });
});
