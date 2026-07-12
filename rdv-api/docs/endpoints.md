# Table consolidée des endpoints — rdv-api

Version finale, après toutes les phases (sécurité, chat MongoDB, déploiement). Recopiée exactement depuis `SecurityConfig.java` (colonne Rôle) et les services (colonne Ownership) — 25 endpoints, aucun inventé. Voir `AUDIT-SECURITE.md` pour le détail des correctifs qui ont amené cette table à son état actuel.

**Légende Ownership** : ✅ = vérifié par une comparaison explicite (id JWT vs propriétaire réel, 403 sinon) · *implicite* = l'id vient exclusivement du JWT donc aucune ressource d'autrui n'est atteignable, mais pas de comparaison à proprement parler (rien à comparer) · — = sans objet (action publique ou administrative sans notion de "propriétaire").

## Authentification (`AuthController`)

| Méthode | URI | Rôle requis | Ownership | Description |
|---|---|---|---|---|
| POST | `/login` | `permitAll` | — | Authentification, retourne un JWT |
| POST | `/register` | `permitAll` | — | Inscription patient (actif immédiat) ou médecin (`enabled=false`, attente validation admin) |

## Accès public (`DoctorController`, `SlotController`)

| Méthode | URI | Rôle requis | Ownership | Description |
|---|---|---|---|---|
| GET | `/doctors` | `permitAll` | — | Liste des médecins actifs (`DoctorDTO`, sans PII) |
| GET | `/doctor/{id}` | `permitAll` | — | Profil d'un médecin actif |
| GET | `/doctors/{id}/slots` | `permitAll` | — | Calendrier d'un médecin (`PublicSlotDTO` : aucune donnée patient) |
| GET | `/slots` | `permitAll` | — | Tous les créneaux (`PublicSlotDTO` : aucune donnée patient) |

## Profil (`UserController`)

| Méthode | URI | Rôle requis | Ownership | Description |
|---|---|---|---|---|
| GET | `/users/me` | `authenticated` | implicite (id JWT) | Profil de l'utilisateur connecté |
| PATCH | `/users/me` | `authenticated` | implicite (id JWT) | Mise à jour partielle du profil |

## Patient (`SlotController`)

| Méthode | URI | Rôle requis | Ownership | Description |
|---|---|---|---|---|
| POST | `/slot/{idDoctor}` | `ROLE_PATIENT` | implicite (id JWT, création) | Réserve un créneau |
| GET | `/patients/me/slots` | `ROLE_PATIENT` | implicite (id JWT) | Liste des RDV du patient connecté |
| PATCH | `/slots/{id}/cancel` | `ROLE_PATIENT` | ✅ `requireOwnerPatient` | Annule un RDV |
| PATCH | `/slots/{id}` | `ROLE_PATIENT` | ✅ `requireOwnerPatient` | Modifie le motif d'un RDV |

## Médecin (`SlotController`)

| Méthode | URI | Rôle requis | Ownership | Description |
|---|---|---|---|---|
| GET | `/doctors/me/slots` | `ROLE_DOCTOR` | implicite (id JWT) | Planning complet du médecin connecté |
| POST | `/doctors/me/slots` | `ROLE_DOCTOR` | implicite (id JWT, création) | Ajoute une indisponibilité |
| PUT | `/slots/{id}/complete` | `ROLE_DOCTOR` | ✅ `requireOwnerDoctor` | Marque un RDV comme terminé |
| DELETE | `/slot/{id}` | `ROLE_DOCTOR` | ✅ `requireOwnerDoctor` | Supprime une indisponibilité |

## Chat d'un RDV (`MessageController`, MongoDB)

| Méthode | URI | Rôle requis | Ownership | Description |
|---|---|---|---|---|
| GET | `/slots/{id}/messages` | `authenticated` (patient ou médecin) | ✅ `MessageService.requireParticipant` | Historique des messages du RDV, chronologique |
| POST | `/slots/{id}/messages` | `authenticated` (patient ou médecin) | ✅ `MessageService.requireParticipant` | Envoie un message sur le RDV |

## Administration (`AdminController`, `PatientController`, `DoctorController`)

| Méthode | URI | Rôle requis | Ownership | Description |
|---|---|---|---|---|
| GET | `/admin/doctors/pending` | `ROLE_ADMIN` | — | Médecins en attente de validation (`enabled=false`) |
| PUT | `/admin/doctors/{id}/approve` | `ROLE_ADMIN` | — | Active un compte médecin |
| DELETE | `/admin/doctors/{id}/reject` | `ROLE_ADMIN` | — | Rejette (supprime) une inscription médecin |
| GET | `/patients` | `ROLE_ADMIN` | — | Liste tous les patients (NIR inclus — accès admin uniquement) |
| POST | `/patient` | `ROLE_ADMIN` | — | Création manuelle d'un compte patient (mot de passe haché BCrypt, `ROLE_PATIENT` assigné) |
| DELETE | `/patient/{id}` | `ROLE_ADMIN` | — | Supprime un compte patient |
| DELETE | `/doctor/{id}` | `ROLE_ADMIN` | — | Supprime un compte médecin |

## Routes fermées par deny-by-default

Toute route non listée ci-dessus tombe sur `anyRequest().authenticated()` (dernière règle de `SecurityConfig`) : accès refusé aux anonymes par défaut, jamais l'inverse. Voir `AUDIT-SECURITE.md`, faille 3.

## Endpoint supprimé (historique)

`GET /slots/{idDoctor}/{idPatient}` a existé puis a été **supprimé** (pas juste fermé) en Phase 1 : gap d'ownership résiduel, confirmé inutilisé par `rdv_medic_front` et `doctolia-mobile`, redondant avec `/patients/me/slots` et `/doctors/me/slots`. Retourne `404` depuis sa suppression. Voir `AUDIT-SECURITE.md`, faille 1.
