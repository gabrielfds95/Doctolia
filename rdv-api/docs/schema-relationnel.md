# Schéma relationnel — rdv-api

Reconstitué à partir des entités JPA réelles (`src/main/java/.../model/*.java`) et de la stratégie `@Inheritance(strategy = JOINED)` sur `User`. 8 tables au total, générées par Hibernate (`spring.jpa.hibernate.ddl-auto=update`, pas de script SQL manuel — voir `CARNET-JUSTIFICATIONS.md`). Confirmé en conditions réelles sur PostgreSQL (`\dt` + `SELECT`, voir Phase 5).

## Vue d'ensemble (MCD simplifié)

```
roles ──┐
        │ user_roles (table de jointure N-N)
users ──┘
  │
  ├── (JOINED) doctors
  ├── (JOINED) patients
  └── (JOINED) admins

doctors ──1───N── slots ──N───0..1── patients
                                        │
                                        └──1───N── documents
```

## Dictionnaire de données

### `roles`
| Colonne | Type | Contraintes |
|---|---|---|
| `id` | BIGINT | PK, auto-incrémenté |
| `name` | VARCHAR(20) | valeurs : `ROLE_PATIENT`, `ROLE_DOCTOR`, `ROLE_ADMIN` |
| `description` | TEXT | nullable |

### `users` (table mère de la hiérarchie JOINED)
| Colonne | Type | Contraintes |
|---|---|---|
| `id` | BIGINT | **PK**, auto-incrémenté |
| `username` | VARCHAR | **UNIQUE**, NOT NULL |
| `email` | VARCHAR | **UNIQUE**, NOT NULL |
| `password` | VARCHAR | NOT NULL (hash BCrypt) |
| `first_name` | VARCHAR | nullable |
| `last_name` | VARCHAR | nullable |
| `user_type` | VARCHAR | colonne *discriminator* (`@DiscriminatorColumn`) — `PATIENT`/`DOCTOR`/`ADMIN`, non modifiable par l'app (`insertable=false, updatable=false`) |
| `enabled` | BOOLEAN | NOT NULL, défaut `true` |
| `created_at` | TIMESTAMP | NOT NULL, généré à l'insertion (`@CreationTimestamp`) |
| `updated_at` | TIMESTAMP | nullable, mis à jour à chaque modification (`@PreUpdate`) |

### `user_roles` (table de jointure, relation N-N `users` ↔ `roles`)
| Colonne | Type | Contraintes |
|---|---|---|
| `user_id` | BIGINT | **FK** → `users.id` |
| `role_id` | BIGINT | **FK** → `roles.id` |

### `doctors` (JOINED sur `users`)
| Colonne | Type | Contraintes |
|---|---|---|
| `id` | BIGINT | **PK**, **FK** → `users.id` (même valeur que la ligne `users` correspondante) |
| `speciality` | VARCHAR | nullable |
| `license_number` | VARCHAR | **UNIQUE**, nullable |
| `department` | VARCHAR | nullable |
| `experience_years` | INTEGER | nullable |

### `patients` (JOINED sur `users`)
| Colonne | Type | Contraintes |
|---|---|---|
| `id` | BIGINT | **PK**, **FK** → `users.id` |
| `ssn` | VARCHAR | nullable (NIR) |
| `phone_number` | VARCHAR | nullable |
| `address` | VARCHAR | nullable |
| `age` | INTEGER | NOT NULL (primitif `int`, défaut 0) |

### `admins` (JOINED sur `users`)
| Colonne | Type | Contraintes |
|---|---|---|
| `id` | BIGINT | **PK**, **FK** → `users.id` |
| *(aucune colonne propre)* | | |

### `slots`
| Colonne | Type | Contraintes |
|---|---|---|
| `id` | BIGINT | **PK**, auto-incrémenté |
| `slot_date` | DATE | NOT NULL |
| `slot_time` | TIME | NOT NULL |
| `end_time` | TIME | nullable |
| `slot_reason` | VARCHAR | nullable (motif de consultation) |
| `status` | VARCHAR | NOT NULL — `AVAILABLE` / `RESERVED` / `CANCELLED` / `COMPLETED` |
| `doctor_id` | BIGINT | **FK** → `doctors.id`, **NOT NULL** |
| `patient_id` | BIGINT | **FK** → `patients.id`, **NULLABLE** — `null` = indisponibilité médecin, non-null = RDV réel |

### `documents`
| Colonne | Type | Contraintes |
|---|---|---|
| `id` | BIGINT | **PK**, auto-incrémenté |
| `name` | VARCHAR | NOT NULL |
| `type` | VARCHAR | NOT NULL (ex. PDF, JPEG) |
| `file_url` | VARCHAR | NOT NULL |
| `description` | TEXT | nullable |
| `uploaded_at` | TIMESTAMP | NOT NULL, généré à l'insertion |
| `patient_id` | BIGINT | **FK** → `patients.id`, NOT NULL |

⚠️ **Table présente en base, mais sans controller** : l'entité et le repository existent (`DocumentRepository`), mais aucun endpoint HTTP ne l'expose. Assumé hors périmètre (Phase 2 non traitée) — voir dossier de certification.

## Relations (cardinalités)

| Relation | Cardinalité | Portée par |
|---|---|---|
| `users` 1 — 1 `doctors` / `patients` / `admins` | héritage JOINED (1 ligne mère ↔ 0 ou 1 ligne fille selon le type réel) | `users.id` = PK partagée |
| `users` N — N `roles` | via `user_roles` | table de jointure |
| `doctors` 1 — N `slots` | un médecin a plusieurs créneaux, un créneau a exactement un médecin | `slots.doctor_id` NOT NULL |
| `patients` 1 — N `slots` | un patient a plusieurs RDV, un créneau a 0 ou 1 patient | `slots.patient_id` NULLABLE |
| `patients` 1 — N `documents` | un patient a plusieurs documents, un document appartient à un seul patient | `documents.patient_id` NOT NULL |

## Hors SQL — MongoDB (pour mémoire, pas dans ce schéma relationnel)

La collection `messages` (base `doctolia_chat`) n'est **pas** une table SQL : `Message.slotId`/`senderId` sont de simples `Long` sans contrainte de clé étrangère réelle — la cohérence référentielle est vérifiée applicativement par `MessageService`, jamais par une contrainte de base de données. Voir `docs/class-diagram.puml` et `CARNET-JUSTIFICATIONS.md` (section Phase 3) pour la justification du choix NoSQL.
