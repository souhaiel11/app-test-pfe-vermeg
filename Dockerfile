# APPLICATION DE DÉMONSTRATION INTENTIONNELLEMENT VULNÉRABLE — USAGE DE TEST UNIQUEMENT.
# Ne jamais publier ce service sur un port exposé à Internet.
#
# Images de base épinglées par digest, pas par tag : un tag flottant rendrait le build
# non reproductible et ferait varier les résultats de scan d'un jour à l'autre, sans
# qu'aucun commit n'ait changé.

# ── Étape 1 — construction (Maven 3.9.9, JDK 17.0.15) ────────────────────────────
FROM docker.io/library/maven@sha256:82e47241881f23ad774f5db8829efca15758e8fdb5b1d64ea9f8d6420a85068e AS builder
WORKDIR /build
# pom.xml d'abord : la couche de dépendances est mise en cache tant qu'il ne change pas.
COPY pom.xml ./
COPY src/ ./src/
# `verify` et non `package -DskipTests` : une image ne doit pas pouvoir être produite
# à partir d'un code dont les tests échouent.
RUN mvn -B -ntp clean verify

# ── Étape 2 — exécution (JRE 17 Alpine) ──────────────────────────────────────────
FROM docker.io/library/eclipse-temurin@sha256:3c472129dc75a8d1d7a3f2df5b2093a8077e4493deff046754d4764d0371de63
LABEL org.opencontainers.image.title="app-test-pfe-vermeg" \
      org.opencontainers.image.description="APPLICATION DE DEMONSTRATION INTENTIONNELLEMENT VULNERABLE — USAGE DE TEST UNIQUEMENT ; donnees synthetiques ; test local isole" \
      org.opencontainers.image.vendor="PFE 2026 — demonstration DevSecOps"
WORKDIR /app

# Les jars de dépendances sont copiés dans l'image, et c'est volontaire : Trivy scanne
# l'image, pas le dépôt. Sans ces jars, son analyseur Java n'a rien à signaler et la
# fermeture de sécurité double-scanner devient impossible à prouver.
COPY --from=builder /build/target/runtime/ /app/dependencies/
COPY --from=builder /build/target/app-test-pfe-vermeg-1.0.0.jar /app/application.jar

# Utilisateur non privilégié, par identifiant numérique : aucune dépendance à
# l'existence d'un compte dans l'image de base.
USER 65532:65532
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["/opt/java/openjdk/bin/java", "-Dapp.bind=0.0.0.0", \
            "-cp", "/app/application.jar:/app/dependencies/*", "tn.pfe.demo.App"]
