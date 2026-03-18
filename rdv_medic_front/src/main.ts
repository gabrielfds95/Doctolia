//point d'entree de l'app

// Fonction pour démarrer l’application Angular
import { bootstrapApplication } from '@angular/platform-browser';
// Configuration de l’application (fichier séparé)
import { appConfig } from './app/app.config';
// Composant racine
import { App } from './app/app';
import { provideRouter } from '@angular/router';
import { routes } from './app/app.routes';

// Démarrage de l’application avec le composant racine et la config
bootstrapApplication(App, {
  ...appConfig,       // inclut tes autres options (ex: imports, providers, etc.)
  providers: [
    ...(appConfig.providers || []), // si appConfig contient déjà des providers
    provideRouter(routes)           // ajoute le router
  ]
}).catch(err => console.error(err));