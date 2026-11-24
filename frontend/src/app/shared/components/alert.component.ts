import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-alert',
  template: `<p *ngIf="message" class="state" [ngClass]="type">{{ message }}</p>`
})
export class AlertComponent {
  @Input() message = '';
  @Input() type: 'error' | 'success' | 'info' = 'info';
}
