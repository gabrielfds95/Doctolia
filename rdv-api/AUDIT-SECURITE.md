# Audit de sécurité — rdv-api

Preuves collectées en conditions réelles (app démarrée en local sur `localhost:9000`, base H2 avec les données de démo générées par `DataInitializer`). Chaque faille suit le format **avant / après**, avec requête + réponse brutes.

Comptes de démo utilisés : `pat.marc` (id patient = 4), `pat.jean` (id patient = 5), mot de passe `password` pour tous.

---

## 📋 Fiche de révision — les 4 failles en un coup d'œil

| # | Faille (terminologie OWASP) | Ce qu'un attaquant pouvait faire | Correctif | Ligne de code clé |
|---|---|---|---|---|
| **1** | **Broken Object Property Level Authorization** (OWASP API3:2023) — exposition de données sensibles sur un endpoint public | Sans aucun token, lire le NIR, le téléphone, l'adresse et le motif de consultation de **tous les patients** via `GET /slots` ou `GET /doctors/{id}/slots` ; ou, authentifié en tant que n'importe quel patient, lire le dossier complet d'un patient précis via `GET /slots/{doctor}/{patient}` | Nouveau DTO `PublicSlotDTO` sans champ `patient` ni `slotReason` sur les 2 endpoints publics ; `GET /slots/{doctor}/{patient}` **supprimé** (confirmé mort côté front, redondant avec les endpoints `/me/slots` scopés par ownership) — réduction de la surface d'attaque plutôt qu'un contrôle de plus à maintenir | `PublicSlotDTO.java` (pas de champ `patient`) ; endpoint retiré de `SlotController.java` |
| **2** | **Broken Object Level Authorization / IDOR** (OWASP API1:2023) | Authentifié en tant que patient A, annuler/modifier le RDV du patient B (ou médecin A clôturer le planning du médecin B) en changeant juste l'`{id}` dans l'URL | Vérification d'ownership dans `SlotService` : l'id de l'appelant (extrait du JWT) doit correspondre au `patient`/`doctor` du slot, sinon `403` | `SlotService.java` — `requireOwnerPatient()` / `requireOwnerDoctor()` |
| **3** | **Security Misconfiguration / Missing Function Level Access Control** (OWASP API5:2023) | En tant que simple patient connecté, lister tous les patients (avec NIR), ou supprimer le compte d'un médecin — ces routes n'avaient **aucune règle de rôle déclarée** | Règles explicites `hasRole("ADMIN")` / `hasRole("DOCTOR")` ajoutées pour chaque route sensible, avec un commentaire "deny-by-default" sur le filet de repli | `SecurityConfig.java` — bloc `hasRole("ADMIN")` sur `/patients`, `/patient`, `/patient/*`, `/doctor/*` |
| **4** | **Mass Assignment / Improper Input Validation** (OWASP API3:2023) | Glisser un champ `id` (ciblant un créneau existant d'un autre patient) ou `roles`/`enabled` dans le JSON envoyé, en s'appuyant sur le binding direct `@RequestBody Patient`/`@RequestBody Slot` | DTO d'entrée dédiés (`SlotCreateDTO`, `PatientCreateDTO`) qui ne déclarent que les champs autorisés au client — `id`/`status`/`roles` n'existent pas dans le type | `SlotCreateDTO.java` / `PatientCreateDTO.java` |

---

## Faille 1 — Fuite de données personnelles (PII) sans authentification

**Catégorie** : Broken Access Control / Sensitive Data Exposure (OWASP API3:2023 — Broken Object Property Level Authorization)

**Cause racine** : `GET /slots` et `GET /doctors/{id}/slots` sont déclarés `permitAll()` dans `SecurityConfig.java`, et renvoient `SlotDTO`, qui embarque un `PatientDTO` complet (NIR/SSN, téléphone, adresse, email) pour chaque rendez-vous — sans aucune vérification d'identité de l'appelant.

### Preuve — requête

```
$ curl -i http://localhost:9000/slots
```

Aucun header `Authorization` envoyé.

### Preuve — réponse (avant correctif)

```
HTTP/1.1 200
Content-Type: application/json

[
  {
    "id": 1,
    "slotDate": "2026-04-01",
    "slotReason": "Caries",
    "status": "RESERVED",
    "doctor": { "id": 3, "username": "doc.paul", ... },
    "patient": {
      "id": 4,
      "username": "pat.marc",
      "email": "marc.galar@rdvmedic.local",
      "ssn": "SSN-001",
      "phoneNumber": "0600000001",
      "address": "1 rue de Paris",
      "age": 34
    }
  },
  {
    "id": 2,
    "slotReason": "Rhume",
    "patient": {
      "id": 5,
      "username": "pat.jean",
      "ssn": "SSN-002",
      "phoneNumber": "0600000002",
      "address": "2 rue de Lyon",
      "age": 21
    }
  },
  ...
]
```

**Constat** : un attaquant non authentifié obtient, pour chaque patient du cabinet, son NIR, son téléphone, son adresse et le motif médical de sa consultation — sans avoir besoin du moindre identifiant.

### Tableau avant / après

| | **AVANT** | **APRÈS (correctif Phase 1 – point 1)** |
|---|---|---|
| Authentification requise sur `GET /slots` | ❌ Non (`permitAll()`) | ✅ Non — reste public (nécessaire pour parcourir le calendrier avant connexion), **mais** aucune donnée patient dans la réponse |
| Authentification requise sur `GET /doctors/{id}/slots` | ❌ Non (`permitAll()`) | ✅ Non — même raison, même correctif |
| DTO renvoyé | `SlotDTO` (patient complet imbriqué) | `PublicSlotDTO` (id, date, heure, statut, médecin — **aucun champ patient, aucun motif**) |
| SSN/téléphone/adresse visibles sans token | ✅ Oui | ❌ Non |
| `GET /slots/{idDoctor}/{idPatient}` (requête ciblée patient×médecin) | ❌ Public, retourne le patient complet | ✅ Authentification requise (`401` sans token) — retiré de la liste `permitAll()` |

### Preuve — réponse (après correctif)

```
$ curl -i http://localhost:9000/slots
```

```
HTTP/1.1 200
Content-Type: application/json

[
  {
    "id": 1,
    "slotDate": "2026-04-01",
    "slotTime": "09:30:00",
    "endTime": "10:00:00",
    "status": "RESERVED",
    "doctor": { "id": 3, "username": "doc.paul", "speciality": "Dentiste", ... }
  },
  {
    "id": 2,
    "status": "RESERVED",
    "doctor": { "id": 2, "username": "doc.john", ... }
  },
  ...
]
```

Plus aucun champ `patient`, plus de `slotReason`. Même constat sur `GET /doctors/2/slots`.

```
$ curl -i http://localhost:9000/slots/2/5   # requête ciblée sur un patient précis, sans token

HTTP/1.1 403 Forbidden
{"timestamp":"...","status":403,"error":"Forbidden","message":"Forbidden","path":"/slots/2/5"}
```

L'endpoint IDOR (`/slots/{idDoctor}/{idPatient}`) exige désormais une authentification.

### Mise à jour — endpoint supprimé (pas seulement fermé)

Le correctif initial (authentification requise) laissait un gap résiduel documenté : un patient authentifié pouvait toujours interroger le `SlotDTO` complet (NIR inclus) de n'importe quel autre patient via cet endpoint, faute de contrôle d'ownership.

**Recherche d'usage front, avant suppression** : recherche exhaustive dans `rdv_medic_front/` (Angular) et `doctolia-mobile/` (React Native/Expo). Résultat :
- `rdv_medic_front/src/app/services/api.service.ts:26-28` définit `getSlotsByDoctorsAndPatient(idDoctor, idPatient)` qui appelle cet endpoint — **mais cette méthode n'est appelée par aucun composant** (recherche `getSlotsByDoctorsAndPatient(` sur tout `src/**/*.ts` et `*.html` : seule la définition apparaît).
- `doctolia-mobile/` n'a aucun appel de ce type (les hooks `useSlots.ts`, `useMySlots.ts` utilisent `/doctors/{id}/slots`, `/patients/me/slots`, `/slots/{id}/cancel` — jamais `/slots/{doctor}/{patient}`).

**Conclusion : endpoint mort côté front comme côté backend.** Décision : **suppression complète** plutôt qu'ajout d'un contrôle d'ownership supplémentaire à maintenir — réduction de la surface d'attaque. Aucun impact front (rien à répercuter côté Angular/mobile, si ce n'est retirer par cohérence la méthode `getSlotsByDoctorsAndPatient` désormais définitivement morte côté Angular).

**Suppression** :
- `SlotController.getSlotsByDoctorIdAndPatientId()` (le `@GetMapping("/slots/{idDoctor}/{idPatient}")`) — retiré.
- `SlotService.getSlotsByDoctorIdAndPatientId()` — devenue orpheline, retirée.
- `SlotRepository.findByDoctorIdAndPatientId()` — devenue orpheline, retirée.
- Les 2 tests `@WebMvcTest` qui ciblaient cet endpoint (`SlotControllerTest`) — retirés (ils testaient une route qui n'existe plus).

**Découverte annexe pendant la vérification** : `GlobalExceptionHandler` avait un `@ExceptionHandler(Exception.class)` générique qui interceptait `NoResourceFoundException` (l'exception que Spring MVC lève quand aucune route ne correspond) et la transformait en **500** au lieu de **404** — un bug préexistant, jamais visible avant car aucune route n'avait encore été supprimée en cours de vie de l'app. Corrigé par l'ajout d'un `@ExceptionHandler(NoResourceFoundException.class)` dédié, retournant 404. Sans ce correctif, le test de non-régression ci-dessous aurait échoué (500 au lieu de 404 attendu) — c'est en écrivant ce test qu'on l'a détecté.

**Preuve — après suppression, en conditions réelles :**

```
$ curl -i http://localhost:9000/slots/3/4          # sans token
HTTP/1.1 403 Forbidden                              # bloqué par anyRequest().authenticated() AVANT
                                                      # même d'atteindre le dispatcher (route inexistante ou pas,
                                                      # peu importe : deny-by-default s'applique en premier)

$ curl -i http://localhost:9000/slots/3/4 -H "Authorization: Bearer <token valide>"
HTTP/1.1 404 Not Found
{"status":404,"error":"Not Found","message":"Route inexistante."}

# Les routes /slots légitimes fonctionnent toujours :
$ curl -o /dev/null -w "%{http_code}" http://localhost:9000/slots            # 200
$ curl -o /dev/null -w "%{http_code}" http://localhost:9000/doctors/2/slots  # 200
```

**Test de non-régression** : `SecurityRegressionTest.getSlotsByDoctorAndPatient_routeNoLongerExists` — appelle la route **avec un token valide** (pas sans) pour isoler précisément "la route n'existe plus" (404) de "bloqué par la sécurité" (403, qui se produirait de toute façon sans token même si la route existait encore).

---

## Faille 2 — Absence de contrôle d'ownership sur les créneaux (IDOR)

**Catégorie** : Broken Object Level Authorization (OWASP API1:2023 — le grand classique de l'IDOR)

**Cause racine** : `SlotController.cancelSlot()` (et `updateSlotReason`, `completeSlot`) ne vérifient jamais que le `slotId` passé dans l'URL appartient bien à l'utilisateur authentifié. Seul le **rôle** (`ROLE_PATIENT`) est vérifié par `SecurityConfig` — n'importe quel patient connecté peut donc agir sur le rendez-vous de n'importe quel autre patient.

### Preuve — scénario

1. Rendez-vous `id=2` appartient au patient `pat.jean` (id=5), statut `RESERVED`.
2. Connexion avec le compte `pat.marc` (id=4) — un patient **différent**.
3. `pat.marc` annule le rendez-vous `id=2` en changeant simplement l'id dans l'URL.

### Preuve — requêtes et réponses

**Étape 1 — état initial du slot 2 (via `GET /slots`, avant correctif de la faille 1) :**
```json
{ "id": 2, "status": "RESERVED", "patient": { "id": 5, "username": "pat.jean" } }
```

**Étape 2 — connexion `pat.marc` :**
```
$ curl -i -X POST http://localhost:9000/login \
  -H "Content-Type: application/json" \
  -d '{"username":"pat.marc","password":"password"}'

HTTP/1.1 200
{
  "id": 4,
  "username": "pat.marc",
  "token": "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJwYXQubWFyYyIsInVzZXJJZCI6NCwicm9sZXMiOiJST0xFX1BBVElFTlQiLCJpYXQiOjE3ODM4NjY1MDEsImV4cCI6MTc4Mzg2NzQwMX0...",
  "roles": ["ROLE_PATIENT"]
}
```
Le JWT décodé contient bien `"userId":4` (pat.marc), pas 5.

**Étape 3 — attaque : annulation du slot d'un autre patient**
```
$ curl -i -X PATCH http://localhost:9000/slots/2/cancel \
  -H "Authorization: Bearer <token de pat.marc, userId=4>"

HTTP/1.1 200
{
  "id": 2,
  "status": "CANCELLED",
  "patient": { "id": 5, "username": "pat.jean", "ssn": "SSN-002", ... }
}
```

**Constat** : réponse `200 OK` (attendu : `403 Forbidden`). Le statut passe de `RESERVED` à `CANCELLED` en base. `pat.marc` (id=4) a annulé, sans aucune autorisation légitime, le rendez-vous de `pat.jean` (id=5) — un patient qu'il ne connaît pas et dont il ne devrait même pas savoir qu'il existe.

### Tableau avant / après

| | **AVANT** | **APRÈS (prévu — Phase 1, point 2)** |
|---|---|---|
| `PATCH /slots/{id}/cancel` par un patient tiers | `200 OK`, RDV annulé | `403 Forbidden`, RDV inchangé |
| `PATCH /slots/{id}` (motif) par un patient tiers | `200 OK`, motif modifié | `403 Forbidden` |
| `PUT /slots/{id}/complete` par un médecin tiers | `200 OK` | `403 Forbidden` |
| Vérification de propriété | Aucune — seul le rôle est vérifié | `slot.getPatient().getId().equals(jwtUserId)` (patient) / `slot.getDoctor().getId().equals(jwtUserId)` (médecin) |

### Preuve — réponse (après correctif)

**Attaque rejouée à l'identique** : `pat.marc` (id=4) tente d'annuler le slot `2` appartenant à `pat.jean` (id=5).

```
$ curl -i -X PATCH http://localhost:9000/slots/2/cancel -H "Authorization: Bearer <token pat.marc>"

HTTP/1.1 403 Forbidden
{"timestamp":"2026-07-12T18:23:05","status":403,"error":"Forbidden","message":"Ce créneau ne vous appartient pas."}
```

**Cas légitime** : `pat.jean` (propriétaire réel) annule son propre slot `2`.

```
$ curl -i -X PATCH http://localhost:9000/slots/2/cancel -H "Authorization: Bearer <token pat.jean>"

HTTP/1.1 200 OK
{ "id": 2, "status": "CANCELLED", "patient": { "id": 5, "username": "pat.jean", ... } }
```

**Même vérification côté médecin** (`PUT /slots/{id}/complete`) : `doc.paul` (id=3) tente de clôturer un créneau du planning de `doc.john` (id=2) → `403 Forbidden` ("Ce créneau n'appartient pas à votre planning."). `doc.john` clôture son propre créneau → `200 OK`.

Les deux cas — accès refusé au tiers, accès autorisé au propriétaire — sont vérifiés, ce qui exclut un correctif "trop strict" qui bloquerait aussi le cas légitime.

---

---

## Faille 3 — Endpoints sans règle de sécurité déclarée (absence de deny-by-default)

**Catégorie** : Broken Access Control / Security Misconfiguration

**Cause racine** : `GET /patients`, `POST /patient`, `DELETE /patient/{id}`, `DELETE /doctor/{id}` et `DELETE /slot/{id}` n'apparaissaient dans **aucune** règle explicite de `SecurityConfig`. Ils retombaient donc sur la règle générique `.anyRequest().authenticated()` — accessibles à **n'importe quel utilisateur connecté, quel que soit son rôle**. Un simple patient authentifié pouvait ainsi lister tous les patients (avec leur NIR), en créer un, ou supprimer un médecin.

### Preuve — requêtes et réponses

**Avant correctif** (comportement déduit du code, la règle par défaut de Spring Security pour une route non listée + `anyRequest().authenticated()` autorise tout utilisateur connecté) :

| Requête | Rôle requis (avant) | Résultat avec un token `pat.marc` (PATIENT) |
|---|---|---|
| `GET /patients` | Aucun — juste connecté | `200 OK`, liste complète des patients (NIR inclus) |
| `DELETE /slot/{id}` | Aucun — juste connecté | `200`/`204` — un patient pouvait supprimer un créneau du planning d'un médecin |
| `DELETE /doctor/{id}` | Aucun — juste connecté | `204` — un patient pouvait supprimer un compte médecin |

**Après correctif** — testé en réel :

```
$ curl -o /dev/null -w "%{http_code}" http://localhost:9000/patients          # sans token
403
$ curl -o /dev/null -w "%{http_code}" -X DELETE http://localhost:9000/doctor/2  # sans token
403
$ curl -o /dev/null -w "%{http_code}" -X DELETE http://localhost:9000/slot/3    # sans token
403

# avec un token PATIENT (mauvais rôle) :
$ curl -o /dev/null -w "%{http_code}" http://localhost:9000/patients -H "Authorization: Bearer <token pat.marc>"
403
$ curl -o /dev/null -w "%{http_code}" -X DELETE http://localhost:9000/slot/3 -H "Authorization: Bearer <token pat.marc>"
403

# avec un token ADMIN (bon rôle) :
$ curl -o /dev/null -w "%{http_code}" http://localhost:9000/patients -H "Authorization: Bearer <token admin>"
200
```

### Tableau avant / après

| | **AVANT** | **APRÈS** |
|---|---|---|
| `GET /patients` | N'importe quel utilisateur connecté | `hasRole("ADMIN")` |
| `POST /patient` | N'importe quel utilisateur connecté | `hasRole("ADMIN")` |
| `DELETE /patient/{id}` | N'importe quel utilisateur connecté | `hasRole("ADMIN")` |
| `DELETE /doctor/{id}` | N'importe quel utilisateur connecté | `hasRole("ADMIN")` |
| `DELETE /slot/{id}` | N'importe quel utilisateur connecté | `hasRole("DOCTOR")` + ownership (son propre planning uniquement) |
| Règle de repli (`anyRequest()`) | `.authenticated()` (déjà correcte comme filet, mais rien ne forçait à déclarer les routes ci-dessus explicitement) | inchangée, mais commentée comme garde-fou explicite "deny-by-default" |

---

## Faille 4 — Mass assignment via `@RequestBody` sur des entités JPA

**Catégorie** : Improper Input Validation / Mass Assignment (OWASP API3:2023)

**Cause racine** : `POST /slot/{idDoctor}` et `POST /doctors/me/slots` acceptaient `@RequestBody Slot` (l'entité JPA directement), et `POST /patient` acceptait `@RequestBody Patient`. Un client pouvait donc fournir **n'importe quel champ** de l'entité dans le JSON, y compris des champs que le serveur est censé maîtriser seul.

### Preuve — requête et réponse

**Scénario** : `pat.jean`, authentifié, réserve normalement un créneau chez `doc.paul` (`POST /slot/3`) mais glisse un `id` correspondant à un créneau **existant d'un autre patient** (`id: 1`, appartenant à `doc.paul` / `pat.marc`), ainsi qu'un `status` arbitraire.

```
$ curl -i -X POST http://localhost:9000/slot/3 \
  -H "Authorization: Bearer <token pat.jean>" -H "Content-Type: application/json" \
  -d '{"id":1,"slotDate":"2026-05-01","slotTime":"11:00:00","endTime":"11:30:00","slotReason":"tentative ecrasement","status":"COMPLETED"}'
```

**Avant correctif** (raisonnement) : `Slot` a un id en `GenerationType.IDENTITY` — si le client fournit un `id` correspondant à une ligne existante, `slotRepository.save(slot)` exécute un **UPDATE** sur cette ligne (comportement standard de `JpaRepository.save()` avec Hibernate : id non-null et déjà en base → merge, pas insert), pas un `INSERT`. Le service écrase bien `doctor`/`patient`/`status` après binding — donc l'exploitation directe de ce chemin précis était déjà partiellement neutralisée — mais le principe reste dangereux : le seul rempart était "le développeur a pensé à tout écraser après coup", pas une garantie structurelle. Le champ `id` n'était filtré par rien.

**Après correctif** (testé en réel) : `SlotCreateDTO` n'a pas de champ `id`, `status`, `doctor` ni `patient` — ils n'existent tout simplement pas dans le type que Jackson désérialise.

```
HTTP/1.1 201 Created
{ "id": 4, "status": "RESERVED", "doctor": {...}, "patient": { "id": 5, "username": "pat.jean", ... } }

$ curl http://localhost:9000/doctors/3/slots
[
  { "id": 1, "status": "RESERVED", ... },   ← créneau original INTACT
  { "id": 4, "status": "RESERVED", ... }    ← nouveau créneau, id différent (INSERT, pas UPDATE)
]
```

Le `id: 1` fourni par le client a été purement et simplement ignoré : un nouveau créneau (`id: 4`) a été créé, le `status` forcé à `RESERVED` côté serveur (pas `COMPLETED` comme tenté), et le créneau `id: 1` d'origine reste inchangé.

**`POST /patient`** : même principe. Avant, un `Patient` envoyé avec un champ `roles` ou `enabled` aurait été accepté tel quel par `patientRepository.save(patient)`. `PatientCreateDTO` ne porte pas ces champs — rôle et statut sont désormais toujours fixés côté serveur (`ROLE_PATIENT`, `enabled = true`). Vérifié : le patient créé via `POST /patient` (avec un token ADMIN) se connecte ensuite avec succès et reçoit un JWT `roles: ["ROLE_PATIENT"]`, prouvant que le mot de passe est bien haché en BCrypt (l'ancien code le stockait en clair) et le rôle bien assigné.

### Tableau avant / après

| | **AVANT** | **APRÈS** |
|---|---|---|
| Type du body accepté sur `POST /slot/{id}` et `POST /doctors/me/slots` | `Slot` (entité JPA complète) | `SlotCreateDTO` (date, heure, motif — rien d'autre) |
| Type du body accepté sur `POST /patient` | `Patient` (entité JPA complète) | `PatientCreateDTO` (champs métier uniquement, jamais `roles`/`enabled`) |
| `id` fourni par le client pris en compte | Oui (risque d'UPDATE au lieu d'INSERT) | Non — le DTO n'a pas de champ `id` |
| Mot de passe patient créé via `POST /patient` | Stocké **en clair** (bug découvert pendant l'audit) | Haché en BCrypt (`passwordEncoder.encode(...)`) |
| Rôle assigné à la création | Aucun (compte inutilisable) | `ROLE_PATIENT` assigné automatiquement |

---

## Journal des correctifs

| Date | Faille | Statut |
|---|---|---|
| 2026-07-12 | Faille 1 — PII exposée sans authentification | ✅ Corrigée (voir diff `PublicSlotDTO` + `SecurityConfig`) |
| 2026-07-12 | Faille 2 — Absence d'ownership sur les slots | ✅ Corrigée (voir diff `SlotService` + `SlotController`) |
| 2026-07-12 | Faille 3 — Endpoints sans règle déclarée (deny-by-default) | ✅ Corrigée (voir diff `SecurityConfig`) |
| 2026-07-12 | Faille 4 — Mass assignment sur `Slot`/`Patient` | ✅ Corrigée (voir diff `SlotCreateDTO` + `PatientCreateDTO`) |
| 2026-07-12 | Gap résiduel — `GET /slots/{doctor}/{patient}` sans ownership | ✅ Corrigé par **suppression** de l'endpoint (confirmé mort dans tout le monorepo) |
| 2026-07-12 | Bug annexe — `NoResourceFoundException` renvoyait 500 au lieu de 404 | ✅ Corrigé (`GlobalExceptionHandler`) |

---

## Table finale des règles de sécurité — `SecurityConfig`

Toutes les routes de l'application, croisées avec la règle `SecurityConfig` qui s'applique et le contrôle d'ownership éventuel effectué en plus dans le service. Vérifiée exhaustivement en conditions réelles (`curl` sans token sur chaque ligne, voir plus bas).

| Route | Méthode | Règle `SecurityConfig` | Ownership (service) | Donnée patient exposée ? |
|---|---|---|---|---|
| `/login`, `/register` | POST | `permitAll()` | — | Non |
| `/h2-console/**`, `/error` | * | `permitAll()` | — | Non (console H2, dev uniquement) |
| `/doctors`, `/doctor/{id}` | GET | `permitAll()` | — | Non (`DoctorDTO`, pas de PII) |
| `/doctors/{id}/slots` | GET | `permitAll()` | — | **Non** — `PublicSlotDTO`, pas de champ `patient` |
| `/slots` | GET | `permitAll()` | — | **Non** — `PublicSlotDTO` |
| ~~`/slots/{idDoctor}/{idPatient}`~~ | GET | *supprimé* — 404 quel que soit l'appelant | — | — (route inexistante) |
| `/patients/me/slots` | GET | `hasRole("PATIENT")` | ✅ id extrait du JWT | Oui, mais seulement les siennes |
| `/doctors/me/slots` | GET, POST | `hasRole("DOCTOR")` | ✅ id extrait du JWT | Oui, mais seulement les siennes |
| `/slot/{idDoctor}` | POST | `hasRole("PATIENT")` | ✅ id patient extrait du JWT | — (création) |
| `/slot/{id}` | DELETE | `hasRole("DOCTOR")` | ✅ `requireOwnerDoctor` | — (suppression) |
| `/slots/{id}/cancel` | PATCH | `hasRole("PATIENT")` | ✅ `requireOwnerPatient` | Oui, mais seulement le sien |
| `/slots/{id}` | PATCH | `hasRole("PATIENT")` | ✅ `requireOwnerPatient` | Oui, mais seulement le sien |
| `/slots/{id}/complete` | PUT | `hasRole("DOCTOR")` | ✅ `requireOwnerDoctor` | Oui, mais seulement le sien |
| `/slots/{id}/messages` | GET, POST | `authenticated()` (les 2 rôles possibles) | ✅ `MessageService.requireParticipant` | Oui, mais seulement les 2 participants du RDV (Phase 3, MongoDB) |
| `/users/me` | GET, PATCH | `authenticated()` | ✅ id extrait du JWT (implicite : ne lit/modifie que sa propre ligne) | Oui, mais seulement soi-même |
| `/patients` | GET | `hasRole("ADMIN")` | — | Oui (réservé admin) |
| `/patient` | POST | `hasRole("ADMIN")` | — | — (création) |
| `/patient/{id}` | DELETE | `hasRole("ADMIN")` | — | — (suppression) |
| `/doctor/{id}` | DELETE | `hasRole("ADMIN")` | — | — (suppression) |
| `/admin/**` | * | `hasRole("ADMIN")` | — | Oui (réservé admin) |
| *toute autre route* | * | `authenticated()` *(deny-by-default)* | — | — |

**Le gap résiduel signalé précédemment (`GET /slots/{idDoctor}/{idPatient}` sans ownership) est fermé — par suppression de l'endpoint**, confirmé inutilisé dans tout le monorepo (voir section dédiée plus haut dans ce document).

**Vérifié en conditions réelles, sans token, sur chaque route `/slots` et `/slot`** :

```
GET    /slots                    → 200 (PublicSlotDTO, aucun champ "patient"/"ssn"/"phoneNumber"/"slotReason")
GET    /slots/2/5                → 403 (bloqué par deny-by-default avant même de savoir que la route n'existe plus)
PATCH  /slots/1/cancel           → 403
PATCH  /slots/1                  → 403
PUT    /slots/1/complete         → 403
POST   /slot/2                   → 403
DELETE /slot/1                   → 403
GET    /doctors/2/slots          → 200 (PublicSlotDTO, aucun champ "patient"/"ssn")
GET    /doctors/me/slots         → 403
POST   /doctors/me/slots         → 403
GET    /patients/me/slots        → 403
```

Et avec un token valide, sur la route supprimée spécifiquement :

```
GET /slots/3/4 -H "Authorization: Bearer <token valide>" → 404 ("Route inexistante.")
```

Seules les deux routes explicitement voulues publiques (`/slots`, `/doctors/{id}/slots`) répondent sans token — et toutes deux sans la moindre donnée patient dans le corps de la réponse.

---

## Tests de non-régression sécurité

Les 18 tests existants (`*ControllerTest`) sont des `@WebMvcTest` avec `excludeAutoConfiguration = {SecurityAutoConfiguration.class, ...}` et les services mockés (`@MockBean`) : ils ne passent **jamais** par la vraie chaîne de filtres Spring Security, donc ne peuvent structurellement pas détecter une régression sur une règle d'autorisation. Le fait qu'ils soient "18/18 verts" avant et après chaque correctif de cette session ne prouvait donc rien côté sécurité — c'est le problème soulevé, à raison.

**Nouvelle suite : `SecurityRegressionTest.java`** (`src/test/java/.../security/`)
- `@SpringBootTest` + `@AutoConfigureMockMvc` **sans** exclusion de sécurité : contexte Spring complet, vraie chaîne de filtres (`JwtAuthenticationFilter` inclus), vrais services, vraie base H2 seedée par `DataInitializer`.
- Authentification via de vrais JWT obtenus par `POST /login` (pas de `UserPrincipal` fabriqué à la main comme dans les tests `@WebMvcTest`).
- `@Transactional` au niveau classe : chaque test s'exécute dans sa propre transaction, annulée automatiquement à la fin — isolation entre tests sans redémarrer tout le contexte Spring à chaque fois.
- 11 tests, un par scénario de la checklist demandée (fuite PII sur `/slots` et `/doctors/{id}/slots`, rejet sans token sur `/slots/{d}/{p}`, ownership patient/médecin, deny-by-default sur `/doctor/{id}` et `/patients`, hachage BCrypt + rôle assigné sur `POST /patient`, non-écrasement sur `id` falsifié).

### Preuve que ces tests détectent réellement les régressions

Demande explicite : *"Ces tests doivent échouer si on remet le code d'avant. Vérifie-le."*

Méthode : `git worktree add` sur le commit `ba9488a` (état du code juste avant les 4 correctifs de cette session — confirmé via `git show --stat`, ce commit ne contient que l'ajout du carnet de justifications, aucun correctif), copie du fichier `SecurityRegressionTest.java` dans ce worktree isolé (sans toucher à l'arbre de travail principal), puis `./mvnw test -Dtest=SecurityRegressionTest` dans ce worktree.

**Résultat : 9 échecs sur 11 tests**, exactement les 9 qui ciblent une des 4 failles (les 2 qui passent sont les cas légitimes — `patientCancelsOwnSlot_succeeds` et `getPatients_withAdminRole_isAllowed` — qui n'ont jamais été cassés, ce n'est pas des gardes anti-régression).

```
[ERROR] Tests run: 11, Failures: 9, Errors: 0, Skipped: 0

getSlots_publicEndpoint_neverExposesPatientData        → contient "patient"/"ssn" (faille 1)
getDoctorSlots_publicEndpoint_neverExposesPatientData   → contient "patient" (faille 1)
getSlotsByDoctorAndPatient_withoutToken_isRejected      → 200 au lieu de 401/403 (faille 1)
patientCancelsAnotherPatientsSlot_isForbidden           → 200 au lieu de 403 (faille 2)
doctorCompletesAnotherDoctorsSlot_isForbidden           → 200 au lieu de 403 (faille 2)
patientDeletingDoctor_isForbidden                       → 204 au lieu de 403 (faille 3)
getPatients_withoutAdminRole_isForbidden                → 200 au lieu de 403 (faille 3)
createPatient_passwordIsHashed_andRoleIsAssigned        → mot de passe stocké en clair (faille 4)
createSlot_withForgedId_neverOverwritesExistingRow      → id du client accepté, écrase l'existant (faille 4)
```

Worktree supprimé après vérification (`rm -rf` + `git worktree prune`), aucune trace dans l'arbre de travail principal. Sur le code actuel (corrigé) : **29/29 tests verts** (18 existants + 11 nouveaux).
