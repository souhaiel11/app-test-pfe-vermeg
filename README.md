# app-test-pfe-vermeg

> ## ⚠ AVERTISSEMENT — APPLICATION DE DÉMONSTRATION INTENTIONNELLEMENT VULNÉRABLE
>
> **USAGE DE TEST UNIQUEMENT.**
> **NE PAS DÉPLOYER PUBLIQUEMENT COMME SERVICE DE PRODUCTION.**
>
> Ce dépôt contient des dépendances volontairement obsolètes afin que des scanners de
> sécurité réels aient quelque chose à trouver. Il est destiné à une exécution locale,
> en CI, ou dans un environnement de test PFE contrôlé. Les données sont **synthétiques** :
> aucun identifiant, aucune donnée de production, aucun appel vers un système tiers.

**Application officielle de démonstration** du mémoire de PFE : plateforme DevSecOps de
remédiation automatique. C'est l'application utilisée devant le jury pour montrer la
chaîne complète — détection, remédiation, validation, publication.

## Ce qu'elle est, et ce qu'elle n'est pas

| | |
|---|---|
| **Est** | une cible de démonstration petite, lisible, reproductible et réellement remédiable |
| **N'est pas** | une application métier, un produit, ni une réécriture de `pfe-app-test` |

Trois projets, trois rôles, à ne pas confondre :

| Projet | Rôle |
|---|---|
| `pfe-app-test` | **implémentation de référence** — contrat d'intégration, inspecté, jamais renommé |
| `devsecops-remediation-testbed` | **banc technique interne** — qualification isolée, conservé tel quel |
| `app-test-pfe-vermeg` | **application officielle de démonstration** (ce dépôt) |

## Périmètre de sûreté

Ce dépôt ne contient ni identifiant réel, ni donnée de production, ni code destructeur.
L'application n'ouvre aucun shell, n'établit aucune persistance, n'attaque aucun service
externe, ne monte pas la socket Docker et ne requiert aucun conteneur privilégié.
Les seules méthodes HTTP acceptées sont `GET` et `HEAD`.

Le conteneur tourne en utilisateur non privilégié (`65532:65532`), en système de fichiers
en lecture seule, toutes capacités retirées, et `compose.yaml` ne publie le port que sur
la boucle locale de l'hôte.

## L'application

Serveur HTTP minimal bâti sur `com.sun.net.httpserver` (fourni par le JDK). Aucun
framework : les seules dépendances déclarées sont celles qui portent les scénarios de
remédiation, pour qu'un scan ne mélange jamais les vulnérabilités de démonstration avec
celles d'un framework.

| Route | Méthode | Réponse |
|---|---|---|
| `/health` | GET | `{"status":"UP"}` — déterministe, à l'octet |
| `/api/info` | GET | identité de l'application et avertissement |
| `/api/products` | GET | les trois produits synthétiques, valeur immobilisée totale |
| `/api/products/{id}` | GET | un produit, ou `404 not_found` |
| tout le reste | — | `404` ; méthode mutante → `405` |

## Construire et exécuter

```bash
mvn -B clean verify              # build + 18 tests
java -cp target/app-test-pfe-vermeg-1.0.0.jar:'target/runtime/*' tn.pfe.demo.App
curl -s http://127.0.0.1:8080/health     # {"status":"UP"}

docker build -t app-test-pfe-vermeg:local .
docker compose up -d && curl -s http://127.0.0.1:18080/health
```

Prérequis : JDK 17 ou 21, Maven 3.6.3 ou plus. Le build est reproductible
(`project.build.outputTimestamp`), et les images de base du `Dockerfile` sont épinglées
par digest — jamais par tag flottant.

## Intégration à la plateforme

Le `Jenkinsfile` tient en quatre lignes utiles : tout le pipeline vit dans la
bibliothèque partagée `pfe-devsecops`. Un projet ne recopie rien, il se déclare.

```groovy
@Library('pfe-devsecops') _
devSecOpsPipeline {
    applicationName = 'app-test-pfe-vermeg'
    skipTests       = false
    zapTargetUrl    = 'http://app-test-pfe-vermeg:8080'
}
```

Une seule chaîne d'identité, partout : nom du projet = nom du job Jenkins = clé Sonar =
dépôt d'image Docker = `app-test-pfe-vermeg`. Le rattachement build → projet se fait par
`GET /api/projects/internal/by-job/<jobName>` : si le nom du job diverge, le build n'est
rattaché à rien.

## Documentation

| Document | Contenu |
|---|---|
| `docs/PFE_APP_TEST_REFERENCE_CONTRACT.md` | audit du projet de référence, élément par élément |
| `docs/DEMO_REMEDIATION_CAPABILITY_MATRIX.md` | ce que la plateforme sait réellement remédier |
| `testbed/SCENARIO_MATRIX.md` | les scénarios positifs et les contrôles négatifs |
| `docs/JURY_DEMO_RUNBOOK.md` | déroulé de la démonstration, étape par étape |
| `docs/FINAL_DEMO_MATRIX.md` | capacité → preuve → écran → action attendue |
| `RESET_TO_VULNERABLE_BASELINE.md` | remise à l'état vulnérable de référence |
| `docs/demo-evidence/` | preuves de scan réelles, pour servir de repli en soutenance |
