import { Component, signal, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuditService } from '../services/audit.service';
import { AuditEvent } from '../models/audit.model';
import { AuditTimelineComponent } from './audit-timeline.component';
import { JsonDiffViewerComponent } from './json-diff-viewer.component';
import { Subscription } from 'rxjs';

interface UiAuditEvent extends AuditEvent {
  isNew?: boolean;
}

@Component({
  selector: 'app-audit-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, AuditTimelineComponent, JsonDiffViewerComponent],
  template: `
    <div class="max-w-7xl mx-auto p-6">
      <header class="mb-8 flex justify-between items-end">
        <div>
          <h1 class="text-3xl font-bold text-gray-900 tracking-tight flex items-center gap-3">
            AuditVault Dashboard
            <span class="inline-flex items-center bg-green-100 text-green-800 text-xs font-medium px-2.5 py-0.5 rounded-full">
              <span class="w-2 h-2 mr-1 bg-green-500 rounded-full animate-pulse"></span>
              Live SSE Active
            </span>
          </h1>
          <p class="text-gray-500 mt-2">Track event history, full-text search, and real-time streaming.</p>
        </div>
        
        <div class="flex gap-4">
          <button (click)="viewState()" class="px-4 py-2 bg-indigo-50 text-indigo-700 font-medium rounded-md hover:bg-indigo-100 transition shadow-sm">
            View Consolidated State
          </button>
          
          <button (click)="exportPdf()" [disabled]="isExporting()" class="px-4 py-2 bg-indigo-600 text-white font-medium rounded-md hover:bg-indigo-700 transition shadow-sm disabled:opacity-50 flex items-center gap-2">
            @if(isExporting()) {
              <svg class="animate-spin h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              Generating PDF...
            } @else {
              Export PDF Report
            }
          </button>
        </div>
      </header>

      <div class="bg-white p-6 rounded-xl shadow-sm border border-gray-100 mb-8 grid grid-cols-1 md:grid-cols-2 gap-6">
        <div>
          <label for="aggId" class="block text-sm font-medium text-gray-700 mb-2">Aggregate ID Search</label>
          <div class="flex gap-3">
            <input id="aggId" type="text" [(ngModel)]="aggregateId" placeholder="e.g. agg-123" class="flex-1 rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm px-4 py-2 border">
            <button (click)="searchEvents()" class="px-6 py-2 bg-gray-900 text-white font-medium rounded-md hover:bg-gray-800 transition">Fetch</button>
          </div>
        </div>
        <div>
          <label for="globalSearch" class="block text-sm font-medium text-gray-700 mb-2">Global Elasticsearch</label>
          <div class="flex gap-3">
            <input id="globalSearch" type="text" [(ngModel)]="searchQuery" placeholder="Search payloads..." class="flex-1 rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm px-4 py-2 border">
            <button (click)="executeGlobalSearch()" class="px-6 py-2 bg-teal-600 text-white font-medium rounded-md hover:bg-teal-700 transition">Search</button>
          </div>
        </div>
      </div>
      
      @if(downloadUrl()) {
        <div class="mb-8 p-4 bg-green-50 border border-green-200 rounded-lg flex items-center justify-between">
          <span class="text-green-800">Your export is ready!</span>
          <a [href]="downloadUrl()" target="_blank" class="px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700 font-medium shadow-sm transition">Download PDF</a>
        </div>
      }

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-8">
        <section>
          <h2 class="text-xl font-bold text-gray-800 mb-6 flex justify-between">
            <span>Event Timeline</span>
            <span class="text-sm font-normal text-gray-500">{{ events().length }} events loaded</span>
          </h2>
          @if(isLoading()) {
            <div class="animate-pulse flex space-x-4">
              <div class="flex-1 space-y-4 py-1">
                <div class="h-4 bg-gray-200 rounded w-3/4"></div>
                <div class="space-y-2">
                  <div class="h-4 bg-gray-200 rounded"></div>
                  <div class="h-4 bg-gray-200 rounded w-5/6"></div>
                </div>
              </div>
            </div>
          } @else {
            <app-audit-timeline [events]="events()" (onSelect)="selectedEvent.set($event)"></app-audit-timeline>
          }
        </section>

        <section class="lg:sticky lg:top-6 lg:self-start">
           <h2 class="text-xl font-bold text-gray-800 mb-6">
             @if(isShowingState()) {
                Consolidated State (CQRS)
             } @else {
                Event Payload Diff
             }
           </h2>
           @if(isShowingState()) {
             <app-json-diff-viewer [payload]="consolidatedState()"></app-json-diff-viewer>
           } @else if(selectedEvent()) {
             <app-json-diff-viewer [payload]="selectedEvent()?.payload || ''"></app-json-diff-viewer>
             <div class="mt-4 p-4 bg-gray-50 rounded-lg text-sm text-gray-600 border border-gray-100">
               <strong>Event ID:</strong> {{ selectedEvent()?.id }}<br>
               <strong>Type:</strong> {{ selectedEvent()?.aggregateType }}<br>
               <strong>Obfuscated:</strong> <span [class.text-green-600]="selectedEvent()?.obfuscated" [class.text-red-600]="!selectedEvent()?.obfuscated">{{ selectedEvent()?.obfuscated }}</span>
             </div>
           } @else {
             <div class="p-8 border-2 border-dashed border-gray-200 rounded-xl text-center text-gray-500 bg-gray-50">
               Select an event from the timeline to view its mutation payload.
             </div>
           }
        </section>
      </div>
    </div>
  `
})
export class AuditDashboardComponent implements OnInit, OnDestroy {
  
  private auditService = inject(AuditService);
  private sseSubscription?: Subscription;
  
  aggregateId = signal<string>('');
  searchQuery = signal<string>('');
  events = signal<UiAuditEvent[]>([]);
  selectedEvent = signal<UiAuditEvent | null>(null);
  
  isLoading = signal<boolean>(false);
  isExporting = signal<boolean>(false);
  
  isShowingState = signal<boolean>(false);
  consolidatedState = signal<string>('');
  downloadUrl = signal<string | null>(null);

  ngOnInit() {
    this.sseSubscription = this.auditService.connectToAuditStream().subscribe({
      next: (event) => {
        // Add new event to top of timeline
        const uiEvent: UiAuditEvent = { ...event, isNew: true };
        this.events.update(current => [uiEvent, ...current]);
        
        // Remove animation after a few seconds
        setTimeout(() => {
          this.events.update(current => {
             const index = current.findIndex(e => e.id === uiEvent.id);
             if(index > -1) {
                const arr = [...current];
                arr[index] = { ...arr[index], isNew: false };
                return arr;
             }
             return current;
          });
        }, 3000);
      }
    });
  }

  ngOnDestroy() {
    if (this.sseSubscription) {
      this.sseSubscription.unsubscribe();
    }
  }

  searchEvents() {
    if (!this.aggregateId()) return;
    this.isLoading.set(true);
    this.isShowingState.set(false);
    this.downloadUrl.set(null);
    this.searchQuery.set('');
    
    this.auditService.getEvents(this.aggregateId())
      .subscribe({
        next: (page) => {
          this.events.set(page.content);
          this.isLoading.set(false);
          this.selectedEvent.set(page.content.length > 0 ? page.content[0] : null);
        },
        error: (err) => {
          console.error('Failed to load events', err);
          this.isLoading.set(false);
        }
      });
  }

  executeGlobalSearch() {
    if (!this.searchQuery()) return;
    this.isLoading.set(true);
    this.isShowingState.set(false);
    this.downloadUrl.set(null);
    this.aggregateId.set('');
    
    this.auditService.search(this.searchQuery())
      .subscribe({
        next: (page) => {
          this.events.set(page.content);
          this.isLoading.set(false);
          this.selectedEvent.set(page.content.length > 0 ? page.content[0] : null);
        },
        error: (err) => {
          console.error('Failed to search events', err);
          this.isLoading.set(false);
        }
      });
  }

  viewState() {
    if (!this.aggregateId()) {
      alert('Please enter an Aggregate ID to view its state.');
      return;
    }
    this.isLoading.set(true);
    
    this.auditService.getConsolidatedState(this.aggregateId())
      .subscribe({
        next: (state) => {
          this.consolidatedState.set(JSON.stringify(state, null, 2));
          this.isShowingState.set(true);
          this.isLoading.set(false);
        },
        error: (err) => {
          console.error('Failed to load state', err);
          this.isLoading.set(false);
        }
      });
  }

  exportPdf() {
    if (!this.aggregateId()) {
      alert('Please enter an Aggregate ID to export.');
      return;
    }
    this.isExporting.set(true);
    this.downloadUrl.set(null);
    
    this.auditService.triggerExport(this.aggregateId())
      .subscribe({
        next: (res) => {
          setTimeout(() => {
            this.downloadUrl.set(`/api/audit/export/download/${res.jobExecutionId}`);
            this.isExporting.set(false);
          }, 3000);
        },
        error: (err) => {
          console.error('Failed to trigger export', err);
          this.isExporting.set(false);
        }
      });
  }
}
