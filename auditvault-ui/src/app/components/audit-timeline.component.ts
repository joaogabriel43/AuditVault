import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuditEvent } from '../models/audit.model';

@Component({
  selector: 'app-audit-timeline',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="relative border-l border-gray-200 ml-3">
      @for (event of events; track event.id; let i = $index) {
        <div class="mb-10 ml-6 relative group cursor-pointer p-2 rounded-lg transition-colors hover:bg-gray-50" [class.animate-pulse]="event.isNew" [class.bg-teal-50]="event.isNew" (click)="onSelect.emit(event)">
          <span class="absolute flex items-center justify-center w-6 h-6 bg-blue-100 rounded-full -left-9 ring-8 ring-white" [class.bg-teal-200]="event.isNew">
             <div class="w-2.5 h-2.5 bg-blue-600 rounded-full" [class.bg-teal-600]="event.isNew"></div>
          </span>
          <h3 class="flex items-center mb-1 text-lg font-semibold text-gray-900">
            {{ event.eventType }}
            @if(i === 0) {
              <span class="bg-blue-100 text-blue-800 text-sm font-medium mr-2 px-2.5 py-0.5 rounded ml-3">Latest</span>
            }
          </h3>
          <time class="block mb-2 text-sm font-normal leading-none text-gray-400">{{ event.timestamp | date:'medium' }}</time>
          <p class="text-base font-normal text-gray-500">User: {{ event.userId || 'System' }}</p>
        </div>
      } @empty {
        <div class="ml-6 text-gray-500 py-4">No events found for this aggregate.</div>
      }
    </div>
  `
})
export class AuditTimelineComponent {
  @Input({ required: true }) events: AuditEvent[] = [];
  @Output() onSelect = new EventEmitter<AuditEvent>();
}
