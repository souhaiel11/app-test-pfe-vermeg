package tn.pfe.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Comportement de l'API tel qu'il sera montré au jury. Ces tests sont le filet de
 * sécurité fonctionnel : une mise à niveau de dépendance qui casserait la sérialisation
 * ou le routage doit faire échouer l'un d'eux, et donc faire rejeter le candidat.
 */
class ProductApiTest {

    @Test
    @DisplayName("GET /api/products expose les trois produits synthétiques")
    void productsListHasThreeItems() {
        App.Response response = App.route("GET", "/api/products");
        assertEquals(200, response.status());
        assertTrue(response.body().startsWith("{\"count\":3,"), response.body());
        assertTrue(response.body().contains("\"P-001\""));
        assertTrue(response.body().contains("\"P-003\""));
    }

    @Test
    @DisplayName("La valeur immobilisée totale vaut 173 300 centimes")
    void totalStockValueIsExact() {
        // 1250*40 + 9900*7 + 4500*12 — arithmétique entière, aucun flottant.
        assertEquals(173_300, ProductCatalog.totalStockValueCents());
        assertTrue(App.products().contains("\"totalStockValueCents\":173300"));
    }

    @Test
    @DisplayName("GET /api/products/P-002 renvoie ce produit et ses champs attendus")
    void singleProductHasExpectedShape() {
        App.Response response = App.route("GET", "/api/products/P-002");
        assertEquals(200, response.status());
        assertEquals(
            "{\"id\":\"P-002\",\"label\":\"Mandat de gestion\",\"priceCents\":9900,"
                + "\"stock\":7,\"stockValueCents\":69300}",
            response.body());
    }

    @Test
    @DisplayName("Un identifiant inconnu renvoie 404 not_found")
    void unknownProductIsNotFound() {
        App.Response response = App.route("GET", "/api/products/P-999");
        assertEquals(404, response.status());
        assertEquals("{\"error\":\"not_found\"}", response.body());
    }

    @Test
    @DisplayName("Un identifiant vide n'est pas traité comme le premier produit")
    void blankProductIdIsNotFound() {
        assertEquals(404, App.route("GET", "/api/products/").status());
        assertEquals(404, App.route("GET", "/api/products/%20").status());
    }

    @Test
    @DisplayName("Une méthode mutante est refusée en 405")
    void mutatingMethodIsRejected() {
        for (String method : new String[] {"POST", "PUT", "PATCH", "DELETE"}) {
            App.Response response = App.route(method, "/api/products");
            assertEquals(405, response.status(), method);
            assertEquals("{\"error\":\"method_not_allowed\"}", response.body());
        }
    }

    @Test
    @DisplayName("GET /api/info porte l'identité de l'application et l'avertissement")
    void infoCarriesIdentityAndNotice() {
        App.Response response = App.route("GET", "/api/info");
        assertEquals(200, response.status());
        assertTrue(response.body().contains("\"application\":\"app-test-pfe-vermeg\""));
        assertTrue(response.body().contains("\"version\":\"1.0.0\""));
        assertTrue(response.body().contains("INTENTIONNELLEMENT VULNERABLE"));
    }

    @Test
    @DisplayName("Un chemin inconnu renvoie 404")
    void unknownPathIsNotFound() {
        assertEquals(404, App.route("GET", "/admin").status());
        assertEquals(404, App.route("GET", "/").status());
    }
}
