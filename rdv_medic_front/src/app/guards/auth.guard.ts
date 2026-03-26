import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Guard de protection des routes Angular.
 *
 * Rôle : empêcher l'accès à une route si l'utilisateur n'est pas connecté
 * (ou si son token JWT est expiré), et le rediriger vers /login.
 *
 * Utilisé dans app.routes.ts sur toutes les routes protégées :
 *   { path: 'mes-rdv', component: MesRdvComponent, canActivate: [authGuard] }
 *
 * CanActivateFn : format fonctionnel Angular 17+.
 * Appelé automatiquement par le Router AVANT de charger le composant.
 *
 * Flux :
 *   Utilisateur navigue vers /mes-rdv
 *     → Router appelle authGuard()
 *       → isAuthenticated() vérifie le token (présence + expiration)
 *         → true  : navigation autorisée → MesRdvComponent chargé
 *         → false : redirection vers /login (createUrlTree retourne une URL)
 *
 * Note : cette vérification est côté CLIENT uniquement.
 * Même si quelqu'un contournait le guard, le backend rejetterait
 * la requête avec 401 (JwtAuthenticationFilter) → double protection.
 */
export const authGuard: CanActivateFn = () => {
  if (inject(AuthService).isAuthenticated()) {
    return true; // autorise la navigation
  }
  // Retourne une UrlTree → Angular effectue la redirection proprement
  // (plus propre que router.navigate() qui retournerait false)
  return inject(Router).createUrlTree(['/login']);
};
