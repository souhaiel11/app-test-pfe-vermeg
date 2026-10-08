#!/usr/bin/env sh
# Installe ou retire une fixture de contrôle négatif.
#
# Les contrôles vivent sous testbed/controls/ avec l'extension .txt pour ne jamais
# entrer dans la suite de tests normale. Ce script les met en place le temps d'une
# qualification isolée, puis les retire.
#
#   ./testbed/scripts/install-control.sh nc1      # installe le contrôle NC-1
#   ./testbed/scripts/install-control.sh --remove  # retire tout contrôle installé
set -eu

ROOT=$(cd "$(dirname "$0")/../.." && pwd)
TARGET="$ROOT/src/test/java/tn/pfe/demo/LegacyClientContractTest.java"

case "${1:-}" in
  nc1)
    cp "$ROOT/testbed/controls/LegacyClientContractTest.java.txt" "$TARGET"
    echo "Contrôle NC-1 installé : $TARGET"
    echo "Attendu : PASSE sur la base vulnérable, ÉCHOUE sur tout candidat."
    ;;
  --remove)
    rm -f "$TARGET"
    echo "Contrôle retiré. La suite de tests est revenue à son état normal."
    ;;
  *)
    echo "usage: $0 {nc1|--remove}" >&2
    exit 2
    ;;
esac

# Un contrôle laissé en place polluerait tous les builds suivants : on le rappelle.
echo "N'oubliez pas : « $0 --remove » avant de committer quoi que ce soit."
