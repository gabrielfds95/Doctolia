import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  if (auth.isAuthenticated() && auth.isAdmin()) {
    return true;
  }
  // Ne vider la session que si le token est réellement invalide/expiré —
  // un utilisateur connecté mais non-admin ne doit pas être déconnecté,
  // juste renvoyé loin d'une route qui ne le concerne pas.
  if (!auth.isAuthenticated()) {
    auth.logout();
  }
  return inject(Router).createUrlTree(['/login']);
};
