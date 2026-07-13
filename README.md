# Doctolia

Application de prise de rendez-vous médicaux (type Doctolib), développée en monorepo.

- **Backend** : Java 17 + Spring Boot 3.5.4 → [`rdv-api/`](rdv-api/README.md)
- **Frontend Web** : Angular 20.2 → [`rdv_medic_front/`](rdv_medic_front/README.md)
- **Frontend Mobile** : React Native (Expo) → [`doctolia-mobile/`](doctolia-mobile/)

---

## 🚀 Lancer l'application pour la tester manuellement

Backend et frontend sont deux process séparés, à lancer dans deux terminaux différents.

### 1. Backend

```bash
cd rdv-api

# Pour tester le chat aussi (sinon cette ligne est facultative) :
docker compose up -d mongo

./mvnw spring-boot:run
# API sur http://localhost:9000, base H2 en mémoire (recréée à chaque démarrage)
```

### 2. Frontend Angular

```bash
cd rdv_medic_front
npm install      # première fois seulement
ng serve
# Interface sur http://localhost:4200
```

⚠️ L'URL du backend est codée en dur dans `rdv_medic_front/src/app/services/api.service.ts` (`baseURL = 'http://localhost:9000'`) — le backend doit tourner sur le port 9000, pas un autre.

### Comptes de démo (recréés à chaque démarrage du backend, mot de passe `password` pour tous)

| Rôle | Identifiant |
|---|---|
| Admin | `admin` |
| Médecin | `doc.john`, `doc.paul` |
| Patient | `pat.marc`, `pat.jean` |

---

## ✅ Ce qui fonctionne aujourd'hui (vérifié, pas supposé)

| Fonctionnalité | Backend | Frontend Angular |
|---|---|---|
| Connexion (JWT) | ✅ | ✅ `login.component` |
| Inscription patient / médecin | ✅ | ✅ `register.component` |
| Validation admin des inscriptions médecin | ✅ | ✅ `admin-dashboard.component` |
| Liste des médecins | ✅ | ✅ `doctor-list.component` |
| Calendrier + réservation d'un créneau | ✅ | ✅ `slot-list.component` |
| Mes rendez-vous (liste, annulation, motif) | ✅ | ✅ `mes-rdv.component` |
| Planning médecin (indisponibilité, marquer terminé) | ✅ | ✅ `mon-planning.component` |
| Profil (consulter/modifier) | ✅ | ✅ `profile.component` |
| **Chat lié à un RDV (MongoDB)** | ✅ | ❌ **`messagerie.component` est mocké** — données en dur (`MOCK_CONVERSATIONS`), aucun appel à l'API (le fichier contient lui-même un `// TODO: remplacer par un vrai appel API`) |
| Documents patient | ❌ (entité + repository seuls, aucun controller) | ❌ |
| Assistant IA | — (hors périmètre du projet) | ❌ `assistant-ia.component` mocké (`MOCK_RESPONSES`, jamais branché à un vrai service) |

Sécurité (ownership, deny-by-default, DTO d'entrée, tests de non-régression), déploiement (profils `dev`/`prod`, Docker, CI) : détaillés dans `rdv-api/AUDIT-SECURITE.md`, `rdv-api/CARNET-JUSTIFICATIONS.md` et `rdv-api/docs/`.

---

## 🧪 Scénario de test manuel suggéré

1. **Visiteur** — ouvrir `http://localhost:4200/accueil` sans être connecté : la liste des médecins doit être visible (accès public).
2. **Inscription** — `/register`, créer un compte patient → connexion automatique.
3. **Réservation** — `/accueil` → choisir un médecin → `/doctor-slots/{id}` → réserver un créneau libre sur le calendrier.
4. **Mes RDV** — `/mes-rdv` : voir le RDV réservé, modifier son motif, l'annuler.
5. **Ownership** (le cœur de la démonstration sécurité) — se déconnecter, se connecter avec un **autre** compte patient (`pat.jean` si tu étais sur `pat.marc`) : il ne doit voir **que ses propres** RDV sur `/mes-rdv`, jamais ceux de l'autre. Pour la preuve directe qu'une tentative d'accès croisé est bloquée (403), voir `rdv-api/AUDIT-SECURITE.md` (requêtes `curl` réelles) ou `rdv-api/README.md` section "Guide de test manuel via l'API".
6. **Médecin** — se connecter en `doc.john` → `/mon-planning` : ajouter une indisponibilité, marquer un RDV terminé.
7. **Inscription médecin + validation** — `/register` avec le type "Médecin" → tenter de se connecter avec ce compte → refusé (en attente de validation). Se connecter en `admin` → `/admin` → approuver le médecin → il peut désormais se connecter.
8. **Chat** — pas d'écran Angular fonctionnel pour l'instant (`messagerie.component` est un mock, voir tableau ci-dessus). Le tester nécessite `curl`/Postman directement sur l'API — voir `rdv-api/README.md`, section "Guide de test manuel via l'API (curl)".

---

## 📌 Ce qu'il reste à faire (honnête, post-certification)

- [ ] Brancher `messagerie.component` sur le vrai backend chat (l'API MongoDB existe et fonctionne, seul le composant Angular est mocké)
- [ ] `DocumentController` + upload UI (l'entité `Document` est modélisée, jamais implémentée côté API ni front)
- [ ] Décider du sort de `assistant-ia.component` (mock jamais branché à un vrai service — le garder comme démo visuelle assumée, ou le retirer)
- [ ] WebSocket/STOMP pour le chat en temps réel (choix assumé de rester en REST simple, voir `rdv-api/CARNET-JUSTIFICATIONS.md`)

---

## Commandes utiles

```bash
# Backend
cd rdv-api
./mvnw spring-boot:run          # Lancer l'API en dev (port 9000, H2)
./mvnw test                     # 31 tests (16 fonctionnels + 15 sécurité, MongoDB requis pour 4 d'entre eux)
docker compose up -d --build    # Stack complète : API + PostgreSQL + MongoDB (voir rdv-api/README.md)

# Frontend Web
cd rdv_medic_front
ng serve                        # Dev server (port 4200)
ng build                        # Build prod

# Application Mobile (Expo)
cd doctolia-mobile
npm install                     # Installer les dépendances
npx expo start
# Puis : appuyer sur 'i' (iOS simulator) ou 'a' (Android emulator)
# Sur Android emulator : changer BASE_URL dans src/api/axiosInstance.ts
#   → http://10.0.2.2:9000 (au lieu de localhost)
```

### Documentation approfondie

| Sujet | Où |
|---|---|
| Détail complet de l'API backend, sécurité, tests | [`rdv-api/README.md`](rdv-api/README.md) |
| Audit de sécurité (4 failles corrigées, preuves avant/après) | [`rdv-api/AUDIT-SECURITE.md`](rdv-api/AUDIT-SECURITE.md) |
| Justifications techniques (fiche de révision) | [`rdv-api/CARNET-JUSTIFICATIONS.md`](rdv-api/CARNET-JUSTIFICATIONS.md) |
| Diagrammes UML + schéma relationnel + table des endpoints | [`rdv-api/docs/`](rdv-api/docs/) |

### Concepts React/mobile déjà en place (`doctolia-mobile/`)

| Concept React | Fichier |
|---|---|
| `useState` + `useEffect` | `LoginScreen.tsx`, custom hooks |
| Custom hooks | `useDoctors.ts`, `useSlots.ts`, `useMySlots.ts` |
| Composant avec props TypeScript | `SlotCard.tsx` (`SlotCardProps`) |
| Context API | `AuthContext.tsx` — auth state global |
| Axios + intercepteur | `axiosInstance.ts` — Bearer JWT auto |
| React Navigation (Stack) | `AppNavigator.tsx` — navigation conditionnelle |
