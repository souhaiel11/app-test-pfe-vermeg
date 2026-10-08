#!/usr/bin/env python3
"""Vérifie qu'un couple de rapports de scanners correspond à ce que la base déclare.

Le but n'est pas de « valider » les scanners, mais l'inverse : s'assurer que la
documentation du dépôt n'a pas dérivé par rapport à ce que les outils trouvent
réellement. Un écart est signalé, jamais corrigé en silence.

    ./testbed/scripts/assert-scanner-results.py \
        --trivy docs/demo-evidence/trivy-baseline.json \
        --owasp docs/demo-evidence/owasp-baseline.json \
        --expect baseline
"""
import argparse
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
BASELINE = ROOT / "testbed" / "security-baseline.json"


def trivy_java_findings(path):
    """{(package, version): {cve: fixedVersion}} pour la classe lang-pkgs."""
    data = json.loads(pathlib.Path(path).read_text(encoding="utf-8"))
    out = {}
    for result in data.get("Results") or []:
        if result.get("Class") != "lang-pkgs":
            continue
        for v in result.get("Vulnerabilities") or []:
            key = (v.get("PkgName"), v.get("InstalledVersion"))
            out.setdefault(key, {})[v.get("VulnerabilityID")] = v.get("FixedVersion")
    return out


def owasp_findings(path):
    """{jarFileName: {cve}}."""
    data = json.loads(pathlib.Path(path).read_text(encoding="utf-8"))
    out = {}
    for dep in data.get("dependencies") or []:
        cves = {v.get("name") for v in dep.get("vulnerabilities") or []}
        if cves:
            out[dep.get("fileName")] = cves
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--trivy", required=True)
    ap.add_argument("--owasp", required=True)
    ap.add_argument("--expect", choices=["baseline", "remediated"], default="baseline")
    args = ap.parse_args()

    base = json.loads(BASELINE.read_text(encoding="utf-8"))
    trivy = trivy_java_findings(args.trivy)
    owasp = owasp_findings(args.owasp)

    failures = []
    for scenario in base["scenarios"]:
        pkg, version = scenario["package"], scenario["installedVersion"]
        cve = scenario["expectedFinding"]
        want_detected = args.expect == "baseline"

        trivy_cves = trivy.get((pkg, version), {})
        trivy_has = cve in trivy_cves
        owasp_has = any(cve in cves for cves in owasp.values())

        for tool, found in (("trivy", trivy_has), ("owasp", owasp_has)):
            if found != want_detected:
                failures.append(
                    f"{scenario['id']} · {tool} : {cve} "
                    f"{'absente' if want_detected else 'encore présente'} "
                    f"(attendu : {'détectée' if want_detected else 'absente'})")

        if want_detected and trivy_has:
            fixed = trivy_cves.get(cve)
            expected = scenario["expectedCandidateVersion"]
            if fixed != expected:
                failures.append(
                    f"{scenario['id']} · trivy annonce une version corrigée "
                    f"« {fixed} », la base déclare « {expected} »")

    for line in failures:
        print("ÉCART :", line)
    if failures:
        print(f"\n{len(failures)} écart(s). La documentation et les scanners divergent.")
        return 1
    print(f"Conforme : les {len(base['scenarios'])} scénarios correspondent "
          f"aux deux scanners (attendu « {args.expect} »).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
