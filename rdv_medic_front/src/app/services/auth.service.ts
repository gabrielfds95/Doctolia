import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { AuthResponse } from '../model/auth.model';
import { RegisterRequest } from '../model/register.model';

/**
 * Service central d'authentification côté Angular.
 *
 * Responsabilités :
 *  1. Appeler les endpoints /login et /register du backend
 *  2. Persister le token JWT et les infos utilisateur dans localStorage
 *  3. Exposer l'état de connexion sous forme d'Observable (currentUser$)
 *     → les composants peuvent réagir en temps réel aux changements de session
 *  4. Vérifier si le token est encore valide (non expiré)
 *  5. Nettoyer la session à la déconnexion
 *
 * @Injectable({ providedIn: 'root' }) → service singleton : une seule instance
 * partagée dans toute l'application Angular.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly BASE_URL = 'http://localhost:9000';

  // Clés utilisées pour persister les données dans localStorage
  private readonly TOKEN_KEY = 'jwt_token';
  private readonly USER_KEY  = 'current_user';

  /**
   * BehaviorSubject : un Observable qui mémorise sa DERNIÈRE valeur.
   * Avantage : tout composant qui s'y abonne reçoit immédiatement la valeur actuelle,
   * même s'il s'abonne après que la valeur ait été émise.
   *
   * Initialisé avec l'utilisateur stocké en localStorage (persistance entre recharges).
   */
  private currentUserSubject = new BehaviorSubject<AuthResponse | null>(this.loadUser());

  /** Observable public : les composants s'y abonnent pour réagir aux changements */
  currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {}

  /**
   * Envoie les credentials au backend et sauvegarde la session si succès.
   *
   * pipe(tap(...)) : effectue un effet de bord (sauvegarde session) sans modifier
   * la valeur de l'Observable → le composant recevra toujours l'AuthResponse brute.
   *
   * @returns Observable<AuthResponse> : le composant subscribe et navigue vers '/'
   */
  login(username: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.BASE_URL}/login`, { username, password }).pipe(
      tap(response => this.saveSession(response)) // sauvegarde token + user si 200 OK
    );
  }

  /**
   * Envoie les données d'inscription au backend.
   *
   * Cas médecin : le backend renvoie enabled = false (compte en attente).
   * Dans ce cas on ne sauvegarde PAS la session — le médecin ne peut pas se connecter.
   * Le composant Register affichera un message d'attente de validation.
   */
  register(data: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.BASE_URL}/register`, data).pipe(
      tap(response => {
        // Ne pas sauvegarder la session si le compte est désactivé (médecin en attente)
        if (response.enabled !== false) {
          this.saveSession(response);
        }
      })
    );
  }

  /**
   * Vérifie si l'utilisateur possède un rôle donné.
   * Exemple : hasRole('ROLE_ADMIN') → true si l'utilisateur est admin.
   */
  hasRole(role: string): boolean {
    return this.currentUser?.roles?.includes(role) ?? false;
  }

  // Raccourcis pratiques utilisés dans les Guards et dans app.html
  isAdmin(): boolean   { return this.hasRole('ROLE_ADMIN'); }
  isDoctor(): boolean  { return this.hasRole('ROLE_DOCTOR'); }
  isPatient(): boolean { return this.hasRole('ROLE_PATIENT'); }

  /**
   * Sauvegarde le token et l'objet utilisateur dans localStorage.
   * localStorage survit aux rechargements de page (≠ sessionStorage).
   * Met aussi à jour le BehaviorSubject → tous les abonnés sont notifiés.
   */
  private saveSession(response: AuthResponse): void {
    localStorage.setItem(this.TOKEN_KEY, response.token);
    localStorage.setItem(this.USER_KEY, JSON.stringify(response));
    this.currentUserSubject.next(response); // notifie tous les abonnés à currentUser$
  }

  /**
   * Déconnexion : supprime les données de localStorage et émet null.
   * Les Guards détecteront que currentUser est null → redirection /login.
   */
  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.currentUserSubject.next(null); // notifie : plus d'utilisateur connecté
  }

  /** Récupère le token brut depuis localStorage (utilisé par authInterceptor). */
  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  /**
   * Vérifie si le token JWT est présent ET non expiré côté client.
   *
   * Mécanisme :
   *  1. Récupère le token depuis localStorage
   *  2. Découpe le JWT en 3 parties (header.payload.signature)
   *  3. Décode la partie payload (base64url → JSON)
   *  4. Lit le claim "exp" (timestamp Unix en secondes)
   *  5. Compare avec Date.now() (en millisecondes)
   *
   * Important : cette vérification est côté CLIENT uniquement.
   * Le backend valide indépendamment avec JwtAuthenticationFilter.
   * C'est utilisé par authGuard pour éviter des redirections inutiles.
   */
  isAuthenticated(): boolean {
    const token = this.getToken();
    if (!token) return false;
    try {
      // JWT format : "xxxxx.yyyyy.zzzzz"
      // La partie [1] est le payload encodé en base64url
      const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      const payload = JSON.parse(atob(base64)); // atob() décode le base64
      return payload.exp * 1000 > Date.now();   // exp est en secondes, Date.now() en ms
    } catch {
      return false; // token malformé
    }
  }

  /**
   * Accès synchrone à l'utilisateur courant (valeur instantanée du BehaviorSubject).
   * Utilisé dans les composants qui n'ont pas besoin de réactivité.
   */
  get currentUser(): AuthResponse | null {
    return this.currentUserSubject.value;
  }

  /**
   * Charge l'utilisateur depuis localStorage au démarrage de l'app.
   * Appelé une seule fois dans l'initialisation du BehaviorSubject.
   */
  private loadUser(): AuthResponse | null {
    const json = localStorage.getItem(this.USER_KEY);
    return json ? JSON.parse(json) : null;
  }
}
