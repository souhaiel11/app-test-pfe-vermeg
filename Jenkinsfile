// APPLICATION DE DÉMONSTRATION INTENTIONNELLEMENT VULNÉRABLE — USAGE DE TEST UNIQUEMENT.
//
// Tout le pipeline (checkout, build, Sonar, Docker, Trivy, OWASP, ZAP, rappel vers la
// plateforme) vit dans la bibliothèque partagée. Un projet ne recopie rien : il se
// déclare. Voir docs/PFE_APP_TEST_REFERENCE_CONTRACT.md.
@Library('pfe-devsecops') _

devSecOpsPipeline {
    applicationName = 'app-test-pfe-vermeg'
    // Déclaration sincère : les tests passent réellement, et le contrôle négatif NC-1
    // repose sur le fait qu'un candidat qui les casse soit rejeté.
    skipTests       = false
    zapTargetUrl    = 'http://app-test-pfe-vermeg:8080'
}
