# Matrice finale de démonstration — app-test-pfe-vermeg

> **APPLICATION DE DÉMONSTRATION INTENTIONNELLEMENT VULNÉRABLE — USAGE DE TEST UNIQUEMENT.**

Une ligne par capacité de la plateforme : la preuve qu'elle existe **dans ce projet**,
l'outil qui la produit, l'écran où la montrer, le geste, et ce qui doit s'afficher.
Une capacité sans preuve dans ce projet est marquée comme telle, pas gonflée.

| Capacité | Preuve dans `app-test-pfe-vermeg` | Outil | Écran plateforme | Geste de démonstration | Résultat attendu |
|---|---|---|---|---|---|
| **Git / SCM** | dépôt propre, historique linéaire, étiquette `baseline-vulnerable` | Git / GitHub | Projet → vue d'ensemble | montrer dépôt et branche par défaut | dépôt `souhaiel11/app-test-pfe-vermeg`, branche `main`, SHA de référence |
| **Intégration Jenkins** | `Jenkinsfile` de 4 lignes utiles, délégué à la bibliothèque partagée | Jenkins + `pfe-devsecops` | Projet → Jenkins / CI-CD | ouvrir le dernier build | étapes Init → Checkout → Build → Sonar → Docker → Trivy → OWASP → rappel |
| **Build** | `mvn clean verify` reproductible, plugins épinglés, `outputTimestamp` fixé | Maven 3.6.3 / 3.8.7 / 3.9.9 | Projet → Jenkins / CI-CD | montrer l'étape Build | `BUILD SUCCESS` |
| **Tests** | 20 tests sur 3 classes : santé, API, contrats de dépendances | JUnit 5 / Surefire | Projet → Jenkins / CI-CD | montrer l'agrégat de tests | `tests=20 failures=0 errors=0` |
| **Contrat de santé** | `GET /health` → `{"status":"UP"}`, déterministe à l'octet | JDK HttpServer | Projet → Déploiement | montrer le contrôle de santé configuré | corps attendu `UP`, chemin `/health`, port 8080 |
| **SonarQube** | clé de projet dédiée `app-test-pfe-vermeg`, analyse réelle | SonarQube | Projet → SonarQube | ouvrir les métriques | métriques réelles ; **détection seule**, aucune correction annoncée |
| **Docker** | Dockerfile multi-étapes, images de base **épinglées par digest**, non-root `65532`, `.dockerignore` | Docker / BuildKit | Projet → Docker | montrer l'image et son tag | `app-test-pfe-vermeg:<numéro de build>`, étiquettes OCI `revision` / `source` / `created` |
| **Trivy** | 1 CVE Java **critique** + 1 CVE Java moyenne + 1 CVE de paquet OS, versions corrigées fournies | Trivy 0.69.3 | Projet → Sécurité | ouvrir le rapport Trivy | CVE-2022-42889 → 1.10.0 · CVE-2025-48924 → 3.18.0 · CVE-2026-85091 (`zlib`) |
| **OWASP Dependency-Check** | exactement 2 CVE, une par dépendance, aucune autre | Dependency-Check 12.2.2 | Projet → Sécurité | ouvrir le rapport OWASP | les deux mêmes CVE, confirmées indépendamment de Trivy |
| **ZAP (DAST)** | exécuté par le pipeline sur les builds de branche ; surface minuscule (GET/HEAD seuls, `nosniff`) | OWASP ZAP | Projet → Sécurité | montrer l'état d'exécution ZAP | exécution réelle, **ou** `zap_k8s_unreachable` affiché honnêtement si aucune cible |
| **Constat** | constats réels issus des deux scanners, corrélés au bon paquet et à la bonne version | plateforme | Projet → Sécurité | ouvrir le constat DEMO-001 | paquet, version installée, version corrigée, sévérité, scanner d'origine |
| **Remédiation** | DEMO-001 (directe) et DEMO-002 (par propriété), toutes deux dans l'enveloppe auto-corrigeable | WF6 + vérificateur | Projet → Remédiation | lancer la remédiation contrôlée DEMO-001 | `pom.xml` : `1.9` → `1.10.0`, build PASS, 20 tests PASS, 2 rescans propres |
| **Validation du candidat** | contrôle NC-1 vérifié : 22 tests, 2 échecs, `BUILD FAILURE` | vérificateur de candidats | Projet → Remédiation | montrer NC-1 dans la matrice de scénarios | candidat **rejeté**, pas de `VERIFIED`, **aucune écriture Git** |
| **Fermeture de sécurité** | NC-2 vérifié : `3.12.0 → 3.14.0` compile, 20 tests passent, CVE **toujours présente** | OWASP + Trivy | Projet → Remédiation | montrer NC-2 | candidat **rejeté** — *BUILD PASS n'est pas SECURITY PASS* |
| **Reprise (retry)** | identité neuve par tentative : `attemptId` et `batchId` régénérés, `attemptNumber` incrémenté une fois, borne à 3 | backend | Projet → Remédiation | montrer les tentatives d'une tâche | réservation **retenue** entre deux tentatives, jamais relâchée |
| **Phase 4G — écrivain unique** | DEMO-001 et DEMO-002 visent le **même `pom.xml`**, donc le même `targetKey` | PostgreSQL (index partiel unique) | Projet → Remédiation | lancer les deux tâches | une réservation obtenue, l'autre **409 `REMEDIATION_TARGET_ALREADY_IN_PROGRESS`** |
| **Git / PR** | écriture confinée à n8n ; le backend décide, n8n écrit | n8n (nœud `github`) | Projet → Remédiation / GitHub | montrer branche, commit, PR | branche dédiée, un commit de remédiation, PR ouverte — **jamais de poussée directe sur `main`** |
| **Gouvernance de déploiement** | verdict de gouvernance calculé pour ce projet, contrôles détaillés | backend | Projet → Déploiement | ouvrir l'onglet Déploiement | verdict `READY` / `CONDITIONAL` / `CRITICAL_RISK` / `TECHNICALLY_IMPOSSIBLE` avec ses motifs |
| **Préparation Azure** | configuration dédiée : `aci-app-test-pfe-vermeg`, dépôt d'image propre, contrôle de santé | Azure Container Instances | Projet → Configuration / Déploiement | montrer la configuration Azure | cible, groupe de ressources, registre, santé — **aucun déploiement de la base vulnérable** |
| **Généricité de la plateforme** | aucune mention du projet dans le code d'exécution | audit statique | — | montrer le rapport de qualification | `SPECIAL_CASE_ADDED_TO_PLATFORM_RUNTIME = NO`, `INVALID_RUNTIME_HARDCODING = 0` |

## Capacités délibérément **non** revendiquées

Les lister est aussi important que lister les autres : une matrice qui ne dit que ce qui
marche n'est pas une matrice, c'est une plaquette.

| Capacité | Statut dans ce projet | Pourquoi |
|---|---|---|
| Correction automatique d'un défaut Sonar | **détection seule** | aucune stratégie de remédiation de code source qualifiée dans la chaîne WF6 |
| Correction automatique d'un constat ZAP | **détection seule** | aucune stratégie automatique indépendante de la source |
| Correction automatique d'un paquet OS / image de base | **détection seule** | WF6 n'a pas de stratégie de version d'image ; la CVE `zlib` est affichée, pas corrigée |
| Montée de version **majeure** | **non supporté** | la politique renvoie `CROSS_MAJOR_ONLY` : une majeure exige un humain |
| Dépendance **transitive** | **non supporté** | `TRANSITIVE_NOT_AUTOFIXABLE_V1` |
| Dépendance gérée par **BOM** / parent | **non supporté / partiel** | `ADMIN_ACTION_REQUIRED` ; aucun parent dans ce projet, précisément pour éviter ce cas |
| Édition dans un **module enfant** | **hors périmètre** | l'adaptateur de cible n'ancre que le descripteur racine ; ce projet n'a qu'un seul `pom.xml` |
| Déploiement Azure de la base vulnérable | **non exécuté** | une application volontairement vulnérable ne doit pas être exposée |
