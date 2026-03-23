import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [RouterModule],
  template: `
    <div class="not-found">
      <h1>404</h1>
      <p>Cette page n'existe pas.</p>
      <a routerLink="/">Retour à l'accueil</a>
    </div>
  `,
  styles: [`
    .not-found {
      min-height: calc(100vh - 60px);
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      text-align: center;
      gap: 1rem;
    }
    h1 { font-size: 6rem; font-weight: 700; color: #2563eb; margin: 0; line-height: 1; }
    p { color: #6b7280; font-size: 1.1rem; margin: 0; }
    a { color: #2563eb; font-weight: 600; text-decoration: none; }
    a:hover { text-decoration: underline; }
  `]
})
export class NotFoundComponent {}
