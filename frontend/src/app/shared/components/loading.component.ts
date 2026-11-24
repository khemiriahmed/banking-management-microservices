import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-loading',
  template: `<p class="state loading" *ngIf="show">{{ text }}</p>`
})
export class LoadingComponent {
  @Input() show = false;
  @Input() text = 'Chargement...';
}
