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
- [Phases de Développement](#phases-de-développement)
- [Installation & Démarrage](#-installation--démarrage)

---

## 🎯 Objectifs

Développer une application web permettant aux **patients** de prendre des rendez-vous avec des **médecins** via une interface sécurisée.

Le système doit gérer :
- ✅ Les médecins et leurs spécialités
- ✅ Les créneaux horaires disponibles
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
│                      👤 USER (Entité Mère)                    │
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
│ FK  roles: Set<Role> (ManyToMany) ↔ ROLE_PATIENT, ROLE_DOCTOR │
└────────────────────────────────────────────────────────────────┘
         △                              △
         │ @Inheritance                 │ @Inheritance
         │ (JOINED)                     │ (JOINED)
         │                              │
    ┌────┴──────────────┐       ┌───────┴────────┐
    │  👨‍⚕️ DOCTOR          │       │ 🧑‍🤝‍🧑 PATIENT      │
    ├───────────────────┤       ├────────────────┤
    │ PK  id: Long      │       │ PK  id: Long   │
    │     speciality    │       │     ssn        │
    │     license       │       │     phone      │
    │     department    │       │     address    │
    │     experience    │       │                │
    │                   │       │                │
    │ FK  slots: List   │◄──────┼─── FK patient │
    │     (OneToMany)   │       │     (ManyToOne)
    │                   │       │                │
    │                   │       │ FK  documents: │
    │                   │       │     List       │
    │                   │       │     (OneToMany)│
    └───────────────────┘       └────────────────┘
         │                            │
         │ OneToMany                  │
         │ (owner: doctor)            │
         │                            │
    ┌────┴────────────────┐      │
    │  📅 SLOT            │      │
    │  (Créneau)          │      │
    ├─────────────────────┤      │
    │ PK  id: Long        │      │
    │     slotDate        │      │
    │     slotTime        │      │
    │     endTime         │      │
    │     slotReason      │      │
    │     status          │      │
    │ FK  doctor_id       │      │
    │ FK  patient_id ◄────┘
    │ (ManyToOne)         │
    └─────────────────────┘

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

### Admin 🔧 (Optionnel)
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
       ├─→ 1. Visualise la liste des médecins ➜ GET /api/doctors
       │
       ├─→ 2. Sélectionne un médecin et voit ses créneaux ➜ GET /api/doctors/{id}/slots
       │
       ├─→ 3. Choisit un créneau et réserve ➜ POST /api/reservations
       │
       └─→ 4. Confirmation : RDV créé ✅
       
┌──────────────┐
│   BACKEND    │
└──────┬───────┘
       │
       ├─→ Vérifie la disponibilité du créneau
       │
       ├─→ Valide les données du patient
       │
       ├─→ Crée l'objet Slot avec patient_id
       │
       └─→ Retourne confirmation avec JWT token
```

---

## 🔌 API REST

### 🔐 Authentification

```bash
POST   /api/auth/register          # Inscription (Patient/Doctor)
POST   /api/auth/login             # Connexion (JWT)
POST   /api/auth/refresh           # Rafraîchir token
POST   /api/auth/logout            # Déconnexion
```

### 👨‍⚕️ Médecins

```bash
GET    /api/doctors                # Liste des médecins
GET    /api/doctors/{id}           # Détails d'un médecin
GET    /api/doctors/{id}/slots     # Créneaux d'un médecin
POST   /api/doctors/{id}/slots     # Ajouter un créneau (DOCTOR)
DELETE /api/slots/{id}             # Supprimer un créneau (DOCTOR)
PUT    /api/slots/{id}             # Modifier un créneau (DOCTOR)
```

### 🧑‍🤝‍🧑 Patients

```bash
GET    /api/patients/me            # Profil du patient connecté
PUT    /api/patients/me            # Mettre à jour profil
GET    /api/patients/{id}/slots    # Rendez-vous du patient
```

### 📅 Rendez-vous (Slots)

```bash
GET    /api/slots                  # Tous les créneaux (Admin)
GET    /api/slots/{id}             # Détails créneau
POST   /api/slots                  # Créer créneau (DOCTOR)
PUT    /api/slots/{id}             # Modifier créneau (DOCTOR)
DELETE /api/slots/{id}             # Supprimer créneau (DOCTOR)
PUT    /api/slots/{id}/cancel      # Annuler rendez-vous (PATIENT/DOCTOR)
```

### 📄 Documents

```bash
GET    /api/documents              # Documents du patient connecté
POST   /api/documents              # Uploader document
DELETE /api/documents/{id}         # Supprimer document
```

---

## 🔒 Sécurité & Authentification

### 🔐 Spring Security

- **Authentification** : Username/Email + Password (BCrypt)
- **Autorisation** : Rôles basés (ROLE_PATIENT, ROLE_DOCTOR)
- **Stateless** : Pas de session (JWT tokens)

### 🎫 JWT (JSON Web Token)

```json
{
  "header": {
    "alg": "HS512",
    "typ": "JWT"
  },
  "payload": {
    "sub": "user_id",
    "username": "patient@example.com",
    "roles": ["ROLE_PATIENT"],
    "iat": 1708150800,
    "exp": 1708237200
  },
  "signature": "..."
}
```

**Durée de vie** : 
- Access Token : 24 heures
- Refresh Token : 7 jours

### 🛡️ Endpoints Protégés

```
Tout endpoint nécessite un header :
Authorization: Bearer <JWT_TOKEN>

Exemple avec curl :
curl -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGc..." https://api.example.com/api/doctors
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

## 📅 Phases de Développement

### Phase 1️⃣ : Infrastructure & Sécurité ⏳ **EN COURS**

**Objectif** : Mettre en place les bases de Spring Security et JWT

- [x] Créer l'entité `User` (classe mère)
- [x] Créer l'entité `Role` pour Spring Security
- [x] Modifier `Patient` et `Doctor` pour hériter de `User`
- [x] Ajouter l'entité `Document`
- [x] Configurer Spring Security
- [x] Implémenter JwtTokenProvider
- [x] Créer AuthController (login/register)
- [x] Créer AuthService
- [ ] Tests d'authentification

**Dépendances** :
- Spring Security 6
- JJWT 0.12.5
- Jakarta Bean Validation

### Phase 2️⃣ : Réimplémentation des Services Existants

**Objectif** : Adapter les services existants avec la nouvelle architecture sécurisée

- [ ] Refactoriser `DoctorService`
- [ ] Refactoriser `PatientService`
- [ ] Refactoriser `SlotService`
- [ ] Implémenter les contrôles d'accès

### Phase 3️⃣ : Frontend Angular

**Objectif** : Créer l'interface utilisateur Angular

- [ ] Service d'authentification Angular
- [ ] Guard pour les routes protégées
- [ ] Formulaires d'inscription/connexion
- [ ] Liste des médecins
- [ ] Réservation de créneau
- [ ] Gestion des rendez-vous

### Phase 4️⃣ : Tests & Documentation

**Objectif** : Couvrir de tests et documenter l'API

- [ ] Tests unitaires (JUnit 5)
- [ ] Tests d'intégration
- [ ] Swagger/OpenAPI (optionnel)
- [ ] Documentation des endpoints

---

## 🚀 Installation & Démarrage

### Prérequis

- Java 17+
- Maven 3.8+
- Node.js 18+ (pour Angular)
- Git

### Backend

```bash
# 1. Cloner le projet
git clone <repo-url>
cd rdv-api

# 2. Installer les dépendances Maven
mvn clean install

# 3. Démarrer l'application
mvn spring-boot:run

# API sera accessible sur : http://localhost:9000
```

### Frontend

```bash
# 1. Créer le projet Angular (si nécessaire)
ng new rdv-frontend

# 2. Installer les dépendances
npm install

# 3. Démarrer le serveur Angular
ng serve

# Interface sera accessible sur : http://localhost:4200
```

---

## 📝 Exemple d'Authentification

### 1️⃣ Inscription Patient

```bash
POST /api/auth/register
Content-Type: application/json

{
  "username": "patient@example.com",
  "email": "patient@example.com",
  "password": "SecurePass123!",
  "firstName": "Jean",
  "lastName": "Dupont",
  "userType": "PATIENT"
}

✅ Réponse :
{
  "id": 1,
  "username": "patient@example.com",
  "token": "eyJ0eXAiOiJKV1QiLCJhbGc...",
  "refreshToken": "eyJ0eXAiOiJKV1QiLCJhbGc..."
}
```

### 2️⃣ Connexion

```bash
POST /api/auth/login
Content-Type: application/json

{
  "username": "patient@example.com",
  "password": "SecurePass123!"
}

✅ Réponse :
{
  "token": "eyJ0eXAiOiJKV1QiLCJhbGc...",
  "refreshToken": "eyJ0eXAiOiJKV1QiLCJhbGc...",
  "expiresIn": 86400
}
```

### 3️⃣ Accès aux Ressources Protégées

```bash
GET /api/doctors
Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGc...

✅ Réponse :
[
  {
    "id": 1,
    "firstName": "Marie",
    "lastName": "Dupuis",
    "speciality": "Cardiologie",
    "department": "Cardiologie",
    "experience": 10
  }
]
```

---

## 🔗 Ressources Utiles

- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [JJWT GitHub](https://github.com/jwtk/jjwt)
- [Spring Boot Tutorial](https://openclassrooms.com/fr/courses/6900101-creez-une-application-java-avec-spring-boot)
- [Angular Tutorial](https://openclassrooms.com/en/courses/7471261-debutez-avec-angular)

---

## 📧 Support

Pour toute question, consultez la documentation ou créez une issue.
