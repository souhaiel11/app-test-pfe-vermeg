# Runbook de démonstration — soutenance

> **APPLICATION DE DÉMONSTRATION INTENTIONNELLEMENT VULNÉRABLE — USAGE DE TEST UNIQUEMENT.**

Pas de « mode démonstration » : rien n'est simulé, rien n'est pré-enregistré. Ce
document est un déroulé, pas un décor. Chaque étape indique **quoi cliquer**, **quoi
dire**, **ce qui doit s'afficher**, et **quoi faire si un outil externe traîne**.

Durée visée : **12 à 15 minutes** pour le parcours complet. Les étapes marquées
*(optionnel)* se sautent sans casser le récit.

---

## A. Vérifications avant d'entrer dans la salle

À faire **une heure avant**, pas devant le jury.

| # | Vérification | Commande ou écran | Attendu |
|---|---|---|---|
| 1 | Santé de la plateforme | `curl -s localhost:3001/healthz` | réponse avec la base joignable (vrai `SELECT 1`) |
| 2 | Santé de Jenkins | `http://localhost:8082` | tableau de bord, job `app-test-pfe-vermeg` visible |
| 3 | Santé de n8n | `curl -s localhost:5678/healthz` | `{"status":"ok"}` |
| 4 | Scanners | `docker ps` | `sonarqube`, `pfe-builder-scanner`, `pfe-candidate-verifier` démarrés |
| 5 | Accès GitHub | `git ls-remote origin` | les références répondent |
| 6 | Projet visible | écran **Projets** de l'interface | `app-test-pfe-vermeg` présent |
| 7 | État du dépôt connu | `git describe --tags` | `baseline-vulnerable` (ou l'écart assumé) |
| 8 | Cible DAST vivante | `kubectl -n pfe-devsecops get pod -l app=app-test-pfe-vermeg` | pod `Running`, sinon l'étape ZAP signalera honnêtement l'absence de cible |
| 9 | État de la dépêche connu | variables du backend | `SECURITY_REMEDIATION_DISPATCH_ENABLED` : savoir si c'est `true` ou `false` **avant** de promettre une remédiation en direct |

**Le point 8 est le plus important.** Si la dépêche est désactivée, ne promettez pas une
remédiation en direct : montrez la chaîne jusqu'à la tâche, puis les preuves
pré-enregistrées de `docs/demo-evidence/`. Annoncer une capacité qu'on n'a pas activée
est la seule faute impossible à rattraper devant un jury.

---

## B. Le déroulé

### 1. Ouvrir l'écran Projets

**Cliquer** : onglet *Projets*.
**Dire** : « La plateforme ne connaît pas d'application en particulier. Tout ce que vous
allez voir arrive par le même chemin d'intégration que n'importe quel projet. »
**Attendu** : la liste des projets, dont `app-test-pfe-vermeg`.
**Si lent** : la liste vient d'un simple `GET /api/projects` ; un rechargement suffit.

### 2. Sélectionner `app-test-pfe-vermeg`

**Cliquer** : la ligne du projet.
**Dire** : « Aucune ligne de code de la plateforme ne mentionne ce projet. Les onglets
que vous voyez sont ceux de tout projet. »
**Attendu** : la vue projet avec ses onglets — Rapport, Jenkins/CI-CD, SonarQube, Docker,
Sécurité, Remédiation, Incidents, Configuration, Déploiement.
**Point de force** : c'est ici qu'on prouve la généricité. Si le jury doute, montrer
`docs/qa/…/APP_TEST_PFE_VERMEG_FINAL_REPORT.md` : `INVALID_RUNTIME_HARDCODING = 0`.

### 3. Vue d'ensemble du projet

**Dire** : « L'application est volontairement petite : un serveur HTTP de 130 lignes,
quatre routes, deux dépendances. Petite parce que la démonstration porte sur la
plateforme, pas sur l'application. »
**Attendu** : dépôt, branche, dernier build, score de sécurité.
**À souligner** : le dépôt affiché vient de l'enregistrement du projet — c'est lui qui
sert de dépôt de confiance, jamais une valeur envoyée par le build.

### 4. Onglet Jenkins / CI-CD

**Cliquer** : *Jenkins / CI-CD*.
**Dire** : « Le `Jenkinsfile` du projet tient en quatre lignes. Tout le pipeline vit dans
une bibliothèque partagée : checkout, build, Sonar, Docker, Trivy, OWASP, ZAP, puis le
rappel vers la plateforme. »
**Attendu** : le dernier build, son statut, ses étapes.
**Si lent** : Jenkins peut mettre quelques secondes ; le lien *consoleText* est un repli
lisible.
**Phrase utile** : « Jenkins produit des faits. Il ne décide d'aucune remédiation. »

### 5. Onglet SonarQube

**Cliquer** : *SonarQube*.
**Dire** : « Sonar analyse le code source. Sur ce projet, la plateforme le **détecte**
mais ne prétend pas le corriger automatiquement — et c'est écrit noir sur blanc dans la
matrice de capacités. »
**Attendu** : les six cartes renseignées — contrôle qualité **OK**, 0 bug,
0 vulnérabilité, **0** problème de maintenabilité, couverture **88,8 %**,
statut d'analyse terminé.
**Ce qu'il faut dire** : « Le code source est propre. Les vulnérabilités de ce
projet sont dans ses **dépendances**, délibérément périmées — c'est là que la
plateforme sait corriger automatiquement. »
**Si le jury demande « et s'il y avait des défauts ? »** : il y en avait sept,
relevés par cette même analyse. Ils ont été corrigés et **Sonar les a fermés
lui-même** en `resolution=FIXED`. Montrer `PROBLEMES_ET_CORRECTIONS.md` § 5 :
chaque règle, la correction, et pourquoi les laisser en place « pour avoir
quelque chose à montrer » aurait été une mise en scène.
**Anecdote utile** : à un build précédent le contrôle qualité valait **ERROR**,
parce que la couverture du code nouveau était nulle. Il est passé à **OK** quand
la couverture est arrivée. C'est un signal vivant, pas un décor.

### 6. Onglet Sécurité

**Cliquer** : *Sécurité*.
**Dire** : « Deux scanners indépendants : Trivy sur l'image construite, OWASP
Dependency-Check sur l'arbre de dépendances. Ils ne regardent pas le même objet, et c'est
exactement pour cela qu'on les garde tous les deux. »
**Attendu** : **3 CVE Java** (une CRITICAL, une HIGH, une MEDIUM) confirmées par
les deux scanners, **1 CVE de paquet OS**, et **1 alerte ZAP**.
**À montrer absolument** : les deux constats que la plateforme **ne prétend pas**
corriger — la CVE `zlib` de la couche Alpine, et l'alerte ZAP. Ce sont de
vraies trouvailles laissées manuelles, parce que la stratégie de remédiation ne
couvre ni les paquets OS ni les constats DAST. C'est la meilleure démonstration
d'honnêteté de toute la soutenance.

### 7. Le constat DEMO-001

**Cliquer** : le constat `CVE-2022-42889` sur `org.apache.commons:commons-text`.
**Dire** : « Text4Shell, CVSS 9.8. Détectée par Trivy **et** par OWASP. Version installée
1.9, version corrigée 1.10.0 — même version majeure, donc dans l'enveloppe que la
plateforme accepte de corriger seule. Elle ne franchit jamais une majeure sans un humain. »
**Attendu** : paquet, version installée, version corrigée, sévérité, scanner d'origine.
**Repli** : `docs/demo-evidence/trivy-baseline.json` et `owasp-baseline.json` portent la
même information, hors ligne.

### 8. La tâche de remédiation

**Cliquer** : *Remédiation*.
**Dire** : « Un constat n'est pas une tâche. Un constat dit ce qui est vu ; une tâche dit
ce qu'on a décidé d'en faire. »
**Attendu** : la tâche correspondant à DEMO-001, son état, sa cible.

### 9. Lancer la remédiation contrôlée

**Cliquer** : l'action de remédiation sur la tâche DEMO-001.
**Dire** : « Une seule remédiation, sur une seule cible. La réservation garantit qu'aucune
autre tâche ne peut toucher ce `pom.xml` en même temps. »
**Attendu** : la tâche passe en dépêche, un `attemptId` et un `batchId` neufs apparaissent.
**Si la dépêche est désactivée** : le dire franchement — « la dépêche est volontairement
coupée hors essai contrôlé » — et enchaîner sur les preuves de `docs/demo-evidence/`.
**Ne jamais** activer la dépêche en direct devant le jury pour sauver la démonstration.

### 10. WF6 en cours de traitement

**Cliquer** : n8n, exécution de WF6 *(optionnel)*.
**Dire** : « WF6 orchestre, mais ne juge pas. Il délègue l'évaluation à un vérificateur
de candidats qui n'a **aucune capacité** et **aucun secret** — c'est lui qui exécute le
code proposé. »
**Attendu** : l'exécution progresse à travers ses zones.
**Si lent** : WF6 est long par nature (build + deux scans). Expliquer la chaîne à l'écran
plutôt que d'attendre en silence.

### 11. Validation du candidat

**Attendu** : le candidat modifie `pom.xml` — `<version>1.9</version>` → `1.10.0`.
**Dire** : « Un seul fichier, une seule ligne. La plateforme n'improvise pas de
refactorisation : elle monte une version, et c'est tout. »

### 12. Build et tests du candidat

**Attendu** : build PASS, **24 tests PASS**.
**Dire** : « Si un seul test échouait, le candidat serait rejeté — même avec la
vulnérabilité corrigée. La sécurité ne justifie pas de casser l'application. »
**À garder sous la main** : le contrôle NC-1 prouve ce rejet. Si le jury demande,
montrer `testbed/SCENARIO_MATRIX.md` § NC-1, vérifié : 2 échecs, BUILD FAILURE.

### 13. Fermeture de sécurité — les deux rescans

**Attendu** : Trivy et OWASP relancés sur le candidat ; CVE-2022-42889 absente des deux.
**Dire** : « La fermeture exige deux choses **ensemble** : la CVE a disparu, **et** le
paquet est bien présent à la version cible. Un scan qui n'a pas abouti ne prouve rien —
le code le traduit par un refus, jamais par un succès. »
**Le contre-exemple qui marque** : NC-2. Un candidat qui monte `commons-lang3` de 3.12.0 à
3.14.0 compile, passe les 20 tests… et reste vulnérable. Il est rejeté. **Un build vert
n'est pas une fermeture de sécurité.**

### 14. Git et pull request *(si l'écrivain est activé)*

**Attendu** : une branche neuve, un commit de remédiation, une PR.
**Dire** : « La capacité d'écrire sur GitHub vit dans n8n, pas dans le backend. Le backend
décide ; n8n écrit. Aucun composant ne réunit la décision et l'écriture. »
**À dire absolument** : « Une PR créée n'est pas un projet corrigé. Candidat validé, PR
créée, PR fusionnée, déployé : ce sont **quatre états différents**. »
**Si l'écrivain est désactivé** : le documenter à l'écran plutôt que de le contourner.

### 15. Gouvernance de déploiement

**Cliquer** : *Déploiement*.
**Dire** : « La plateforme sépare deux questions qu'on confond d'habitude : *est-ce
techniquement possible* et *est-ce raisonnable*. La première est une capacité, la seconde
une décision de gouvernance. Seule la seconde peut être contournée — explicitement, en
recopiant une phrase imposée. »
**Attendu** : le verdict de gouvernance pour ce projet, avec ses contrôles.
**Important** : la base vulnérable **n'est pas déployée**. La configuration Azure est
préparée et validée ; le déploiement réel attend un candidat corrigé.

---

## C. Replis, par ordre de préférence

1. **Rafraîchir l'écran.** La plupart des lenteurs sont un appel en vol.
2. **Les preuves hors ligne.** `docs/demo-evidence/` contient les rapports réels de Trivy
   et d'OWASP, à la base et sur le candidat qualifié. Ouvrables sans réseau.
3. **La matrice de scénarios.** `testbed/SCENARIO_MATRIX.md` donne attendu et vérifié
   pour chaque cas, y compris les contrôles négatifs.
4. **Expliquer au tableau.** Si un outil externe est vraiment bloqué, la chaîne se raconte
   sans écran — et le jury retient le raisonnement, pas l'interface.

## D. Les trois phrases à ne pas rater

1. « Le LLM propose, il ne décide jamais. Le backend décide, il n'écrit jamais sur GitHub.
   n8n écrit, il ne juge jamais. »
2. « Un build vert n'est pas une fermeture de sécurité — et un scan silencieux n'est jamais
   une bonne nouvelle. »
3. « Une pull request créée n'est pas un projet corrigé. »
