import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuditEvent } from '../models/audit.model';

@Component({
  selector: 'app-audit-timeline',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="relative border-l border-gray-200 dark:border-gray-700 ml-3 transition-colors">
      @for (event of events; track event.id; let i = $index) {
        <div class="mb-10 ml-6 relative group cursor-pointer p-3 -mt-3 rounded-xl transition-all duration-300 hover:bg-gray-50 dark:hover:bg-gray-800/50 hover:shadow-sm" 
             [class.animate-[fadeIn_0.5s_ease-out]]="event.isNew" 
             [class.bg-teal-50]="event.isNew" 
             [class.dark:bg-teal-900/20]="event.isNew" 
             (click)="onSelect.emit(event)">
          <span class="absolute flex items-center justify-center w-6 h-6 bg-blue-100 dark:bg-blue-900/50 rounded-full -left-[45px] ring-8 ring-white dark:ring-gray-900 transition-colors group-hover:scale-110 group-hover:ring-gray-50 dark:group-hover:ring-gray-800" 
                [class.bg-teal-200]="event.isNew" 
                [class.dark:bg-teal-700]="event.isNew">
             <div class="w-2.5 h-2.5 bg-blue-600 dark:bg-blue-400 rounded-full transition-colors" 
                  [class.bg-teal-600]="event.isNew" 
                  [class.dark:bg-teal-400]="event.isNew" 
                  [class.animate-pulse]="event.isNew"></div>
          </span>
          <h3 class="flex items-center mb-1 text-lg font-semibold text-gray-900 dark:text-gray-100 transition-colors group-hover:text-blue-600 dark:group-hover:text-blue-400">
            {{ event.eventType }}
            @if(i === 0) {
              <span class="bg-blue-100 dark:bg-blue-900/40 text-blue-800 dark:text-blue-300 text-xs font-medium mr-2 px-2.5 py-0.5 rounded-full ml-3 border border-blue-200 dark:border-blue-800">Latest</span>
            }
          </h3>
          <time class="block mb-2 text-sm font-normal leading-none text-gray-400 dark:text-gray-500 transition-colors">{{ event.timestamp | date:'medium' }}</time>
          <p class="text-base font-normal text-gray-500 dark:text-gray-400 transition-colors">User: <span class="font-medium text-gray-700 dark:text-gray-300">{{ event.userId || 'System' }}</span></p>
        </div>
      } @empty {
        <div class="ml-6 text-gray-500 dark:text-gray-400 py-4 transition-colors">No events found for this aggregate.</div>
      }
    </div>
  `
})
export class AuditTimelineComponent {
  @Input({ required: true }) events: AuditEvent[] = [];
  @Output() onSelect = new EventEmitter<AuditEvent>();
}
