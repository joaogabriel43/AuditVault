import { Component, Input, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-json-diff-viewer',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="bg-gray-900 text-green-400 p-4 rounded-lg overflow-x-auto shadow-inner text-sm font-mono whitespace-pre-wrap">
      @if (parsedJson()) {
        {{ parsedJson() | json }}
      } @else {
        <span class="text-gray-500 italic">No valid JSON payload available.</span>
      }
    </div>
  `
})
export class JsonDiffViewerComponent {
  
  private _payload = signal<string>('');
  
  @Input() set payload(value: string) {
    this._payload.set(value);
  }

  parsedJson = computed(() => {
    const raw = this._payload();
    if (!raw) return null;
    try {
      return JSON.parse(raw);
    } catch {
      return raw;
    }
  });
}
