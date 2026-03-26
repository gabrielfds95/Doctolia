import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Intercepteur HTTP de gestion des erreurs.
 *
 * Rôle principal : détecter les réponses 401 Unauthorized du backend
 * et déconnecter automatiquement l'utilisateur + rediriger vers /login.
 *
 * Cas couverts par le 401 :
 *  - Token JWT expiré (le backend rejette la requête)
 *  - Token falsifié ou invalide
 *  - Utilisateur supprimé en base entre deux requêtes
 *
 * Sans cet intercepteur, l'utilisateur verrait une erreur silencieuse et
 * resterait bloqué sur la page sans comprendre pourquoi les données ne chargent plus.
 *
 * pipe(catchError(...)) : opérateur RxJS qui intercepte les erreurs dans le flux
 * de l'Observable sans interrompre les autres requêtes réussies.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {

      // Vérifie que l'erreur vient d'une route protégée, pas de /login ou /register
      // → évite une boucle infinie : si /login renvoie 401 (mauvais credentials),
      //   on NE redirige PAS vers /login (on laisse le composant gérer l'erreur)
      const isAuthEndpoint = req.url.endsWith('/login') || req.url.endsWith('/register');

      if (error.status === 401 && !isAuthEndpoint) {
        // Token expiré ou invalide → on nettoie la session et on redirige
        inject(AuthService).logout();           // supprime token + user du localStorage
        inject(Router).navigate(['/login']);    // redirige vers la page de connexion
      }

      // Propage l'erreur vers le composant qui a fait la requête
      // → il pourra afficher un message d'erreur spécifique si besoin
      return throwError(() => error);
    })
  );
};
