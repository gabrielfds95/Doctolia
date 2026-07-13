# Contexte de passation — session frontend Angular

Document autoportant pour une nouvelle session Claude Code, ouverte à la racine du monorepo (`Doctolia/`), chargée de terminer le frontend Angular (`rdv_medic_front/`). Écrit depuis la session backend (`rdv-api/`), qui vient d'être **gelée** — plus aucune modification n'y sera faite. Tout ce qui suit est vérifié sur le code réel, rien n'est inventé.

---

## 1. Contexte projet

**Doctolia** — application de prise de rendez-vous médicaux (type Doctolib), développée dans le cadre d'une **certification CDA** (Concepteur Développeur d'Applications). L'étudiant passe un oral où un jury le questionne en détail sur le code produit : toute décision doit être explicable simplement, sans sur-ingénierie.

Monorepo :
```
Doctolia/
├── rdv-api/          Backend Spring Boot — GELÉ, ne plus toucher
├── rdv_medic_front/  Frontend Angular 20.2 — PÉRIMÈTRE DE CETTE SESSION
└── doctolia-mobile/  App React Native (Expo) — HORS PÉRIMÈTRE, ne pas y toucher
```

Le backend a traversé plusieurs phases cette session (sécurité, chat MongoDB, déploiement Docker/CI, documentation UML) — tout est documenté et vérifié, voir section 8. Le frontend, lui, n'a pas suivi : deux composants sont encore intégralement mockés alors que leur backend existe (chat) ou n'existe pas du tout côté API (assistant IA).

---

## 2. Contrat d'API

**URL de base** : `http://localhost:9000` — actuellement codée en dur dans `rdv_medic_front/src/app/services/api.service.ts` (`baseURL = 'http://localhost:9000'`), pas de fichier `environment.ts` utilisé pour ça. Le backend tourne en local via `cd rdv-api && ./mvnw spring-boot:run` (profil `dev`, H2 en mémoire) ; pour tester le chat, MongoDB doit aussi tourner : `docker compose up -d mongo` depuis `rdv-api/`.

**Authentification** : JWT dans le header `Authorization: Bearer <token>`.

Structure réelle du token (vérifiée par décodage) :
```json
{
  "header": { "alg": "HS384" },
  "payload": {
    "sub": "pat.marc",      // username
    "userId": 4,             // id BDD — c'est CET id qui détermine l'ownership côté serveur
    "roles": "ROLE_PATIENT", // string, pas un tableau (peut contenir plusieurs rôles séparés par virgule)
    "iat": 1783866501,
    "exp": 1783867401
  }
}
```
- **Durée de vie : 15 minutes** (900000 ms). **Pas de refresh token** — le champ `refreshToken` existe dans la réponse de login/register mais vaut toujours `null`, aucun endpoint `/refresh` n'existe. À l'expiration, seule solution : se reconnecter.
- Comptes de démo (recréés à chaque redémarrage du backend, mot de passe `password` pour tous) : `admin`, `doc.john`, `doc.paul`, `pat.marc`, `pat.jean`.

**CORS** : origines autorisées explicitement = `http://localhost:4200` (Angular dev) et `http://localhost:8081` (mobile, hors périmètre ici). Toute autre origine sera bloquée par le navigateur.

---

## 3. Les endpoints du chat, en détail

Le chat est backé par **MongoDB** (collection `messages`, pas de table SQL) — c'est un choix assumé pour démontrer la compétence NoSQL du référentiel, détaillé dans `CARNET-JUSTIFICATIONS.md` (section Phase 3) si le jury demande pourquoi.

### `GET /slots/{id}/messages`
- **Auth requise** : oui, `Authorization: Bearer <token>` (n'importe quel rôle authentifié — PATIENT ou DOCTOR).
- **`{id}`** = l'id du `Slot` (le rendez-vous), **pas** l'id d'un médecin ni d'un patient.
- **200 OK** — `List<MessageDTO>`, triée par ordre chronologique croissant (`sentAt` ASC).
- **403 Forbidden** — l'utilisateur authentifié n'est NI le patient NI le médecin de ce RDV précis. Body : `{"timestamp":"...","status":403,"error":"Forbidden","message":"Vous n'êtes pas participant à ce rendez-vous."}`
- **404 Not Found** — le slot n'existe pas, OU c'est une indisponibilité médecin sans patient (pas un vrai RDV, donc pas de "2 participants" possibles).
- **401 Unauthorized** — pas de token ou token invalide/expiré.

### `POST /slots/{id}/messages`
- Mêmes règles d'auth/ownership que GET (403/404/401 identiques).
- **Body attendu** (`MessageCreateDTO`) :
  ```json
  { "content": "Bonjour docteur" }
  ```
  Validation : `content` obligatoire (`@NotBlank`), max 2000 caractères (`@Size(max=2000)`) → **400 Bad Request** sinon, avec un message d'erreur exploitable.
- **201 Created** — retourne le `MessageDTO` créé. `senderId` et `sentAt` sont **fixés côté serveur** (jamais fournis par le client) : `senderId` = id extrait du JWT, `sentAt` = horodatage serveur au moment de l'insertion.

### `MessageDTO` — forme exacte de la réponse (exemple réel, capturé via curl)
```json
{
  "id": "6a53d664411631abe9910eb6",
  "slotId": 1,
  "senderId": 4,
  "content": "Bonjour docteur, a quelle heure exactement ?",
  "sentAt": "2026-07-12T20:01:08.602677"
}
```
| Champ | Type TypeScript | Remarque |
|---|---|---|
| `id` | `string` | ObjectId MongoDB, **pas un number** — piège classique si on réutilise un modèle `Message` typé `id: number` (c'est le cas du modèle mock actuel, voir section 5) |
| `slotId` | `number` | id du `Slot` (SQL) |
| `senderId` | `number` | id de `User` (SQL) — à comparer au `userId` du JWT décodé pour savoir si "c'est moi" côté UI |
| `content` | `string` | texte du message |
| `sentAt` | `string` | ISO 8601 sans timezone explicite (`LocalDateTime` Java) |

### ⚠️ Écart de modèle important entre le mock actuel et l'API réelle
Le modèle actuel (`rdv_medic_front/src/app/model/message.model.ts`) :
```ts
export interface Message {
  id: number;          // ⚠️ l'API réelle renvoie un STRING (ObjectId Mongo)
  senderId: number;
  senderName: string;  // ⚠️ n'existe pas côté API — à dériver ailleurs (ex. GET /users/me, GET /doctor/{id})
  content: string;
  sentAt: string;
  read: boolean;        // ⚠️ n'existe pas côté API — pas de notion de lu/non-lu dans Message (Mongo)
}
export interface Conversation {
  id: number;
  participantId: number;   // = id d'un DOCTEUR — une conversation regroupe tous ses échanges avec CE médecin
  participantName: string;
  participantRole: 'DOCTOR' | 'PATIENT';
  participantInitials: string;
  lastMessage: string;
  lastMessageAt: string;
  unreadCount: number;
  messages: Message[];
}
```
Le composant modélise donc une **conversation par médecin**. L'API réelle modélise le chat **par rendez-vous** (`/slots/{id}/messages` — un fil de discussion par `Slot`, entre le patient et le médecin de CE créneau précis, et uniquement eux — pas par relation patient↔médecin en général). Ce n'est donc pas un simple remplacement `MOCK_CONVERSATIONS` → `fetch` : le modèle de données (`id: string`, pas de `read`/`senderName` fournis par l'API) et la navigation (comment on arrive sur le chat d'un RDV donné — depuis `mes-rdv`/`mon-planning`, avec un `slotId`) doivent être repensés. C'est précisément pourquoi la méthode demandée (section 7) est "scanner puis stop", pas "foncer".

---

## 4. Règles de sécurité côté front (rappel)

**Le front ne sécurise rien — le back revalide systématiquement tout.** Concrètement :
- Ne jamais faire confiance à un rôle/id décodé du JWT côté client pour *autoriser* une action UI-side — décoder le JWT sert seulement à l'**affichage** (ex. "c'est moi" sur une bulle de message, masquer un bouton). Si l'action n'est pas permise, l'API renverra 403 de toute façon ; il faut juste bien gérer ce cas (message utilisateur clair, pas de crash).
- Toujours envoyer le token via l'intercepteur existant (`Authorization: Bearer`) — ne pas réinventer un mécanisme d'attache de header pour le chat.
- Gérer explicitement les 4 codes de retour possibles (200/201, 400, 401, 403, 404) — pas juste le cas heureux.
- Le 401 est déjà intercepté globalement (`error.interceptor.ts`, déconnexion automatique) — vérifier que ça s'applique aussi aux appels chat, ne pas dupliquer cette logique dans le composant.
- Ne jamais construire une URL `/slots/{id}/messages` avec un `id` que l'utilisateur pourrait modifier librement dans un contexte où il ne devrait pas avoir accès à ce RDV — l'UI doit naturellement n'exposer que les RDV de l'utilisateur connecté (via `mes-rdv`/`mon-planning`, déjà correctement scopés côté API), pas laisser deviner des ids.

---

## 5. La mission

**(a) Supprimer `assistant-ia.component`** — mock hors périmètre du projet (aucun backend, jamais prévu dans le référentiel de certification). Fichiers : `src/app/component/assistant-ia/` (3 fichiers : `.ts`/`.html`/`.scss`). Références à retirer aussi : la route `{ path: 'assistant', ... }` dans `app.routes.ts`, l'import `AssistantIaComponent`, et le lien de navigation dans `app.html` (à localiser).

**(b) Brancher `messagerie.component` sur la vraie API chat.** Objectifs :
- **Zéro mock résiduel** : `MOCK_CONVERSATIONS` doit disparaître entièrement, tous les échanges passent par `GET`/`POST /slots/{id}/messages`.
- **États explicites** : chargement (loading), erreur (échec réseau, 403, 404), vide (aucun message pour l'instant sur ce RDV) — pas juste le cas où tout va bien.
- **Pas de WebSocket, pas de polling** — REST simple (charger au montage, recharger après un envoi). C'est un choix déjà assumé et justifié côté backend (voir `CARNET-JUSTIFICATIONS.md`, "Pourquoi pas de WebSocket/STOMP") : rester cohérent avec cette décision plutôt que d'ajouter du temps réel non demandé.
- Résoudre l'écart de modèle décrit en section 3 (conversation-par-médecin actuelle vs chat-par-RDV réel) — c'est une vraie décision de conception à prendre, pas un détail.

---

## 6. Règle "code maîtrisé" (non négociable)

Un jury questionne l'étudiant **45 minutes** sur ce code à l'oral. En conséquence :
- Jamais de code que l'étudiant ne pourrait pas expliquer ligne par ligne.
- Pas de sur-ingénierie : pas de pattern/librairie/abstraction non justifiée par le besoin réel (ex. pas de state management global type NgRx pour un simple fil de chat, pas de WebSocket non demandé — voir section 5).
- Chaque bloc de code livré doit être accompagné d'une explication *quoi + pourquoi ce choix plutôt qu'une alternative*.
- **Alimenter un carnet de justifications** au fil de l'eau (format Q/R), pour la révision orale. Le backend a le sien : `rdv-api/CARNET-JUSTIFICATIONS.md`. À décider en démarrant : soit y ajouter une section "Frontend", soit créer `rdv_medic_front/CARNET-JUSTIFICATIONS.md` dédié — les deux sont défendables, mais il en faut UN, pas zéro.

---

## 7. Méthode imposée

1. **Scan ciblé d'abord, ne rien coder tout de suite.** Lire précisément :
   - `rdv_medic_front/src/app/component/messagerie/` (les 3 fichiers)
   - `rdv_medic_front/src/app/component/assistant-ia/` (les 3 fichiers, pour la suppression propre)
   - `rdv_medic_front/src/app/services/api.service.ts` (pattern existant des autres méthodes HTTP, pour rester cohérent en ajoutant les méthodes chat)
   - `rdv_medic_front/src/app/model/message.model.ts` (le modèle `Conversation`/`Message` actuel, incompatible avec l'API réelle — voir section 3)
   - `rdv_medic_front/src/app/app.routes.ts` et `app.html` (routes et nav à mettre à jour)
   - `rdv_medic_front/src/app/interceptors/auth.interceptor.ts` (attache le JWT) et `interceptors/error.interceptor.ts` (gestion 401 déjà en place)
2. **Synthèse** : ce qui existe / ce qui doit changer / la décision de modèle de données (section 3) / une proposition concrète.
3. **Stop.** Attendre la validation de l'utilisateur avant de coder quoi que ce soit — même règle que celle suivie côté backend tout au long de cette certification.

---

## 8. Où trouver le reste (backend, gelé, référence uniquement)

Tout dans `rdv-api/`, chemins relatifs à la racine du monorepo :

| Besoin | Fichier |
|---|---|
| Liste exhaustive des 25 endpoints (méthode/URI/rôle/ownership) | `rdv-api/docs/endpoints.md` |
| Guide de test manuel via curl (dont le chat) | `rdv-api/README.md`, section "Guide de test manuel via l'API (curl)" |
| Audit de sécurité complet (4 failles corrigées, preuves avant/après) | `rdv-api/AUDIT-SECURITE.md` |
| Justifications techniques backend (fiche de révision orale) | `rdv-api/CARNET-JUSTIFICATIONS.md` |
| Diagrammes UML (classes, séquence, déploiement, cas d'usage) + schéma relationnel | `rdv-api/docs/*.puml`, `rdv-api/docs/schema-relationnel.md` |
| État réel du frontend (ce qui marche vs mocké) | `README.md` (racine du monorepo), section "Ce qui fonctionne aujourd'hui" |

**Ne pas modifier `rdv-api/`** — c'est gelé. Si un besoin de changement backend apparaît (ex. un DTO qui manquerait un champ utile au front), le documenter/signaler à l'utilisateur plutôt que de le faire directement.
