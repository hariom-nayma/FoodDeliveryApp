import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-pending-verification',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './pending-verification.component.html',
  styles: [`
    .container { max-width: 600px; margin: 4rem auto; text-align: center; padding: 2rem; }
    img { max-width: 100%; height: auto; margin-bottom: 2rem; }
    h2 { color: #333; margin-bottom: 1rem; }
    p { color: #666; margin-bottom: 2rem; line-height: 1.6; }
    .btn { background: var(--primary-color); color: white; padding: 0.75rem 1.5rem; text-decoration: none; border-radius: 4px; display: inline-block; }
  `]
})
export class PendingVerificationComponent {}
