export interface AuditEvent {
  id: string;
  aggregateId: string;
  aggregateType: string;
  eventType: string;
  timestamp: string;
  userId: string;
  payload: string;
  obfuscated: boolean;
  isNew?: boolean;
}

export interface Page<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
  };
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
  size: number;
  number: number;
  empty: boolean;
}
