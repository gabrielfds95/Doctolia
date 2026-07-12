# ── Stage 1 : build ──────────────────────────────────────────────────────────
# Image avec Maven + JDK 17, utilisée uniquement pour compiler ; n'est PAS
# présente dans l'image finale (donc son poids n'impacte pas le déploiement).
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copie du pom.xml seul d'abord : permet à Docker de mettre en cache la couche
# de téléchargement des dépendances Maven tant que le pom.xml ne change pas
# (rebuild bien plus rapide quand seul le code source change).
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline

COPY src src
RUN ./mvnw -q clean package -DskipTests

# ── Stage 2 : exécution ──────────────────────────────────────────────────────
# Image minimale : uniquement un JRE (pas de JDK, pas de Maven) → image plus
# petite et surface d'attaque réduite pour la prod.
# jammy (Ubuntu), pas alpine : meilleure disponibilité multi-plateforme
# (l'image alpine n'a pas de manifeste pour toutes les architectures).
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Utilisateur non-root : ne jamais faire tourner le process applicatif en root
# dans un conteneur (principe de moindre privilège, même à l'intérieur du conteneur).
RUN groupadd -r rdvapi && useradd -r -g rdvapi rdvapi
USER rdvapi

COPY --from=build /app/target/*.jar app.jar

EXPOSE 9000

ENTRYPOINT ["java", "-jar", "app.jar"]
