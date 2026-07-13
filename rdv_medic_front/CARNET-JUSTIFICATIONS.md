# Carnet de justifications — révision orale (Frontend Angular)

Format Q/R, alimenté au fil des sessions. Chaque réponse renvoie au fichier réel. Pendant du carnet backend : [rdv-api/CARNET-JUSTIFICATIONS.md](../rdv-api/CARNET-JUSTIFICATIONS.md).

---

## Messagerie (chat par rendez-vous)

**Q : Pourquoi le chat est-il organisé "par rendez-vous" (un fil par `slotId`) et pas "par médecin" (une conversation qui regroupe tous les échanges avec un praticien) ?**
R : C'est le modèle imposé par l'API réelle (`GET`/`POST /slots/{id}/messages`), pas un choix arbitraire du front. Un échange médical se rattache à une consultation précise — le contexte clinique d'un message n'a de sens que rapporté au RDV qui l'a motivé, pas à une relation patient↔médecin générale qui mélangerait plusieurs épisodes de soins sans distinction. Le mock initial (`MOCK_CONVERSATIONS`, un fil par `participantId`) ne correspondait à aucun endpoint côté serveur — il fallait donc aligner le front sur l'API, pas l'inverse.
Fichiers : [message.model.ts](src/app/model/message.model.ts), [api.service.ts](src/app/services/api.service.ts)

**Q : Pourquoi `Message.id` est typé `string` et pas `number` ?**
R : L'API renvoie l'`id` MongoDB natif (`ObjectId`, ex. `"6a53d664411631abe9910eb6"`) — une chaîne hexadécimale de 24 caractères, pas un entier auto-incrémenté comme les ids SQL du reste de l'app (`Slot.id`, `Doctor.id`...). Le typer `number` aurait été un mensonge de type : soit `NaN` à la conversion, soit une troncature silencieuse. C'est un piège classique quand on réutilise un modèle pensé pour du SQL sur une collection Mongo.
Fichier : [message.model.ts](src/app/model/message.model.ts)

**Q : Pourquoi `Message` n'a pas de champ `senderName` ni `read` ?**
R : Parce que l'API ne les fournit pas — `MessageDTO` (backend) ne contient que `id`, `slotId`, `senderId`, `content`, `sentAt`. Les ajouter au modèle TypeScript aurait créé un champ qui vaut toujours `undefined` à l'exécution, un décalage entre le type déclaré et la donnée réelle. `senderName` se déduirait, si un jour un affichage utile le demande, des infos déjà présentes sur la page qui ouvre le fil (`slot.doctor`/`slot.patient`) plutôt que d'un champ Mongo qui n'existe pas. `read`/`unreadCount` n'ont simplement pas de contrepartie serveur : Mongo ne stocke aucune notion de lu/non-lu ici.
Fichier : [message.model.ts](src/app/model/message.model.ts)

**Q : Comment le composant sait-il qu'un message "est le mien", sans que l'API ne le précise ?**
R : En comparant `msg.senderId` au `userId` extrait du payload du JWT décodé côté client (`AuthService.getUserId()`), qui réutilise le même décodage base64url déjà écrit pour `isAuthenticated()`. C'est un affichage pur (aligner la bulle à gauche ou à droite), jamais une décision d'autorisation : le back revalide systématiquement l'ownership à chaque requête (`403` si l'utilisateur n'est ni le patient ni le médecin du RDV), donc rien ne dépend côté sécurité de ce que le front croit avoir décodé. Ce choix évite aussi un appel API supplémentaire (ex. `GET /users/me`) juste pour un affichage.
Fichiers : [auth.service.ts](src/app/services/auth.service.ts) (`getUserId`), [messagerie.component.ts](src/app/component/messagerie/messagerie.component.ts) (`isMine`)

**Q : Pourquoi recharger toute la liste après un envoi (`loadMessages()`) plutôt que d'ajouter le message localement (mise à jour optimiste) ?**
R : Un ajout optimiste obligerait à construire un `Message` factice côté client (quel `id` lui donner avant que Mongo n'en génère un ? quel `sentAt` avant l'horodatage serveur ?) puis à le remplacer silencieusement si la réponse diffère — une source de bugs d'affichage (doublons, id qui change sous les yeux) pour un gain de rapidité non demandé. Recharger depuis le serveur après le `POST` garantit que l'écran affiche exactement ce qui est enregistré en base, avec un coût négligeable (une requête `GET` de plus sur un fil qui ne contient jamais des milliers de messages). C'est aussi la même logique que "pas de WebSocket, pas de polling" côté backend : rester simple, REST, explicable.
Fichier : [messagerie.component.ts](src/app/component/messagerie/messagerie.component.ts) (`sendMessage`)

**Q : Pourquoi deux champs d'erreur séparés (`loadError` / `sendError`) plutôt qu'un seul `errorMessage` ?**
R : Les deux échecs ne doivent pas produire le même écran. Si le **chargement** du fil échoue (403 : RDV qui n'est pas le sien : 404 : RDV inexistant), il n'y a rien de valable à afficher ni de raison de laisser un champ de saisie actif — l'utilisateur n'a pas accès à ce fil, point. Si un **envoi** échoue (ex. coupure réseau ponctuelle), le fil déjà chargé reste affiché et seul un bandeau d'erreur apparaît au-dessus du champ de saisie, pour permettre de réessayer sans perdre le contexte. Un seul champ partagé aurait forcé un compromis d'affichage bancal entre ces deux cas.
Fichiers : [messagerie.component.ts](src/app/component/messagerie/messagerie.component.ts), [messagerie.component.html](src/app/component/messagerie/messagerie.component.html)

**Q : Pourquoi les messages d'erreur affichés sont-ils des phrases ("Vous n'êtes pas participant à ce rendez-vous.") et pas les codes HTTP bruts (403, 404) ?**
R : Un code HTTP n'a de sens que pour un développeur qui lit une console réseau — un patient ou un médecin qui voit "Error 403" ne sait pas s'il doit réessayer, appeler le support, ou si c'est normal. `errorLabel()` traduit chaque statut HTTP significatif en phrase actionnable, cohérente avec le comportement réel de l'API documenté côté backend (403 = pas participant à ce RDV, 404 = RDV introuvable). Le cas générique (réseau, 5xx...) retombe sur un message neutre ("Impossible de charger/d'envoyer...") plutôt que d'exposer un détail technique inutile à l'utilisateur final.
Fichier : [messagerie.component.ts](src/app/component/messagerie/messagerie.component.ts) (`errorLabel`)

**Q : Pourquoi la route est `/rdv/:slotId/messages` et pas `/messages/:slotId` ou un `/messages` avec query param ?**
R : `/rdv/:slotId/messages` reflète la hiérarchie réelle : le fil de discussion est une sous-ressource d'un rendez-vous précis, pas une entité "message" autonome qu'on filtrerait après coup. C'est la même construction que l'API backend (`/slots/{id}/messages`), ce qui rend le mapping URL front ↔ endpoint back immédiat à expliquer. Un query param (`?slotId=...`) aurait aussi fonctionné techniquement, mais aurait suggéré à tort que la page peut exister sans `slotId` (ex. avec une valeur par défaut) — alors qu'un fil de messagerie n'a justement aucun sens hors du contexte d'un RDV précis.
Fichier : [app.routes.ts](src/app/app.routes.ts)

**Q : Pourquoi avoir retiré le lien "Messages" de la barre de navigation latérale ?**
R : Dans l'ancien modèle (conversation par médecin), ce lien menait à une liste de conversations existantes — une destination sensée sans paramètre. Dans le modèle par-RDV, une route `/rdv/:slotId/messages` sans `slotId` connu n'a pas de contenu à afficher : il n'existe pas de "liste de tous mes fils de discussion" côté API à parcourir de façon générique. Le point d'entrée naturel est donc le RDV concerné lui-même : un bouton "Message" sur chaque créneau dans `mes-rdv` (patient) et `mon-planning` (médecin), qui connaît déjà le `slotId` au moment du clic. Garder un lien de nav générique aurait été un menu qui pointe vers une page cassée ou vide par construction.
Fichiers : [app.html](src/app/app.html), [mes-rdv.component.html](src/app/component/mes-rdv/mes-rdv.component.html), [mon-planning.component.html](src/app/component/mon-planning/mon-planning.component.html)

**Q : Pourquoi le bouton "Message" apparaît sur tous les statuts de RDV du patient (y compris `COMPLETED`), mais est masqué sur les indisponibilités du médecin ?**
R : Deux contextes différents. Sur `mes-rdv`, chaque créneau listé (via `GET /patients/me/slots`) appartient structurellement au patient connecté, quel que soit son statut — relire les échanges d'une consultation terminée reste légitime sur une appli médicale (ex. retrouver les consignes post-consultation). Sur `mon-planning`, en revanche, certains créneaux sont des indisponibilités posées par le médecin lui-même (`slot.patient` absent, `isUnavailability(slot)`) — il n'y a alors ni second participant ni fil possible, ce qui correspond exactement à la règle documentée côté API (404 sur un slot sans patient). Le bouton y est donc gardé par `@if (slot.patient)`, condition qui n'a pas lieu d'être côté patient puisqu'elle y est toujours vraie.
Fichiers : [mes-rdv.component.html](src/app/component/mes-rdv/mes-rdv.component.html), [mon-planning.component.html](src/app/component/mon-planning/mon-planning.component.html), [mon-planning.component.ts](src/app/component/mon-planning/mon-planning.component.ts) (`isUnavailability`)

**Q : Pourquoi pas de WebSocket ni de polling pour rafraîchir le fil automatiquement ?**
R : Aucun besoin exprimé de temps réel, et le backend a fait le même choix assumé (voir `rdv-api/CARNET-JUSTIFICATIONS.md`, section chat). Un WebSocket (ou un `setInterval` de polling) ajouterait un cycle de vie à gérer (connexion, reconnexion, désabonnement à la destruction du composant) pour un gain que rien ne justifie ici — le chat se recharge au montage du composant et après un envoi, ce qui suffit à un usage de messagerie asynchrone entre un patient et un médecin. Ajouter du temps réel non demandé serait de la sur-ingénierie difficilement défendable à l'oral ("pourquoi ce mécanisme existe-t-il ?").
Fichier : [messagerie.component.ts](src/app/component/messagerie/messagerie.component.ts)

---

## Suppression de l'assistant IA

**Q : Pourquoi `assistant-ia.component` a-t-il été supprimé plutôt que laissé en l'état ou juste débranché de la navigation ?**
R : Aucun endpoint backend n'existe ni n'est prévu pour cette fonctionnalité dans le référentiel de certification — le composant était intégralement mocké (`MOCK_RESPONSES`, un dictionnaire mot-clé → réponse statique) sans plan de le brancher un jour. Un jury qui verrait ce composant demanderait forcément "quel est le backend derrière ?", question sans réponse défendable puisqu'il n'y en a pas. Le laisser accessible via la nav aurait aussi été trompeur pour un utilisateur de l'app, qui croirait parler à un vrai assistant. La suppression complète (fichiers, route, import, lien nav) est plus honnête qu'un débranchement partiel qui laisserait du code mort dans le dépôt.
Suppression : dossier `component/assistant-ia/` (3 fichiers), route `assistant` dans [app.routes.ts](src/app/app.routes.ts), lien nav dans [app.html](src/app/app.html)
