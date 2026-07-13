# 🏥 RDV Médic - API de Gestion de Rendez-vous Médicaux

Projet **Angular + Spring Boot** : Application complète de gestion de rendez-vous médicaux avec authentification JWT et Spring Security.

## 📋 Table des matières

- [Objectifs](#objectifs)
- [Architecture & Stack](#architecture--stack)
- [Schéma UML des Entités](#schéma-uml-des-entités)
- [Fonctionnalités](#fonctionnalités)
- [Utilisateurs et Rôles](#utilisateurs-et-rôles)
- [Workflow de Réservation](#workflow-de-réservation)
- [API REST](#api-rest)
- [Sécurité & Authentification](#sécurité--authentification)
- [Dépendances Maven](#dépendances-maven)
- [État du projet](#-état-du-projet-à-jour-fin-de-session-de-certification)
- [Installation & Démarrage](#-installation--démarrage)
- [Déploiement (profils Spring + Docker)](#-déploiement-profils-spring--docker)
- [Guide de test manuel via l'API (curl)](#-guide-de-test-manuel-via-lapi-curl)

---

## 🎯 Objectifs

Développer une application web permettant aux **patients** de prendre des rendez-vous avec des **médecins** via une interface sécurisée.

Le système doit gérer :
- ✅ Les médecins et leurs spécialités
- ✅ Les créneaux horaires indisponibles
- ✅ Les patients et leurs documents médicaux
- ✅ Les réservations de rendez-vous
- ✅ L'authentification et l'autorisation sécurisées (JWT)

---

## 🏗️ Architecture & Stack

| Composant | Technologie |
|-----------|-------------|
| **Frontend** | Angular 18+ |
| **Backend** | Java 17 + Spring Boot 3.5.4 |
| **Sécurité** | Spring Security 6 + JWT (JJWT) |
| **Base de données** | H2 (développement), PostgreSQL (production) |
| **ORM** | JPA/Hibernate |
| **Build** | Maven |
| **Validation** | Jakarta Bean Validation |
| **Mapping** | Lombok + ModelMapper |

---

## 📊 Schéma UML des Entités

```
┌────────────────────────────────────────────────────────────────┐
│                      👤 USER (Entité Mère)                     │
├────────────────────────────────────────────────────────────────┤
│ PK  id: Long                                                   │
│ ⚠️  username: String (UNIQUE)                                  │
│ ⚠️  email: String (UNIQUE)                                     │
│     password: String (BCrypt)                                  │
│     firstName: String                                          │
│     lastName: String                                           │
│     userType: UserType {PATIENT, DOCTOR}                       │
│     enabled: Boolean                                           │
│     createdAt: LocalDateTime                                   │
│ FK  roles: Set<Role> (ManyToMany) ↔ ROLE_PATIENT, ROLE_DOCTOR  │
└────────────────────────────────────────────────────────────────┘
         △                              △
         │ @Inheritance                 │ @Inheritance
         │ (JOINED)                     │ (JOINED)
         │                              │
    ┌────┴──────────────┐       ┌───────┴────────┐
    │  👨‍⚕️ DOCTOR         │       │ 🧑‍🤝‍🧑 PATIENT     │
    ├───────────────────┤       ├────────────────┤
    │ PK  id: Long      │       │ PK  id: Long   │
    │     speciality    │       │     ssn        │
    │     license       │       │     phone      │
    │     department    │       │     address    │
    │     experience    │       │                │
    │                   │       │                │
    │ FK  slots: List   │◄──────┼─── FK patient  │
    │     (OneToMany)   │       │     (ManyToOne)│
    │                   │       │                │
    │                   │       │ FK  documents: │
    │                   │       │     List       │
    │                   │       │     (OneToMany)│
    └───────────────────┘       └────────────────┘
         │                            │
         │ OneToMany                  │
         │ (owner: doctor)            │
         │                            │
    ┌────┴────────────────┐           │
    │  📅 SLOT            │           │
    │  (Créneau)          │           │
    ├─────────────────────┤           │
    │ PK  id: Long        │           │
    │     slotDate        │           │
    │     slotTime        │           │
    │     endTime         │           │
    │     slotReason      │           │
    │     status          │           │
    │ FK  doctor_id       │           │
    │ FK  patient_id ◄────┘           │
    │ (ManyToOne)         │           │
    └─────────────────────┘           │  
                                      │
         ┌────────────────────────────┐
         │  📄 DOCUMENT               │
         ├────────────────────────────┤
         │ PK  id: Long               │
         │     name: String           │
         │     type: String (PDF...)  │
         │     fileUrl: String        │
         │     uploadedAt             │
         │ FK  patient_id (ManyToOne) │
         └────────────────────────────┘

    ┌──────────────────────────────────┐
    │  🔐 ROLE                         │
    ├──────────────────────────────────┤
    │ PK  id: Long                     │
    │     name: RoleName               │
    │     {ROLE_PATIENT, ROLE_DOCTOR}  │
    │ FK  users (ManyToMany)           │
    └──────────────────────────────────┘
```

---

## ⚙️ Fonctionnalités

### Pour les Patients 🧑‍🤝‍🧑
- ✅ S'inscrire et se connecter
- ✅ Voir la liste des médecins avec spécialités
- ✅ Consulter les créneaux disponibles
- ✅ Réserver un rendez-vous
- ✅ Voir et gérer ses rendez-vous
- ✅ Télécharger/gérer ses documents médicaux
- ✅ Annuler un rendez-vous

### Pour les Médecins 👨‍⚕️
- ✅ Se connecter au système
- ✅ Gérer ses créneaux disponibles (CRUD)
- ✅ Voir ses rendez-vous réservés
- ✅ Voir les informations des patients
- ✅ Gérer le statut des rendez-vous (confirmé, annulé, complété)

### Admin 🔧
- ✅ Gérer les utilisateurs (patients, médecins)
- ✅ Gérer les spécialités
- ✅ Voir les statistiques

---

## 👥 Utilisateurs et Rôles

### Patient
- **Rôle** : `ROLE_PATIENT`
- **Permissions** :
  - Voir la liste des médecins
  - Voir les créneaux disponibles
  - Réserver un créneau
  - Consulter ses rendez-vous
  - Télécharger ses documents

### Médecin
- **Rôle** : `ROLE_DOCTOR`
- **Permissions** :
  - Ajouter/modifier/supprimer ses créneaux
  - Voir ses rendez-vous réservés
  - Modifier le statut des rendez-vous
  - Voir les informations des patients

---

## 📍 Workflow de Réservation

```
┌─────────────┐
│   PATIENT   │
└──────┬──────┘
       │
       ├─→ 1. Visualise la liste des médecins ➜ GET /doctors
       │
       ├─→ 2. Sélectionne un médecin et voit ses créneaux ➜ GET /doctors/{id}/slots
       │
       ├─→ 3. Choisit un créneau et réserve ➜ POST /slot/{idDoctor}
       │
       └─→ 4. Confirmation : RDV créé (201) ✅
       
┌──────────────┐
│   BACKEND    │
└──────┬───────┘
       │
       ├─→ Vérifie que le médecin existe et est actif (404/403 sinon)
       │
       ├─→ Vérifie l'absence de conflit horaire (409 sinon)
       │
       ├─→ Crée le Slot : doctor + patient (id extrait du JWT) + status=RESERVED
       │
       └─→ Retourne le SlotDTO créé (201 Created)
```
Séquence détaillée (avec les branches d'erreur) : [`docs/sequence-diagram-booking.puml`](docs/sequence-diagram-booking.puml).

---

## 🔌 API REST

⚠️ Aucun préfixe `/api/` — tous les endpoints sont à la racine (`http://localhost:9000/...`).

**Liste exhaustive et à jour (25 endpoints, méthode/URI/rôle/ownership)** : voir [`docs/endpoints.md`](docs/endpoints.md) — généré et vérifié directement depuis le code (`grep` sur toutes les annotations `@*Mapping`), c'est la seule source fiable. Un résumé rapide :

```bash
POST   /login                      # Connexion → JWT
POST   /register                   # Inscription patient (actif) ou médecin (en attente admin)
GET    /doctors                    # Liste publique des médecins
GET    /doctors/{id}/slots         # Calendrier public d'un médecin (sans données patient)
POST   /slot/{idDoctor}            # PATIENT : réserver un créneau
GET    /patients/me/slots          # PATIENT : mes rendez-vous
PATCH  /slots/{id}/cancel          # PATIENT : annuler (ownership vérifié)
GET    /doctors/me/slots           # DOCTOR : mon planning
POST   /doctors/me/slots           # DOCTOR : ajouter une indisponibilité
PUT    /slots/{id}/complete        # DOCTOR : marquer terminé (ownership vérifié)
GET    /slots/{id}/messages        # Chat du RDV (2 participants uniquement)
POST   /slots/{id}/messages        # Chat du RDV (2 participants uniquement)
GET    /admin/doctors/pending      # ADMIN : médecins en attente
PUT    /admin/doctors/{id}/approve # ADMIN : valider un médecin
```

---

## 🔒 Sécurité & Authentification

### 🔐 Spring Security

- **Authentification** : username + password (BCrypt, 10 rounds)
- **Autorisation** : par rôle (`hasRole`) déclarée dans `SecurityConfig`, + contrôle d'ownership dans les services pour les actions sur une ressource précise (voir `AUDIT-SECURITE.md`)
- **Stateless** : `SessionCreationPolicy.STATELESS`, pas de session HTTP côté serveur

### 🎫 JWT — structure réelle (vérifiée par décodage d'un vrai token)

```json
{
  "header": { "alg": "HS384" },
  "payload": {
    "sub": "pat.marc",
    "userId": 4,
    "roles": "ROLE_PATIENT",
    "iat": 1783866501,
    "exp": 1783867401
  }
}
```

- **Durée de vie** : 15 minutes (`app.jwt.expiration=900000` ms). Pas de refresh token (le champ `refreshToken` existe dans la réponse JSON mais vaut toujours `null` — aucun endpoint `/refresh` n'existe).
- **Secret** : `${JWT_SECRET:valeur-par-défaut-dev}` — voir `CARNET-JUSTIFICATIONS.md` pour la justification de ce pattern et le garde-fou anti-oubli en prod.

### 🛡️ Endpoints protégés

```bash
Authorization: Bearer <JWT>

# Exemple :
curl -H "Authorization: Bearer eyJhbGciOiJIUzM4NCJ9..." http://localhost:9000/patients/me/slots
```

---

## 📦 Dépendances Maven

```xml
<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- JWT JJWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.5</version>
    <scope>runtime</scope>
</dependency>

<!-- Validation -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- ModelMapper (optionnel) -->
<dependency>
    <groupId>org.modelmapper</groupId>
    <artifactId>modelmapper</artifactId>
    <version>3.1.1</version>
</dependency>
```

---

## 📅 État du projet (à jour, fin de session de certification)

Cette section a longtemps décrit une feuille de route qui ne correspondait plus au code réel — remplacée par un état des lieux factuel. Le détail complet (failles corrigées, justifications, diagrammes) vit dans des documents dédiés, pas ici, pour ne pas se re-périmer :

| Sujet | Document |
|---|---|
| Sécurité (4 failles corrigées + preuves avant/après) | [`AUDIT-SECURITE.md`](AUDIT-SECURITE.md) |
| Justifications techniques (Q/R pour l'oral) | [`CARNET-JUSTIFICATIONS.md`](CARNET-JUSTIFICATIONS.md) |
| Diagrammes UML (cas d'usage, classes, séquence, déploiement) | [`docs/`](docs/) |
| Schéma relationnel (dictionnaire de données) | [`docs/schema-relationnel.md`](docs/schema-relationnel.md) |
| Table consolidée des 25 endpoints | [`docs/endpoints.md`](docs/endpoints.md) |

**Résumé** : sécurité (JWT, ownership, deny-by-default, DTO d'entrée) traitée en profondeur ; chat temps réel implémenté en MongoDB (composant NoSQL du référentiel) avec ownership équivalente ; déploiement via profils Spring `dev`/`prod` + Docker Compose (API + PostgreSQL + MongoDB) + CI GitHub Actions. Portails patient/médecin/admin (Phase 2 du plan initial) traités en parallèle côté Angular — voir la racine du monorepo pour l'état détaillé front/back. `DocumentController` et le WebSocket temps réel du chat restent hors périmètre, assumé.

---

## 🚀 Installation & Démarrage

### Mode développement (rapide, sans Docker)

**Prérequis :** Java 17+, Maven 3.8+ (ou le wrapper `./mvnw` fourni), Git. MongoDB via Docker si tu veux tester le chat (`docker compose up -d mongo`) — le driver Mongo est paresseux, l'app démarre même sans lui.

```bash
git clone <repo-url>
cd rdv-api
./mvnw spring-boot:run
# API sur http://localhost:9000, base H2 en mémoire, aucune config requise
```

### Frontend (Angular)

```bash
cd rdv_medic_front
npm install
ng serve
# Interface sur http://localhost:4200
```

---

## 🚢 Déploiement (profils Spring + Docker)

### Profils Spring : `dev` (H2) / `prod` (PostgreSQL)

| | `dev` (défaut) | `prod` |
|---|---|---|
| Base SQL | H2 en mémoire | PostgreSQL |
| Activation | Aucune (défaut) | `SPRING_PROFILES_ACTIVE=prod` |
| Config | `application-dev.properties` | `application-prod.properties` |
| Console H2 | `/h2-console` activée | absente |
| Credentials DB | aucun (H2 en mémoire) | `POSTGRES_*` obligatoires, pas de défaut |

Basculer manuellement en local (sans Docker), par exemple contre un Postgres déjà démarré :

```bash
SPRING_PROFILES_ACTIVE=prod \
POSTGRES_HOST=localhost POSTGRES_PORT=5432 POSTGRES_DB=doctolia \
POSTGRES_USER=doctolia POSTGRES_PASSWORD=<motdepasse> \
JWT_SECRET=<secret> \
./mvnw spring-boot:run
```

### Stack complète via Docker Compose (API + PostgreSQL + MongoDB)

**Prérequis :** Docker + Docker Compose. Aucun autre logiciel requis — c'est tout l'intérêt.

**Procédure reproductible depuis un poste vierge :**

```bash
# 1. Cloner le monorepo
git clone https://github.com/gabrielfds95/Doctolia.git
cd Doctolia/rdv-api

# 2. Configurer les secrets (jamais commités, voir .gitignore)
cp .env.example .env
# éditer .env : renseigner JWT_SECRET et POSTGRES_PASSWORD avec de vraies valeurs
#   ex. génération d'un secret : openssl rand -base64 32

# 3. Construire l'image et démarrer toute la stack
docker compose up -d --build

# 4. Vérifier que tout tourne
docker compose ps
docker compose logs api --tail 30
# → doit afficher : "The following 1 profile is active: prod"

# API disponible sur http://localhost:9000
```

**Variables d'environnement** (voir `.env.example`) :

| Variable | Rôle | Défaut |
|---|---|---|
| `JWT_SECRET` | Clé de signature des JWT | ❌ aucun — obligatoire, `docker compose` refuse de démarrer sans |
| `POSTGRES_DB` | Nom de la base | `doctolia` |
| `POSTGRES_USER` | Utilisateur PostgreSQL | `doctolia` |
| `POSTGRES_PASSWORD` | Mot de passe PostgreSQL | ❌ aucun — obligatoire |

**Arrêter la stack :** `docker compose down` (ajouter `-v` pour aussi supprimer les volumes de données).

### CI — Non-régression automatisée (GitHub Actions)

Fichier : [`.github/workflows/rdv-api-ci.yml`](../.github/workflows/rdv-api-ci.yml) (à la racine du monorepo — GitHub Actions exige ses workflows sous `.github/workflows/` au niveau du dépôt).

- Déclenché à chaque `push` touchant `rdv-api/**`.
- `./mvnw clean compile` puis `./mvnw test` — la suite complète (18 tests fonctionnels + 15 tests de sécurité, dont l'ownership du chat MongoDB).
- Un **service container MongoDB** (`mongo:7`) est démarré par le workflow avant les tests : sans lui, les 4 tests de chat échoueraient (le reste de la suite, en H2, ne dépend pas de Mongo grâce à la connexion paresseuse du driver).
- PostgreSQL n'est **pas** nécessaire en CI : les tests tournent sous le profil `dev` (H2) par défaut — seul le déploiement réel (`docker compose up`) utilise PostgreSQL.
- Rapports de test (`surefire-reports`) publiés comme artefact à chaque run.

---

## 🧪 Guide de test manuel via l'API (curl)

Suite de commandes **réellement exécutables** telles quelles (copier-coller), contre un serveur lancé en local (`./mvnw spring-boot:run`, profil `dev`/H2). Utilise les comptes de démo créés par `DataInitializer` (mot de passe `password` pour tous).

### 1️⃣ Connexion et récupération du token

```bash
curl -s -X POST http://localhost:9000/login \
  -H "Content-Type: application/json" \
  -d '{"username":"pat.marc","password":"password"}'

# Réponse réelle (extrait) :
# {"id":4,"username":"pat.marc","token":"eyJhbGciOiJIUzM4NCJ9...","expiresIn":900000,"roles":["ROLE_PATIENT"]}
```

Pour la suite, exporte le token dans une variable :
```bash
TOKEN=$(curl -s -X POST http://localhost:9000/login \
  -H "Content-Type: application/json" \
  -d '{"username":"pat.marc","password":"password"}' | python3 -c "import json,sys;print(json.load(sys.stdin)['token'])")
```

### 2️⃣ Réserver un créneau (accès public + réservation authentifiée)

```bash
# Voir les créneaux disponibles chez un médecin (public, sans token)
curl -s http://localhost:9000/doctors/2/slots

# Réserver (PATIENT authentifié) — id du patient pris du JWT, jamais du body
curl -s -X POST http://localhost:9000/slot/2 \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"slotDate":"2026-08-01","slotTime":"09:00:00","endTime":"09:30:00","slotReason":"Consultation test"}'
```

### 3️⃣ Démontrer l'ownership (le cœur de la sécurité)

```bash
# pat.marc annule SON PROPRE rdv (remplacer {id} par l'id retourné à l'étape 2) → 200
curl -s -i -X PATCH http://localhost:9000/slots/{id}/cancel -H "Authorization: Bearer $TOKEN"

# pat.jean (un AUTRE patient) tente d'annuler le même rdv → 403 attendu
TOKEN_JEAN=$(curl -s -X POST http://localhost:9000/login -H "Content-Type: application/json" \
  -d '{"username":"pat.jean","password":"password"}' | python3 -c "import json,sys;print(json.load(sys.stdin)['token'])")
curl -s -i -X PATCH http://localhost:9000/slots/{id}/cancel -H "Authorization: Bearer $TOKEN_JEAN"
# → 403 Forbidden "Ce créneau ne vous appartient pas."
```

### 4️⃣ Chat d'un RDV — ⚠️ pas d'écran Angular pour ça (voir plus bas), testable uniquement via l'API

Nécessite MongoDB démarré (`docker compose up -d mongo`) :

```bash
curl -s -X POST http://localhost:9000/slots/1/messages \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"content":"Bonjour docteur"}'

curl -s http://localhost:9000/slots/1/messages -H "Authorization: Bearer $TOKEN"
```

### 5️⃣ Inscription médecin + validation admin

```bash
# Inscription médecin → enabled=false, pas de token retourné
curl -s -X POST http://localhost:9000/register -H "Content-Type: application/json" \
  -d '{"username":"doc.test","email":"doc.test@mail.com","password":"password123","firstName":"Test","lastName":"Doc","userType":"DOCTOR","speciality":"Généraliste","licenseNumber":"LIC-TEST","department":"Général","experienceYears":3}'

# Connexion admin puis validation
TOKEN_ADMIN=$(curl -s -X POST http://localhost:9000/login -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}' | python3 -c "import json,sys;print(json.load(sys.stdin)['token'])")
curl -s http://localhost:9000/admin/doctors/pending -H "Authorization: Bearer $TOKEN_ADMIN"
curl -s -X PUT http://localhost:9000/admin/doctors/{id}/approve -H "Authorization: Bearer $TOKEN_ADMIN"
```

Plus d'exemples (tous les rôles, toutes les branches d'erreur) : `AUDIT-SECURITE.md` contient des dizaines de requêtes réelles avec leurs réponses exactes.

---

## 🔗 Ressources Utiles

- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [JJWT GitHub](https://github.com/jwtk/jjwt)
- [Spring Boot Tutorial](https://openclassrooms.com/fr/courses/6900101-creez-une-application-java-avec-spring-boot)
- [Angular Tutorial](https://openclassrooms.com/en/courses/7471261-debutez-avec-angular)

---

## 📁 Guide des fichiers complexes

### `model/User.java`
Classe parente de toutes les entités utilisateurs. Utilise la stratégie d'héritage JPA **JOINED** : une table `users` contient les colonnes communes (id, username, email, password, firstName, lastName, enabled, roles, createdAt), et chaque sous-type a sa propre table jointe.

Points critiques :
- `@SuperBuilder` de Lombok (obligatoire — `@Builder` classique génère un conflit avec les sous-classes).
- `@DiscriminatorColumn(name = "user_type")` — valeur `'DOCTOR'` ou `'PATIENT'` insérée automatiquement.
- `@ManyToMany(fetch = EAGER)` sur `roles` → nécessaire pour Spring Security qui lit les rôles à chaque requête.
- `@PreUpdate` → met à jour `updatedAt` automatiquement.

### `model/Doctor.java` et `model/Patient.java`
Entités filles héritant de `User`. Chacune a sa propre table (`doctors`, `patients`) reliée via la clé primaire partagée.
- `@SuperBuilder` obligatoire (même annotation que la classe parente, sinon erreur `builder() cannot hide builder()`).
- `Doctor` ajoute : speciality, licenseNumber, department, experienceYears + liste de `Slot`.
- `Patient` ajoute : ssn, phoneNumber, address, age + liste de `Slot`.

---

### `config/SecurityConfig.java`
Configure toute la chaîne de sécurité Spring Security :

- **Stateless** : pas de session HTTP — l'authentification repose entièrement sur le JWT à chaque requête.
- **CORS** : autorise `localhost:4200` (Angular) et `localhost:8081` (futur mobile React Native).
- **Règles d'autorisation** :
  - `/login`, `/register`, `/h2-console/**` → publics
  - `GET /doctors/**`, `GET /slots/**` → publics (lecture des médecins)
  - `POST /slot/**` → réservé aux `ROLE_PATIENT`
  - Tout le reste → authentifié
- Insère `JwtAuthenticationFilter` **avant** le filtre standard Spring.

---

### `security/JwtTokenProvider.java`
Responsable de la **génération** et **validation** des tokens JWT (JJWT 0.12.5).

- Clé secrète et durée lues depuis `application.properties` (`app.jwt.secret`, `app.jwt.expiration`).
- `generateToken()` : JWT signé HMAC-SHA avec `userId` et `roles` en claims custom.
- `validateToken()` : retourne `false` si invalide ou expiré, sans lever d'exception vers le client.
- `getUsernameFromToken()` : extrait le `subject` (username) pour charger l'utilisateur en base.

---

### `security/JwtAuthenticationFilter.java`
Filtre exécuté **une fois par requête** (`OncePerRequestFilter`). Séquence :

1. Extrait le token de `Authorization: Bearer <token>`.
2. Valide via `JwtTokenProvider`.
3. Charge l'utilisateur via `CustomUserDetailsService`.
4. Injecte un `UsernamePasswordAuthenticationToken` dans le `SecurityContextHolder`.
5. Passe la main au filtre suivant.

Si le token est absent ou invalide, la requête continue sans authentification (Spring Security applique ses règles normalement).

---

### `security/CustomUserDetailsService.java`
Implémente `UserDetailsService` (interface Spring Security). Unique rôle : charger un `User` depuis `UserRepository` par son username et le convertir en `UserPrincipal`. Appelé par le filtre JWT et par `AuthenticationManager` au login.

---

### `service/AuthService.java`
Logique métier de l'authentification. Deux opérations :

**`login()`** : délègue à `AuthenticationManager` → génère un JWT → retourne `AuthResponse`.

**`register()`** :
- Vérifie l'unicité du username et de l'email.
- Selon `UserType` (`PATIENT` ou `DOCTOR`), crée l'entité correspondante avec mot de passe BCrypt.
- Sauvegarde et génère immédiatement un JWT (connexion automatique après inscription).
- La méthode privée `buildAuthResponse()` construit l'`Authentication` directement depuis l'entité sauvegardée, sans re-passer par le login.

---

### `dto/RegisterRequest.java`
DTO d'entrée pour l'inscription. Champs communs obligatoires : `username`, `email`, `password` (min 8 chars), `firstName`, `lastName`, `userType`. Champs Patient optionnels : `ssn`, `phoneNumber`, `address`, `age`. Champs Doctor optionnels : `speciality`, `licenseNumber`, `department`, `experienceYears`.

Annotations `@NotBlank`, `@Email`, `@Size` pour validation automatique via `@Valid`.

---

### `dto/DoctorDTO.java`, `dto/PatientDTO.java`, `dto/SlotDTO.java`
Pattern DTO : les controllers ne retournent jamais les entités JPA directement (évite d'exposer les mots de passe hashés et les relations circulaires). Chaque DTO a une méthode `fromEntity()` avec null-check. Le mapping se fait au niveau du controller, les services restent inchangés.

---

### `exception/GlobalExceptionHandler.java`
`@RestControllerAdvice` qui intercepte les exceptions et retourne une réponse JSON structurée :
```json
{ "timestamp": "...", "status": 404, "error": "Not Found", "message": "Médecin introuvable : 99" }
```
Gère : `ResourceNotFoundException` (404), `MethodArgumentNotValidException` (400), `ResponseStatusException`, et toute `Exception` générique (500).

---

### `config/DataInitializer.java`
Initialise les données de démo au démarrage (`@EventListener(ApplicationReadyEvent.class)`) uniquement si la table `users` est vide. Crée 2 rôles, 2 médecins, 2 patients, 3 créneaux. Tous les mots de passe : BCrypt(`"password"`).

---

## 📧 Support

Pour toute question, consultez la documentation ou créez une issue.
