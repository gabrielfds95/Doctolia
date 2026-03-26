import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

/**
 * Intercepteur HTTP d'authentification.
 *
 * Rôle : injecter automatiquement le token JWT dans le header Authorization
 * de CHAQUE requête HTTP sortante, sans avoir à le faire manuellement dans
 * chaque appel de ApiService.
 *
 * Schéma du flux :
 *   Composant → ApiService.getDoctors()
 *     → HttpClient.get('/doctors')
 *       → authInterceptor (ici)
 *         → requête clonée avec header "Authorization: Bearer <token>"
 *           → Backend Spring Boot
 *             → JwtAuthenticationFilter valide le token
 *
 * HttpInterceptorFn : format fonctionnel (Angular 17+), alternative plus légère
 * aux classes qui implémentent HttpInterceptor.
 * inject() permet d'utiliser l'injection de dépendances dans une fonction pure.
 *
 * Enregistré dans app.config.ts via provideHttpClient(withInterceptors([authInterceptor]))
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // Récupère le token JWT depuis localStorage via AuthService
  const token = inject(AuthService).getToken();

  if (token) {
    // req est immuable → on doit le CLONER pour modifier ses headers
    // setHeaders ajoute ou écrase le header Authorization
    return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
  }

  // Pas de token (utilisateur non connecté) → la requête part sans header
  // (pour les endpoints publics comme /doctors, /login)
  return next(req);
};
