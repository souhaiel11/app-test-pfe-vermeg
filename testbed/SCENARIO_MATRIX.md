# Matrice des scénarios — app-test-pfe-vermeg

> **APPLICATION DE DÉMONSTRATION INTENTIONNELLEMENT VULNÉRABLE — USAGE DE TEST UNIQUEMENT.**

Toutes les valeurs ci-dessous viennent de **scans réels**, jamais d'une sélection de
mémoire. Les rapports bruts sont dans `docs/demo-evidence/`. Le script
`testbed/scripts/assert-scanner-results.py` revérifie cette matrice contre les rapports
et signale tout écart.

Outils : Trivy 0.69.3 (base du 2026-10-07) · OWASP Dependency-Check 12.2.2 (cache NVD
local, `autoUpdate=false`).

## Deux positifs, deux provenances — et pourquoi

La plateforme n'auto-corrige que deux formes de déclaration Maven :
`DIRECT_EXPLICIT` et `PROPERTY_MANAGED` (voir
`docs/DEMO_REMEDIATION_CAPABILITY_MATRIX.md`). Les deux scénarios positifs en couvrent
une chacune. Ce n'est pas un doublon : c'est ce qui prouve que la plateforme n'est pas
câblée sur un seul mécanisme.

---

## DEMO-001 — dépendance directe explicite *(scénario principal du jury)*

| | |
|---|---|
| **ID** | DEMO-001 |
| **PURPOSE** | Démontrer la chaîne complète : détection → tâche → candidat → build → tests → double rescan → fermeture prouvée |
| **INITIAL_PROBLEM** | `org.apache.commons:commons-text` **1.9** porte **CVE-2022-42889** (« Text4Shell », CVSS 9.8, CRITICAL) |
| **DETECTED_BY** | Trivy **et** OWASP Dependency-Check |
| **TARGET_FILE** | `pom.xml` (racine) |
| **PROVENANCE** | `DIRECT_EXPLICIT` — version écrite dans l'élément `<dependency>` |
| **EXPECTED_REMEDIATION** | Montée à la plus basse version corrigée de **même majeure** |
| **EXPECTED_CHANGE** | `<version>1.9</version>` → `<version>1.10.0</version>` |
| **EXPECTED_BUILD** | PASS |
| **EXPECTED_TESTS** | PASS — 20 tests |
| **EXPECTED_TRIVY** | CVE-2022-42889 **absente** (vérifié : 0 vulnérabilité `lang-pkgs`) |
| **EXPECTED_OWASP** | CVE-2022-42889 **absente** (vérifié : aucune CVE) |
| **EXPECTED_PLATFORM_FINAL_STATE** | candidat validé, fermeture de sécurité prouvée ; publication Git seulement si l'écrivain est explicitement activé |
| **DEMO_VALUE** | **Très élevée.** Une CVE critique et connue, une seule par paquet, une correction sans changement de comportement — l'histoire se raconte en une minute |

**Qualification indépendante déjà effectuée** (`docs/demo-evidence/`) :
build PASS · 20 tests PASS · Trivy `lang-pkgs` = 0 · OWASP = aucune CVE.

**Note de sûreté** : l'application n'emploie **pas** l'interpolateur vulnérable
(`StringSubstitutor.createInterpolator()`). La dépendance est présente et réellement
utilisée — `StringEscapeUtils.escapeJson` est sur le chemin de chaque réponse — mais le
chemin exploitable ne l'est pas. Un scanner détecte une **dépendance**, pas une
exploitation ; c'est exactement la distinction à expliquer au jury.

---

## DEMO-002 — dépendance gérée par propriété *(cible indépendante)*

| | |
|---|---|
| **ID** | DEMO-002 |
| **PURPOSE** | Prouver que la plateforme n'est pas câblée autour de DEMO-001, et qu'elle sait corriger une **propriété** et non une version en dur |
| **INITIAL_PROBLEM** | `org.apache.commons:commons-lang3` **3.12.0** porte **CVE-2025-48924** (CVSS 5.3, MEDIUM) |
| **DETECTED_BY** | Trivy **et** OWASP Dependency-Check |
| **TARGET_FILE** | `pom.xml` (racine) — **le même fichier que DEMO-001** |
| **PROVENANCE** | `PROPERTY_MANAGED` — pilotée par `${commons-lang3.version}` |
| **EXPECTED_REMEDIATION** | Montée de la **propriété**, pas de l'élément `<dependency>` |
| **EXPECTED_CHANGE** | `<commons-lang3.version>3.12.0</…>` → `<commons-lang3.version>3.18.0</…>` |
| **EXPECTED_BUILD** | PASS |
| **EXPECTED_TESTS** | PASS — 20 tests |
| **EXPECTED_TRIVY** | CVE-2025-48924 **absente** |
| **EXPECTED_OWASP** | CVE-2025-48924 **absente** |
| **EXPECTED_PLATFORM_FINAL_STATE** | candidat validé, fermeture prouvée |
| **DEMO_VALUE** | Élevée. Deuxième cible, autre mécanisme de déclaration, **même fichier** — ce qui en fait aussi le support du contrôle de concurrence NC-3 |

---

## DEMO-003 — défaut de code source *(DETECTION_ONLY)*

| | |
|---|---|
| **ID** | DEMO-003 |
| **STATUT** | **DETECTION_ONLY** — assumé, documenté, non présenté comme une correction |
| **DETECTED_BY** | SonarQube |
| **TARGET_FILE** | sources Java |
| **EXPECTED_REMEDIATION** | aucune par la chaîne WF6 ; le chemin incident/WF2 existe mais n'est pas qualifié comme positif automatique |
| **EXPECTED_PLATFORM_FINAL_STATE** | constat visible dans l'écran Sonar ; aucune fermeture annoncée |
| **DEMO_VALUE** | Moyenne — utile pour montrer qu'une plateforme honnête distingue *détecter* de *corriger* |

Rien n'a été ajouté au code pour fabriquer un défaut Sonar. L'analyse réelle rapporte ce
qu'elle rapporte ; la démonstration le montre sans le mettre en scène.

---

## DEMO-004 — DAST / ZAP *(DETECTION_ONLY)*

| | |
|---|---|
| **ID** | DEMO-004 |
| **STATUT** | **DETECTION_ONLY** |
| **DETECTED_BY** | OWASP ZAP, exécuté par le pipeline sur les builds de branche |
| **PRÉREQUIS** | une cible Kubernetes joignable dans l'espace de noms `pfe-devsecops` |
| **EXPECTED_REMEDIATION** | aucune stratégie automatique indépendante de la source |
| **EXPECTED_PLATFORM_FINAL_STATE** | exécution DAST réelle, ou constat sincère de cible absente — **jamais un résultat fabriqué** |
| **DEMO_VALUE** | Faible à moyenne. L'application répond `X-Content-Type-Options: nosniff` et n'accepte que GET/HEAD : la surface est volontairement minuscule |

Si aucune cible n'est déployée, la bibliothèque partagée signale `zap_k8s_unreachable`.
C'est un diagnostic, pas un scan réussi — et la plateforme l'affiche comme tel.

---

## DEMO-005 — paquet du système d'exploitation *(DETECTION_ONLY, constaté)*

| | |
|---|---|
| **ID** | DEMO-005 |
| **STATUT** | **DETECTION_ONLY** |
| **INITIAL_PROBLEM** | `zlib` **1.3.2-r0** dans la couche Alpine 3.24.2 porte **CVE-2026-85091** (corrigée en `1.3.2-r1`) |
| **DETECTED_BY** | Trivy (`os-pkgs`) |
| **EXPECTED_REMEDIATION** | aucune — WF6 n'a pas de stratégie de version d'image |
| **DEMO_VALUE** | Élevée comme **contre-exemple** : Trivy trouve une vraie vulnérabilité que la plateforme ne prétend pas corriger. C'est la meilleure illustration de la frontière de capacité |

Ce constat n'a pas été provoqué : il vient de l'image de base épinglée. Il est conservé
tel quel plutôt que masqué.

---

## NC-1 — le candidat casse un test fonctionnel

| | |
|---|---|
| **ID** | NC-1 |
| **PURPOSE** | Prouver qu'un build vert ne suffit pas à valider un candidat |
| **INITIAL_PROBLEM** | aucun — c'est une fixture de qualification, pas une vulnérabilité |
| **MÉCANISME** | `testbed/controls/LegacyClientContractTest.java.txt` modélise un consommateur en aval dont le contrat impose une version de dépendance exacte. Il lit la version **réellement chargée** dans le manifeste du jar, pas celle écrite dans le pom |
| **MISE EN PLACE** | `./testbed/scripts/install-control.sh nc1` puis `--remove` |
| **EXPECTED_BUILD** | PASS — la compilation réussit |
| **EXPECTED_TESTS** | **FAIL** — le contrat de version est rompu dès que le candidat monte la dépendance |
| **EXPECTED_TRIVY / OWASP** | non atteints : la validation s'arrête avant |
| **EXPECTED_PLATFORM_FINAL_STATE** | candidat **REJETÉ** · pas de `VERIFIED` · **aucune écriture Git** |
| **DEMO_VALUE** | Très élevée. C'est le scénario qui prouve que la plateforme protège la fonctionnalité, pas seulement la sécurité |

Le fichier porte l'extension `.txt` pour ne jamais entrer dans la suite normale. Un
contrôle laissé en place polluerait tous les builds suivants — le script le rappelle.

---

## NC-2 — la vulnérabilité subsiste

| | |
|---|---|
| **ID** | NC-2 |
| **PURPOSE** | Prouver que **BUILD PASS n'est pas SECURITY PASS** |
| **MÉCANISME** | Candidat qui monte `commons-lang3` de `3.12.0` à **`3.14.0`** — une version plus récente, **toujours vulnérable** à CVE-2025-48924 (corrigée seulement en 3.18.0) |
| **EXPECTED_BUILD** | PASS — *vérifié* |
| **EXPECTED_TESTS** | PASS — *vérifié, 20 tests* |
| **EXPECTED_OWASP** | CVE-2025-48924 **encore signalée** — *vérifié* (`docs/demo-evidence/owasp-nc2-still-vulnerable.json`) |
| **EXPECTED_TRIVY** | cible encore présente |
| **EXPECTED_PLATFORM_FINAL_STATE** | candidat **REJETÉ** par la fermeture de sécurité · pas de `VERIFIED` · **aucune écriture Git** |
| **DEMO_VALUE** | Très élevée. Un candidat plausible, un build vert, des tests verts — et un refus quand même |

C'est le contrôle le plus instructif : il montre que la plateforme vérifie l'**absence de
la CVE visée**, pas simplement qu'« une mise à jour a eu lieu ».

---

## NC-3 — écrivain unique (Phase 4G)

| | |
|---|---|
| **ID** | NC-3 |
| **PURPOSE** | Prouver qu'une seule remédiation peut détenir un fichier cible à la fois |
| **MÉCANISME** | DEMO-001 et DEMO-002 visent **le même `pom.xml`**. Leur `targetKey` est donc identique, et l'index partiel unique `remediation_target_one_active` n'autorise qu'une seule réservation `ACTIVE` par `(projectId, targetKey)` |
| **EXPECTED_PLATFORM_FINAL_STATE** | une tâche obtient la réservation · l'autre reçoit **409 `REMEDIATION_TARGET_ALREADY_IN_PROGRESS`** |
| **DEMO_VALUE** | Élevée — mais à montrer par capture d'écran plutôt qu'en direct si le temps manque |

Aucun module enfant n'existe dans ce projet : l'adaptateur de cible de production n'ancre
que le descripteur racine, et prétendre démontrer une concurrence inter-fichiers serait
faux.

---

## Récapitulatif

| ID | Type | Détecté par | Remédiable | Prouvé par scan réel |
|---|---|---|---|---|
| DEMO-001 | dépendance directe | Trivy + OWASP | **oui** | oui — base et candidat |
| DEMO-002 | dépendance par propriété | Trivy + OWASP | **oui** | oui — base et candidat |
| DEMO-003 | code source | SonarQube | non (détection) | analyse réelle du pipeline |
| DEMO-004 | DAST | ZAP | non (détection) | exécution réelle, ou absence de cible signalée |
| DEMO-005 | paquet OS | Trivy | non (détection) | oui — constaté à la base |
| NC-1 | contrôle négatif | — | rejet attendu | mécanisme vérifié |
| NC-2 | contrôle négatif | OWASP + Trivy | rejet attendu | oui — build et tests verts, CVE présente |
| NC-3 | contrôle de concurrence | — | un seul écrivain | contrainte de base de données |

**2 scénarios positifs entièrement remédiables · 2 contrôles négatifs · 1 contrôle de
concurrence · 3 constats en détection seule.**
