# Doctolia — Frontend Web (`rdv_medic_front`)

Angular 20.2 · TypeScript 5.9 · Node.js v24 · Standalone Components

## Lancer l'application

```bash
cd rdv_medic_front
npm install
ng serve
# → http://localhost:4200
```

> L'API Spring Boot doit tourner sur `http://localhost:9000`.

Build :
```bash
ng build
```

Tests :
```bash
ng test
```

---

## Architecture des dossiers

```
src/app/
├── component/
│   ├── doctor/     → Liste des médecins disponibles
│   ├── login/      → Page de connexion
│   └── slot/       → Calendrier hebdomadaire + modal de réservation
├── guards/         → Protection des routes (authGuard)
├── interceptors/   → Injection automatique du header JWT
├── model/          → Interfaces TypeScript (Doctor, Patient, Slot, AuthResponse)
├── services/       → Appels HTTP centralisés (ApiService, AuthService)
├── app.ts          → Composant racine avec navbar et bouton déconnexion
├── app.html        → Template racine
├── app.routes.ts   → 3 routes applicatives
└── app.config.ts   → Providers globaux (router, HttpClient + interceptors)
```

---

## Comptes de démo

| Rôle | Username | Mot de passe |
|------|----------|--------------|
| Patient | `pat.marc` | `password` |
| Patient | `pat.jean` | `password` |
| Médecin | `doc.john` | `password` |
| Médecin | `doc.paul` | `password` |

---

## Fichiers complexes — Résumés

### `services/auth.service.ts`
Service central de l'authentification. Point de vérité unique sur l'état de connexion dans toute l'application.

Mécanisme :
- **`BehaviorSubject<AuthResponse | null>`** : flux réactif qui émet la valeur courante immédiatement à chaque abonné. Initialisé depuis `localStorage` au démarrage — l'état de connexion **survit au rechargement de page**.
- **`login()`** : appelle `POST /login`, stocke le token dans `localStorage['jwt_token']` et l'objet utilisateur dans `localStorage['current_user']`, puis met à jour le subject.
- **`logout()`** : vide le localStorage et émet `null` → tous les composants abonnés réagissent instantanément.
- **`currentUser`** (getter synchrone) : utilisé dans les templates pour afficher le nom de l'utilisateur.
- **`isAuthenticated()`** : vérifie la présence du token — utilisé par `authGuard`.

```typescript
// Depuis n'importe quel composant ou template
this.authService.currentUser?.firstName
```

---

### `interceptors/auth.interceptor.ts`
Intercepteur HTTP fonctionnel (pattern Angular 15+, sans classe). Intercepte **chaque requête HTTP** sortante et y ajoute automatiquement `Authorization: Bearer <token>` si un token est présent.

```typescript
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = inject(AuthService).getToken();
  if (token) return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
  return next(req);
};
```

Enregistré dans `app.config.ts` via `provideHttpClient(withInterceptors([authInterceptor]))`. Grâce à cet intercepteur, `ApiService` n'a jamais besoin de gérer les headers manuellement.

---

### `guards/auth.guard.ts`
Guard fonctionnel (`CanActivateFn`) qui protège les routes privées. Si l'utilisateur n'est pas authentifié, redirige vers `/login` via `Router.createUrlTree()`.

```typescript
export const authGuard: CanActivateFn = () => {
  if (inject(AuthService).isAuthenticated()) return true;
  return inject(Router).createUrlTree(['/login']);
};
```

Appliqué sur les routes `/` (liste médecins) et `/doctor-slots/:id` (créneaux).

---

### `app.config.ts`
Point de configuration globale (remplace `AppModule` en mode standalone). Déclare tous les providers applicatifs :
- `provideRouter(routes)` → active le routeur.
- `provideHttpClient(withInterceptors([authInterceptor]))` → active `HttpClient` avec l'intercepteur JWT branché.
- C'est ici qu'on ajoutera les futurs providers (state management, i18n, etc.).

---

### `app.routes.ts`
Définit les 3 routes de l'application :

| Path | Composant | Accès |
|------|-----------|-------|
| `/login` | `LoginComponent` | Public |
| `/` | `DoctorListComponent` | `authGuard` |
| `/doctor-slots/:id` | `SlotListComponent` | `authGuard` |

---

### `component/login/login.component.ts`
Page de connexion. Fonctionnement :
1. L'utilisateur saisit username et mot de passe.
2. `submit()` appelle `authService.login()` et affiche un état `loading`.
3. En cas de succès → navigation vers `/`.
4. En cas d'erreur → message affiché, `loading` remis à `false`.

Pattern `[value]="..." (input)="... = $any($event.target).value"` — cohérent avec le reste du projet, pas de dépendance à `FormsModule`.

---

### `component/slot/slot-list.component.ts`
Composant le plus complexe. Affiche un **calendrier hebdomadaire** pour un médecin et gère la réservation de créneaux.

Logique principale :
- **Calendrier** : `generateWeekDays()` produit 5 dates (lundi→vendredi) depuis `currentWeekStart`. `generateHours()` produit 22 créneaux de 08:00 à 18:30 (pas de 30 min).
- **Navigation** : `previousWeek()` / `nextWeek()` décalent de ±7 jours et rechargent les créneaux via l'API.
- **`isSlotTaken(date, time)`** : compare date ISO et heure (5 premiers chars) pour marquer les cases réservées.
- **`isLunchBreak(time)`** : désactive 12:00, 12:30, 13:00.
- **Modal de réservation** : `submitForm()` calcule `endTime` (+30 min), récupère `patientId` depuis `authService.currentUser!.id`, poste via `ApiService`, fermeture automatique après 2 secondes.

---

### `services/api.service.ts`
Centralise tous les appels HTTP vers `http://localhost:9000`. Chaque méthode retourne un `Observable` typé. Les headers JWT sont injectés automatiquement par `authInterceptor`.

| Méthode | HTTP | Endpoint |
|---------|------|----------|
| `getDoctors()` | GET | `/doctors` |
| `getDoctorById(id)` | GET | `/doctor/:id` |
| `getSlotsByDoctors(id)` | GET | `/doctors/:id/slots` |
| `getSlotsByDoctorsAndPatient(dId, pId)` | GET | `/slots/:dId/:pId` |
| `postNewSlot(data)` | POST | `/slot/:doctorId/:patientId` |
| ~~`postPatient(data)`~~ | POST | `/patient` — dead code, non utilisée |

---

### `model/auth.model.ts`
Interface TypeScript miroir du `AuthResponse` Java :

```typescript
export interface AuthResponse {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  token: string;
  expiresIn: number;
  roles: string[];
}
```

---

## Flux d'authentification

```
[Utilisateur] → /login
              → AuthService.login()
              → POST /login
              ← { token, id, firstName, roles, ... }
              → localStorage + BehaviorSubject
              → navigate('/')

[Requête API] → authInterceptor ajoute Authorization: Bearer <token>
              → backend valide le JWT
              ← données protégées
```
