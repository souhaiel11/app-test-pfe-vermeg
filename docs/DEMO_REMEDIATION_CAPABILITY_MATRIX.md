# DEMO_REMEDIATION_CAPABILITY_MATRIX

**Phase 1.** Ce que la plateforme **déployée aujourd'hui** sait réellement remédier.
Établi par lecture du code déployé, pas par supposition. L'application de démonstration
est conçue **autour** des lignes `FULL` — et nulle part ailleurs.

## Source de vérité

| Preuve | Fichier |
|---|---|
| Portes d'éligibilité | `backend/src/security-remediation/security-eligibility-classifier.ts` |
| Choix de version cible | `backend/src/security-remediation/version-selection-policy.ts` |
| Portée de patch Maven | `backend/src/security-remediation/maven-remediation-scope.ts` |
| Écriture du patch | `maven-security-patch-writer.ts`, `maven-parent-patch-writer.ts` |
| Écrivain unique | `manual-remediation/remediation-target-reservation.ts` |
| Boîte à outils du vérificateur | `docker exec pfe-candidate-verifier` → Java 17, Maven 3.8.7 |
| Boîte à outils Jenkins | `docker exec jenkins` → Java 21, Maven 3.6.3 |
| Audit antérieur réutilisé | `testbeds/devsecops-remediation-testbed/docs/SUPPORTED_REMEDIATION_CAPABILITY_MATRIX.md` |

## Les quatre portes, vérifiées dans le code

`classifySecurityAutoFixEligibility()` n'accorde `AUTO_FIX_ELIGIBLE` que si **tout** est vrai :

1. `source ∈ {TRIVY, OWASP}` — sinon `SOURCE_NOT_SUPPORTED_V1` ;
2. `ecosystem = MAVEN` — sinon `ECOSYSTEM_NOT_SUPPORTED_V1` ;
3. `pkg`, `installedVersion` et au moins une `fixedVersion` présents — sinon fermeture ;
4. `provenance.kind ∈ {DIRECT_EXPLICIT, PROPERTY_MANAGED}`.
   `TRANSITIVE` → `TRANSITIVE_NOT_AUTOFIXABLE_V1` · `PLUGIN` → `PLUGIN_NOT_AUTOFIXABLE_V1` ·
   `BOM_MANAGED` → `ADMIN_ACTION_REQUIRED` · `UNRESOLVED` → fermeture.

Puis `selectEligibleTargetVersion()` retient **la plus basse version corrigée strictement
supérieure ET de même majeure**. Une correction qui n'existe qu'en majeure supérieure
renvoie `CROSS_MAJOR_ONLY`, cible `null` : la plateforme refuse de franchir une majeure
seule. **C'est la contrainte dimensionnante du choix des scénarios de démonstration.**

## Matrice

| PROBLEM_TYPE | DETECTOR | AUTO_REMEDIATION_SUPPORTED | TARGET_TYPE | TARGET_FILE | BUILD_SUPPORTED | TEST_SUPPORTED | RESCAN_SUPPORTED | SECURITY_CLOSURE_SUPPORTED | GIT_PUBLICATION_SUPPORTED | DEMO_SUITABILITY |
|---|---|---|---|---|---|---|---|---|---|---|
| Dépendance Maven **directe explicite**, correction même majeure | Trivy + OWASP | **FULL** | FILE | `pom.xml` racine | FULL | FULL | FULL (deux scanners) | FULL | FULL (sous conditions WF6) | **DEMO-001 — scénario principal** |
| Dépendance Maven **gérée par propriété** (`${x.version}`), même majeure | Trivy + OWASP | **FULL** | FILE | `pom.xml` racine | FULL | FULL | FULL | FULL | FULL (sous conditions WF6) | **DEMO-002 — cible indépendante** |
| Dépendance Maven, correction **majeure supérieure uniquement** | Trivy + OWASP | UNSUPPORTED (`CROSS_MAJOR_ONLY`) | FILE | `pom.xml` | FULL | FULL | FULL | — | — | à éviter en démonstration |
| Dépendance Maven **transitive** | Trivy + OWASP | UNSUPPORTED (`TRANSITIVE_NOT_AUTOFIXABLE_V1`) | — | — | FULL | FULL | FULL | — | — | contre-exemple pédagogique |
| Version de **parent** Maven | Trivy + OWASP | PARTIAL (plan V1.8 autoritatif requis) | FILE | `pom.xml` | FULL | FULL | FULL | PARTIAL | PARTIAL | hors périmètre de démonstration |
| Dépendance gérée par **BOM** | Trivy + OWASP | UNSUPPORTED (`ADMIN_ACTION_REQUIRED`) | — | — | FULL | FULL | FULL | — | — | hors périmètre |
| Édition dans un **module enfant** | Trivy + OWASP | DETECTION_ONLY (l'adaptateur n'ancre que le descripteur racine) | — | `*/pom.xml` | PARTIAL | PARTIAL | FULL | UNSUPPORTED | UNSUPPORTED | hors périmètre — pas de module enfant dans l'application |
| Dépendance **npm** | Trivy + OWASP | DETECTION_ONLY (orchestrateur Maven uniquement) | — | `package.json` | PARTIAL | PARTIAL | UNSUPPORTED | UNSUPPORTED | UNSUPPORTED | hors périmètre |
| Dépendance **Python / Gradle** | variable | DETECTION_ONLY | — | — | PARTIAL | PARTIAL | UNSUPPORTED | UNSUPPORTED | UNSUPPORTED | hors périmètre |
| Défaut de **code source** | SonarQube | DETECTION_ONLY pour la chaîne WF6 ; PARTIAL via le chemin incident/WF2 | SOURCE_FILE | fichier approuvé | FULL | FULL | PARTIAL (machinerie de régression Sonar) | UNSUPPORTED en garantie générale | PARTIAL | **DEMO-003 = DETECTION_ONLY** |
| **Image de base / paquet OS** | Trivy | UNSUPPORTED (aucune stratégie de version d'image dans WF6) | — | `Dockerfile` | FULL | FULL | FULL | UNSUPPORTED | UNSUPPORTED | détection seulement |
| **En-têtes / configuration web** | ZAP | UNSUPPORTED (aucune stratégie indépendante de la source) | — | configuration | variable | variable | hors du rescan Maven | UNSUPPORTED | UNSUPPORTED | **DEMO-004 = DETECTION_ONLY** |

## Décisions de conception qui en découlent

1. **Maven, racine, pas de module enfant.** L'adaptateur de cible n'ancre que le
   descripteur racine. Un `pom.xml` unique, à la racine.
2. **Deux positifs, deux provenances différentes.** DEMO-001 en `DIRECT_EXPLICIT`,
   DEMO-002 en `PROPERTY_MANAGED` : cela prouve que la plateforme n'est pas câblée sur un
   seul mécanisme de déclaration, et les deux passent par la même porte.
3. **Correction de même majeure obligatoire.** Toute CVE dont la seule issue est une
   majeure supérieure est inutilisable pour une démonstration positive.
4. **Les jars doivent être dans l'image.** Trivy scanne l'image, pas le dépôt :
   l'analyseur Java a besoin des jars. Sans cela, Trivy ne signale rien et la fermeture
   double-scanner est impossible à prouver.
5. **Compiler en Java 17.** Le vérificateur de candidats est en JDK 17, Jenkins en JDK 21 :
   `maven.compiler.release=17` est la seule valeur qui passe des deux côtés.
6. **Pas de parent Spring Boot.** Un parent introduit de la gestion de version par BOM —
   exactement la provenance que la plateforme refuse d'auto-corriger. Des dépendances
   directes et une propriété locale sont les deux seules formes pleinement supportées.
7. **Sonar et ZAP restent en détection.** Les annoncer comme remédiables serait faux ;
   ils sont documentés `DETECTION_ONLY` et présentés comme tels devant le jury.
