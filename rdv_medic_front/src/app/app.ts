//composant racine
//fournis le router-outlet la où tous les composants de pages s’afficheront.

// Importation des décorateurs et outils Angular
import { Component } from '@angular/core';
// Importation du composant de routage (utile si tu utilises des routes)
import { RouterOutlet, RouterModule } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterModule], 
  templateUrl: './app.html',
  styleUrls: ['./app.scss']
})
export class App {}