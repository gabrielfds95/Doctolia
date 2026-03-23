# Doctolia

Application de prise de rendez-vous médicaux (type Doctolib), développée en monorepo.

- **Backend** : Java 17 + Spring Boot 3.5.4 → [`rdv-api/`](rdv-api/README.md)
- **Frontend Web** : Angular 20.2 → [`rdv_medic_front/`](rdv_medic_front/README.md)
- **Frontend Mobile** : React Native (Expo) → [`doctolia-mobile/`](doctolia-mobile/)

---

## Ce qui fonctionne aujourd'hui

| Fonctionnalité | Statut |
|----------------|--------|
| Connexion (JWT) | Fait |
| Inscription patient (page Angular + endpoint `/register`) | Fait |
| Liste des médecins | Fait |
| Calendrier hebdomadaire des créneaux | Fait |
| Réservation d'un créneau (portail patient) | Fait |
| Protection des routes Angular (authGuard) | Fait |
| Injection automatique du token JWT | Fait |
| Déconnexion automatique sur expiration (intercepteur 401) | Fait |
| Validation côté Angular (login + inscription) | Fait |
| Vérification d'expiration du JWT côté Angular | Fait |
| Page 404 | Fait |
| Pattern DTO (pas de mots de passe exposés) | Fait |
| Gestionnaire d'erreurs global (`@ControllerAdvice`) | Fait |
| Données de démo au démarrage | Fait |

---

## Améliorations réalisées (base mobile prête)

Ces 6 points ont tous été implémentés.

| Amélioration | Fichiers |
|---|---|
| Page d'inscription Angular | `register.component.ts/html/scss`, `register.model.ts`, `AuthService.register()` |
| Intercepteur erreur 401 | `error.interceptor.ts`, enregistré dans `app.config.ts` |
| Validation des formulaires | `login.component.ts` + `register.component.ts` — messages inline par champ |
| Suppression du code mort | `postPatient()` retiré de `api.service.ts` |
| Page 404 | `not-found.component.ts` (template inline), route `**` dans `app.routes.ts` |
| Vérification expiration JWT | `isAuthenticated()` décode le claim `exp` du token via `atob()` |

---

## Tâches futures — Feuille de route

### Inscription patient (Angular) — Fait
- [x] `RegisterComponent` avec formulaire complet + validation
- [x] Route `/register` publique
- [x] Lien "S'inscrire" depuis la page login
- [x] Connexion automatique après inscription

---

### Portail Médecin
- [ ] Page de profil médecin (ses informations)
- [ ] Vue de son planning hebdomadaire (créneaux réservés vs libres)
- [ ] Créer / supprimer des créneaux disponibles
- [ ] Voir les informations du patient pour chaque RDV
- [ ] Changer le statut d'un RDV (confirmé, annulé, terminé)

**Backend à faire :**
- Endpoints `POST /doctors/{id}/slots`, `DELETE /slots/{id}`, `PUT /slots/{id}`
- `SecurityConfig` : réserver ces endpoints à `ROLE_DOCTOR`

---

### Portail Patient
- [ ] Page "Mes rendez-vous" (liste des RDV passés et à venir)
- [ ] Annuler un rendez-vous
- [ ] Voir le détail d'un RDV (médecin, motif, date)
- [ ] Modifier le motif d'un RDV

**Backend à faire :**
- `GET /patients/me/slots` → créneaux du patient connecté (id extrait du JWT)
- `DELETE /slots/{id}` → annulation (vérifier que l'id patient correspond)

---

### Inscription Médecin + Validation Super Admin
- [ ] Formulaire d'inscription médecin (champs différents : spécialité, numéro de licence, département)
- [ ] À l'inscription, `enabled = false` → le médecin ne peut pas se connecter
- [ ] Rôle `ROLE_ADMIN` créé avec un compte `superadmin`
- [ ] Interface admin : liste des médecins en attente, bouton "Accepter / Refuser"
- [ ] À l'acceptation : `enabled = true` → le médecin peut se connecter

**Backend à faire :**
- `PUT /admin/doctors/{id}/approve` → `hasRole("ADMIN")`
- `GET /admin/doctors/pending` → liste des médecins avec `enabled = false`

---

### Gestion de Documents Patient
- [ ] Upload de PDF/images (ordonnances, résultats)
- [ ] Liste des documents par patient
- [ ] Téléchargement sécurisé (uniquement par le patient concerné)
- [ ] Suppression

**Backend :** l'entité `Document` est déjà modélisée. À implémenter : `DocumentController`, stockage fichiers (dossier local ou S3).

---

### Chat autour d'un RDV
- [ ] Fil de messages attaché à un `Slot` spécifique
- [ ] Médecin et patient peuvent échanger avant/après le RDV
- [ ] Notifications (badge non lu)

**Backend à faire :**
- Entité `Message` (slotId, senderId, content, sentAt)
- Endpoints REST `GET/POST /slots/{id}/messages`
- (Optionnel) WebSocket pour le temps réel

---

### Application Mobile (React Native) — Base faite

Structure dans `doctolia-mobile/` :

| Concept React | Fichier |
|---|---|
| `useState` + `useEffect` | `LoginScreen.tsx`, custom hooks |
| Custom hooks | `useDoctors.ts`, `useSlots.ts` |
| Composant avec props TypeScript | `SlotCard.tsx` (`SlotCardProps`) |
| `useRoute` (= useParams) | `SlotListScreen.tsx` |
| Context API | `AuthContext.tsx` — auth state global |
| Axios + intercepteur | `axiosInstance.ts` — Bearer JWT auto |
| React Navigation (Stack) | `AppNavigator.tsx` — navigation conditionnelle |

**Prochaines étapes mobile :**
- [ ] Écran Register (inscription patient)
- [ ] Écran "Mes rendez-vous" (créneaux du patient connecté)
- [ ] Calendrier visuel pour les créneaux (ex: `react-native-calendars`)
- [ ] Tab navigator (Accueil / RDV / Profil)

---

## Commandes utiles

```bash
# Backend
cd rdv-api
./mvnw spring-boot:run          # Lancer l'API (port 9000)
./mvnw test                     # Tests (18 tests)

# Frontend Web
cd rdv_medic_front
ng serve                        # Dev server (port 4200)
ng generate component component/nom-du-composant
ng build                        # Build prod

# Application Mobile (Expo)
cd doctolia-mobile
npm install                     # Installer les dépendances
npx expo install                # Valider les versions natives (si conflit)
npx expo start                  # Lancer Metro bundler
# Puis : appuyer sur 'i' (iOS simulator) ou 'a' (Android emulator)
# Sur Android emulator : changer BASE_URL dans src/api/axiosInstance.ts
#   → http://10.0.2.2:9000 (au lieu de localhost)
```

### Comptes de démo
| Rôle | Identifiant | Mot de passe |
|---|---|---|
| Patient | `pat.marc` | `password` |
| Patient | `pat.jean` | `password` |
| Médecin | `doc.john` | `password` |
| Médecin | `doc.paul` | `password` |
