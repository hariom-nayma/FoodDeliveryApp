import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { HeaderComponent } from './core/header/header.component';
import { FooterComponent } from './core/footer/footer.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, HeaderComponent, FooterComponent],
  template: `
    <app-header></app-header>
    <main style="min-height: 80vh;">
      <router-outlet></router-outlet>
    </main>
    <app-footer></app-footer>
  `,
  styles: []
})
export class AppComponent {
  title = 'fooddelivery-frontend';
}
