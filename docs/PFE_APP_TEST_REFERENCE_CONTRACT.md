# Contrat d'intégration de référence — audit de `pfe-app-test`

**Phase 0.** Audit complet du projet de référence avant toute création.
Objet : extraire le **contrat** d'intégration à la plateforme, pas le contenu du projet.

| Élément audité | Valeur constatée dans `pfe-app-test` |
|---|---|
| Dépôt | `https://github.com/souhaiel11/pfe-app-test.git` |
| HEAD au moment de l'audit | `c0c21af` |
| Fichiers suivis | 27 (hors `.git`, `target/`) |
| Build | Maven, `com.pfe:pfe-app-test:1.0.0` |
| Parent | `spring-boot-starter-parent:2.7.0` |
| Java déclaré | 11 |
| Jenkinsfile | 5 lignes — délègue tout à la bibliothèque partagée |

## 1. Le point essentiel : le contrat est dans la bibliothèque, pas dans le projet

Le `Jenkinsfile` de référence tient en cinq lignes :

```groovy
@Library('pfe-devsecops') _
devSecOpsPipeline {
    applicationName = 'pfe-app-test'
    zapTargetUrl    = 'http://app-test:8080'
    skipTests       = true
}
```

Toute la chaîne — checkout, build, Sonar, Docker, Trivy, OWASP, ZAP, rappel vers la
plateforme — vit dans `souhaiel11/pfe-devsecops-shared-library` (`vars/devSecOpsPipeline.groovy`,
460 lignes, plus 8 classes de support). **Un nouveau projet ne doit donc rien recopier :
il doit seulement se déclarer.** C'est la conclusion la plus importante de cet audit, et
elle rend la plupart des copier-coller inutiles et nuisibles.

Paramètres acceptés par la closure (`devSecOpsPipeline.groovy` l. 20-36) :

| Paramètre | Obligatoire | Effet |
|---|---|---|
| `applicationName` | oui en pratique | identité unique : nom d'image Docker, clé Sonar, chemin de rapports |
| `workingDirectory` | non | si l'application ne vit pas à la racine du dépôt |
| `dockerfile` | non | chemin non standard |
| `skipTests` | non, défaut `false` | déclaration **sincère**, pas une commodité |
| `zapTargetUrl` | non | défaut `http://<applicationName>:8080` |

## 2. Identité dérivée d'`applicationName` — une seule valeur, cinq usages

Constaté dans le code, pas supposé :

| Usage | Source exacte |
|---|---|
| Nom d'image Docker | `String imageName = applicationName` (l. 97) |
| Tag d'image | `String imageTag = env.BUILD_NUMBER` (l. 110) |
| Clé de projet Sonar | `sonar.project_key = ctx.applicationName` (reportToPlatform) |
| Chemin de rapports Jenkins | `/shared/reports/<applicationName>/<BUILD_NUMBER>` |
| Chemin de rapports n8n | `/home/node/.n8n-files/reports/<applicationName>/<BUILD_NUMBER>` |

**Conséquence de conception** : `applicationName` doit être unique sur toute la plateforme,
et doit être égal au nom du job Jenkins et à la clé Sonar. Une seule chaîne, partout.

## 3. Provenance — le mécanisme exact, et pourquoi il compte

La charge utile envoyée à WF1 **ne contient aucun champ `repository`**. L'association
build → projet se fait par le nom du job :

```
WF1 « Lookup Project »  →  GET /api/projects/internal/by-job/:jobName
```

(`projects.controller.ts` l. 37). Le dépôt de confiance vient donc de l'**enregistrement du
projet** (`projects.githubRepo`), jamais du build. D'où la règle d'onboarding : le
`jenkinsJobName` du projet doit être exactement le nom du job, sinon le build n'est
rattaché à rien.

Le SHA, en revanche, est transporté et durci :

- capturé par `git rev-parse HEAD` **dans** le stage Checkout, jamais via `env.GIT_COMMIT`
  (défaut QA-BUILD-135-R1 : le build #135 avait envoyé `commit="unknown"` malgré un
  checkout réussi) ;
- `payload.commitSha` n'est rempli que si la valeur vérifie `[a-fA-F0-9]{40}` ; sinon
  `commitShaDiagnostic = 'APPLICATION_CHECKOUT_SHA_UNAVAILABLE_OR_INVALID'` — jamais une
  valeur fabriquée ;
- `payload.commit` reste le SHA court (8), **informatif seulement**.

`DockerRunner` échoue **avant** de produire un artefact si le SHA complet n'est pas
disponible (`OCI_REVISION_UNAVAILABLE`), et pose les étiquettes OCI
`org.opencontainers.image.revision` / `.source` / `.created`.

## 4. Étapes du pipeline, telles qu'elles existent

| Étape | Condition d'exécution | Classification |
|---|---|---|
| Init | toujours | REUSE_PATTERN |
| Checkout | toujours, `try/catch(Throwable)` | REUSE_PATTERN |
| Build | type détecté par `ProjectDetector` | REUSE_PATTERN |
| SonarQube Analysis | `SONAR_ENABLED = true` | REUSE_PATTERN |
| Docker Build | `Dockerfile` présent | REUSE_PATTERN |
| Trivy Scan | `TRIVY_ENABLED` **et** Dockerfile présent | REUSE_PATTERN |
| OWASP Dependency Check | `OWASP_ENABLED = true` | REUSE_PATTERN |
| Kubernetes Target Check | builds de branche uniquement | REUSE_PATTERN |
| ZAP DAST Scan | branche uniquement, cible k8s joignable | ADAPT — dépend d'une cible réelle |
| reportToPlatform | **tous les chemins**, échec de checkout compris | REUSE_PATTERN |

`BuildRunner` n'implémente que **Maven** et refuse explicitement le reste plutôt que de
produire une télémétrie de succès fictive. `ProjectDetector` reconnaît
`pom.xml` / `build.gradle` / `package.json`, mais seul Maven passe le build.

## 5. Classification élément par élément

| Élément de `pfe-app-test` | Classification | Motif |
|---|---|---|
| Invocation `@Library('pfe-devsecops')` | **REUSE_PATTERN** | c'est le contrat lui-même |
| `applicationName` dans la closure | **ADAPT** | doit devenir `app-test-pfe-vermeg` |
| `zapTargetUrl` | **ADAPT** | doit viser le service du nouveau projet |
| `skipTests = true` | **DO_NOT_COPY** | l'application de démonstration doit prouver que les tests passent ; le négatif NC-1 en dépend |
| Dockerfile multi-étapes | **REUSE_PATTERN** | builder Maven + runtime JRE, non-root |
| `maven:3.8.6-openjdk-11` / `eclipse-temurin:11-jre-alpine` (tags flottants) | **ADAPT** | préférer un pinning par digest, comme le banc qualifié |
| `COPY --from=builder .../pfe-app-test-1.0.0.jar` | **ADAPT** | le nom du jar change |
| Absence de `.dockerignore` | **OBSOLETE** | un `.dockerignore` est requis pour la reproductibilité depuis un clone neuf |
| `HEALTHCHECK` sur `/api/auth/health` | **ADAPT** | route propre au domaine de référence |
| Parent Spring Boot 2.7.0 | **DO_NOT_COPY** | Jenkins compile en Java 21, le vérificateur en Java 17 : un parent Spring Boot 2.7 ajoute un couplage inutile |
| CVE de référence (Spring4Shell, logback, snakeyaml…) | **DO_NOT_COPY** | doivent être **reproduites par scan réel**, jamais héritées |
| `failBuildOnCVSS 9` (dependency-check-maven) | **DO_NOT_COPY** | la bibliothèque pilote déjà OWASP et sa politique CVSS |
| Plugin `sonar-maven-plugin` dans le pom | **DO_NOT_COPY** | `ScannerRunner` l'invoque lui-même |
| JaCoCo | **DO_NOT_COPY** | le pipeline passe `-Djacoco.skip=true` |
| `k8s/deployment.yaml` | **PROJECT_SPECIFIC** | lié à la cible ZAP de référence |
| `projectId`, SHA de base, numéros de build, PR, lignes de base de données | **DO_NOT_COPY** | identité d'exécution, jamais des constantes |
| `githubRepo = souhaiel11/pfe-app-test` | **PROJECT_SPECIFIC** | |
| Convention `name = jenkinsJobName = sonarqubeKey` | **REUSE_PATTERN** | vérifiée sur les 6 projets existants en base |

## 6. Ce que l'audit impose au nouveau projet

1. Un `Jenkinsfile` de **4 à 6 lignes**, rien de plus.
2. `applicationName = 'app-test-pfe-vermeg'`, identique au job Jenkins et à la clé Sonar.
3. `skipTests = false` — sincèrement, parce que les tests passent.
4. Un `Dockerfile` multi-étapes, non-root, reproductible depuis un clone neuf, avec
   `.dockerignore`.
5. Les jars de dépendances **présents dans l'image**, sinon l'analyseur Java de Trivy ne
   voit rien à signaler (c'est ce que fait le banc qualifié via `target/runtime/`).
6. Un endpoint de santé déterministe.
7. Aucune CVE choisie de mémoire : la sélection vient d'un scan réel.
