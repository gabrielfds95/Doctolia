# Carnet de justifications — révision oral

Format Q/R, alimenté au fil des sessions. Chaque réponse renvoie au fichier réel.

---

## Architecture / Modèle

**Q : Pourquoi les IDs sont en `Long` et pas `int`/`Integer` ?**
R : `Long` supporte des valeurs jusqu'à ~9,2 × 10¹⁸ (vs ~2,1 milliards pour `int`), suffisant pour ne jamais craindre un dépassement même avec une base de données qui grossit sur des années. C'est aussi le type que génère nativement `GenerationType.IDENTITY` avec la plupart des SGBD (auto-increment sur `BIGINT`). Utiliser `int` obligerait en plus à gérer l'auto-boxing différemment et interdirait `null` (utile pour représenter "pas encore persisté").
Fichiers : [User.java](src/main/java/com/rdvmedic/rdv_api/model/User.java), [Slot.java](src/main/java/com/rdvmedic/rdv_api/model/Slot.java)

**Q : Pourquoi `@Inheritance(strategy = JOINED)` et pas `SINGLE_TABLE` ?**
R : `JOINED` crée une table par sous-classe (`users`, `doctors`, `patients`) reliée par clé étrangère sur l'id. Avantage : chaque table ne contient que ses colonnes propres, donc pas de colonnes `NULL` en masse (ex. `ssn` n'existe pas dans `doctors`). `SINGLE_TABLE` aurait mis toutes les colonnes de toutes les sous-classes dans une seule table `users`, avec des `NULL` partout et un risque de collision de contraintes (ex. `unique` sur `license_number` qui n'a de sens que pour un médecin). Le compromis de `JOINED` est un `JOIN` SQL supplémentaire à chaque lecture — acceptable ici car pas de volumétrie critique.
Fichiers : [User.java](src/main/java/com/rdvmedic/rdv_api/model/User.java), [Doctor.java](src/main/java/com/rdvmedic/rdv_api/model/Doctor.java), [Patient.java](src/main/java/com/rdvmedic/rdv_api/model/Patient.java)

**Q : Pourquoi `@SuperBuilder` sur `User`/`Doctor`/`Patient` et pas `@Builder` ?**
R : `@Builder` (Lombok) génère un builder qui ne connaît que les champs de sa propre classe — il ignore les champs hérités de la superclasse. Sur une hiérarchie (`Doctor extends User`), `Doctor.builder().username(...)` ne compile pas avec `@Builder` car `username` est dans `User`. `@SuperBuilder` génère un builder qui chaîne l'appel au builder parent, donc `Doctor.builder().username(...).speciality(...).build()` fonctionne. `Slot` et `Document` n'héritent de rien → `@Builder` simple suffit pour eux.
Fichiers : [User.java](src/main/java/com/rdvmedic/rdv_api/model/User.java), [Doctor.java](src/main/java/com/rdvmedic/rdv_api/model/Doctor.java)

---

## Sécurité

**Q : Pourquoi JWT et pas une session serveur classique ?**
R : Le backend sert à la fois une app Angular web et une app mobile React Native — deux clients qui n'ont pas de notion native de "cookie de session" partagée. Un JWT est auto-porteur : il contient l'identité (`sub`), l'id BDD (`userId`) et les rôles (`roles`), signé en HMAC-SHA256. Le serveur n'a donc rien à stocker (`SessionCreationPolicy.STATELESS`) — chaque requête est vérifiable indépendamment avec juste la clé secrète. C'est aussi ce qui permet de scale horizontalement sans partager un store de sessions entre instances.
Fichiers : [JwtTokenProvider.java](src/main/java/com/rdvmedic/rdv_api/security/JwtTokenProvider.java), [SecurityConfig.java](src/main/java/com/rdvmedic/rdv_api/config/SecurityConfig.java)

**Q : Pourquoi CSRF est désactivé (`csrf.disable()`) ?**
R : CSRF protège les flux authentifiés par cookie de session, où le navigateur envoie automatiquement le cookie sur toute requête (même cross-site). Ici, l'authentification se fait via un header `Authorization: Bearer <token>` que le navigateur n'attache jamais automatiquement — un site tiers ne peut pas forcer son envoi. La protection CSRF n'a donc pas d'objet dans une architecture JWT stateless.
Fichier : [SecurityConfig.java:72](src/main/java/com/rdvmedic/rdv_api/config/SecurityConfig.java#L72)

**Q : En quoi Spring Data JPA protège contre l'injection SQL ?**
R : Toutes les requêtes du projet passent soit par des méthodes dérivées (`findByUsername`, `findByDoctorId`...) soit par le JPQL généré automatiquement — jamais par de la concaténation de chaînes SQL. Spring Data transforme ces appels en requêtes **préparées** (`PreparedStatement`), où les valeurs (username, id...) sont passées comme paramètres liés (`?`), jamais interpolées dans le texte de la requête. Un attaquant ne peut donc pas injecter de SQL via un champ texte, car ce champ est toujours traité comme une donnée, jamais comme du code SQL.
Vérifié : aucune occurrence de `createNativeQuery`, `createQuery` avec concaténation, ni de `@Query` dans tout le code (`grep` sur `src/main/java`).

**Q : Pourquoi BCrypt et pas MD5/SHA-256 nu ?**
R : MD5/SHA sont des fonctions de hachage rapides, conçues pour l'intégrité de données, pas pour les mots de passe — un attaquant peut tester des milliards de combinaisons par seconde (brute-force/rainbow tables). BCrypt est volontairement lent (facteur de coût configurable, 10 rounds par défaut) et **salé automatiquement** : deux utilisateurs avec le même mot de passe obtiennent des hashs différents, ce qui neutralise les rainbow tables.
Fichier : [SecurityConfig.java:46-49](src/main/java/com/rdvmedic/rdv_api/config/SecurityConfig.java#L46-L49)

---

## À compléter en Phase 1+

- Pourquoi MongoDB pour le chat et relationnel pour le métier ? *(à rédiger en Phase 3)*
- Ownership sur les slots (patient/médecin) : décision et implémentation *(à rédiger en Phase 1)*
