# `owasp.log` — volontairement exclu du dépôt

Le journal brut d'OWASP Dependency-Check produit par Jenkins **n'est pas versionné ici**,
et ce n'est pas un oubli.

## Pourquoi

La première ligne de ce journal contient la **clé API NVD en clair** :

```
+ timeout 20m mvn org.owasp:dependency-check-maven:12.2.2:update-only \
    -DdataDirectory=… -DnvdApiKey=<CLE EN CLAIR> -DnvdApiDelay=2000 …
```

Le masquage de Jenkins fonctionne bien dans la console (`-Dsonar.token=****`,
`X-API-Key: ****`), mais il ne s'applique **pas** à ce fichier : l'étape OWASP redirige
un bloc shell entier vers `$REPORT_BASE/owasp.log`, et la trace `set -x` du shell y est
écrite directement, sans passer par le filtre de masquage de Jenkins.

Ce journal est stocké dans le volume `shared_reports`, monté à la fois dans **Jenkins** et
dans **n8n**.

## Portée constatée

**59 fichiers `owasp.log`** du volume partagé contiennent la clé, pour l'ensemble des
projets (`pfe-app-test`, `devsecops-testbed`, `devsecops-platform-testbed`, …). Il s'agit
donc d'un défaut **de la plateforme**, antérieur à ce projet et indépendant de lui.

Signalé comme constat **P0** dans le rapport de qualification, avec sa correction.
Aucune valeur de clé n'est reproduite ici ni dans aucun document de ce dépôt.

## Ce qui reste disponible comme preuve

Le contenu utile du scan OWASP est intégralement dans
`dependency-check-report.json` (même répertoire), qui ne contient aucun secret —
vérifié.
