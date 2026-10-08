# Remise à l'état vulnérable de référence

> **APPLICATION DE DÉMONSTRATION INTENTIONNELLEMENT VULNÉRABLE — USAGE DE TEST UNIQUEMENT.**

La démonstration doit pouvoir être rejouée. Ce document dit comment revenir exactement à
l'état de départ, et surtout **ce qu'il ne faut pas faire** pour y arriver.

## Les deux repères

| Repère | Signification |
|---|---|
| `baseline-vulnerable` | étiquette du commit de référence : les deux CVE de démonstration sont présentes, le build et les 20 tests passent |
| `main` | branche par défaut ; pointe sur le commit de référence tant qu'aucune remédiation n'a été fusionnée |

Le commit exact est enregistré dans `docs/qa/…/APP_TEST_PFE_VERMEG_FINAL_REPORT.md`
(champ `BASELINE_COMMIT`) et dans la sortie de `git rev-parse baseline-vulnerable`.

## Remettre le dépôt à zéro

```bash
cd app-test-pfe-vermeg

# 1. Retirer toute fixture de contrôle encore installée
./testbed/scripts/install-control.sh --remove

# 2. Revenir au commit de référence, sans conserver de modification locale
git checkout main
git reset --hard baseline-vulnerable
git clean -fdx            # supprime target/, rapports de scanners, fichiers non suivis

# 3. Vérifier que l'état est bien le bon
mvn -B clean verify                      # attendu : BUILD SUCCESS, 20 tests
grep -n "commons-text" -A2 pom.xml       # attendu : 1.9
grep -n "commons-lang3.version" pom.xml  # attendu : 3.12.0
```

Si une remédiation a été fusionnée dans `main`, `git reset --hard baseline-vulnerable`
replace bien la branche locale sur la base, mais le dépôt distant conserve l'historique.
Pour réaligner le distant, il faut une poussée forcée — **à ne faire que délibérément** :

```bash
git push --force-with-lease origin main
```

`--force-with-lease` plutôt que `--force` : si quelqu'un d'autre a poussé entre-temps,
la commande refuse au lieu d'écraser son travail.

## Revérifier que les vulnérabilités sont bien revenues

```bash
mvn -B clean verify
docker build -t app-test-pfe-vermeg:baseline .

trivy image --timeout 45m --scanners vuln --format json \
  -o docs/demo-evidence/trivy-baseline.json app-test-pfe-vermeg:baseline

mvn -B org.owasp:dependency-check-maven:12.2.2:check -Dformat=ALL \
  -DfailBuildOnCVSS=11 -DfailOnError=false -DautoUpdate=false \
  -DdataDirectory=<cache NVD local>
cp target/dependency-check-report.json docs/demo-evidence/owasp-baseline.json

./testbed/scripts/assert-scanner-results.py \
  --trivy docs/demo-evidence/trivy-baseline.json \
  --owasp docs/demo-evidence/owasp-baseline.json \
  --expect baseline
```

La dernière commande doit répondre « Conforme ». Si elle signale un écart, c'est que la
base de vulnérabilités d'un scanner a bougé : **mettez à jour la documentation, ne
forcez pas le résultat.** Un scanner qui change d'avis est une information, pas une
panne.

## Ce qu'il ne faut PAS faire

La remise à zéro porte sur **le dépôt**, jamais sur l'état interne de la plateforme.

- **Ne modifiez aucune ligne de la base de données** de la plateforme à la main. Pas de
  `DELETE FROM manual_remediation_tasks`, pas de `UPDATE … SET status`, pas de purge de
  `remediation_target_reservations`. Ces tables portent des invariants garantis par
  PostgreSQL (notamment l'index partiel unique de Phase 4G) ; les éditer à la main crée
  exactement le genre d'incohérence que la plateforme est conçue pour rendre impossible.
- **Ne supprimez pas les constats ni les tâches** d'une démonstration précédente. Un
  nouveau build produit de nouveaux constats ; l'historique reste lisible, et c'est
  préférable à un état « propre » mais faux.
- **Ne réutilisez pas un identifiant de tentative** d'une exécution passée. Chaque
  tentative a une identité neuve, par conception.
- **Ne laissez pas une fixture de contrôle installée.** Elle ferait échouer tous les
  builds suivants, y compris pendant la soutenance.
- **Ne laissez pas la dépêche de remédiation activée** après un essai contrôlé. Remettez
  `SECURITY_REMEDIATION_DISPATCH_ENABLED=false` et
  `SECURITY_REMEDIATION_E2E_TEST_HARNESS_ENABLED=false`.

## Rejouer une démonstration proprement

1. Remettre le dépôt à la base (ci-dessus) et vérifier avec le script d'assertion.
2. Lancer un build Jenkins sur `main` : il produit un **nouveau** numéro de build et de
   **nouveaux** constats, liés au SHA de référence.
3. Les constats des démonstrations précédentes restent visibles. C'est voulu : ils
   racontent l'historique du projet.
4. Si une branche de remédiation ou une PR traîne depuis un essai précédent, fermez la PR
   et supprimez la branche **sur GitHub**, jamais en éditant la base de la plateforme.
