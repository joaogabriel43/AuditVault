import { Component, Input, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-json-diff-viewer',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="bg-gray-900 text-green-400 p-5 rounded-xl border border-gray-700 shadow-[inset_0_2px_4px_rgba(0,0,0,0.4)] text-sm font-mono whitespace-pre-wrap transition-colors hover:border-gray-600">
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
