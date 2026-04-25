import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuditDashboardComponent } from './components/audit-dashboard.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, AuditDashboardComponent],
  template: `<app-audit-dashboard></app-audit-dashboard>`
})
export class AppComponent {
  title = 'auditvault-ui';
}
