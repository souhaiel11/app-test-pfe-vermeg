package tn.pfe.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Comportement métier porté par les deux dépendances de scénario.
 *
 * Ces tests existent pour une raison précise : après une mise à niveau, la plateforme
 * doit prouver que la sécurité s'est améliorée ET que le comportement est préservé.
 * Sans assertion sur le contrat de `normalizeSpace`, une mise à niveau de commons-lang3
 * qui changerait ce comportement passerait inaperçue.
 */
class CatalogBehaviourTest {

    @Test
    @DisplayName("displayLabel réduit les espaces internes et coupe les bords (commons-lang3)")
    void displayLabelNormalizesSpacing() {
        assertEquals("Carte bancaire virtuelle", ProductCatalog.displayLabel("Carte  bancaire   virtuelle"));
        assertEquals("a b", ProductCatalog.displayLabel("  a \t\n b  "));
        assertEquals("", ProductCatalog.displayLabel("   "));
    }

    @Test
    @DisplayName("displayLabel accepte une entrée nulle sans lever")
    void displayLabelAcceptsNull() {
        // Contrat de commons-lang3 : null en entrée, null en sortie. Tester cette
        // frontière protège contre une mise à niveau qui déciderait de lever.
        assertEquals(null, ProductCatalog.displayLabel(null));
    }

    @Test
    @DisplayName("Un produit refuse un prix négatif ou un stock négatif")
    void productRejectsInvalidAmounts() {
        assertThrows(IllegalArgumentException.class, () -> new Product("P-9", "x", -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Product("P-9", "x", 0, -1));
        assertThrows(IllegalArgumentException.class, () -> new Product(" ", "x", 0, 0));
    }

    @Test
    @DisplayName("La valeur immobilisée d'un produit est le prix multiplié par le stock")
    void stockValueIsPriceTimesStock() {
        assertEquals(2500, new Product("P-9", "x", 500, 5).getStockValueCents());
        assertEquals(0, new Product("P-9", "x", 500, 0).getStockValueCents());
    }

    @Test
    @DisplayName("La sérialisation JSON conserve l'ordre des clés (jackson-databind)")
    void jsonPreservesKeyOrder() {
        // L'ordre stable rend les réponses comparables d'un build à l'autre. Une mise à
        // niveau de jackson qui réordonnerait les clés casserait cette propriété, et ce
        // test la rend visible au lieu de la laisser deviner.
        String json = Json.write(Json.ordered("b", 1, "a", 2, "c", 3));
        assertEquals("{\"b\":1,\"a\":2,\"c\":3}", json);
    }

    @Test
    @DisplayName("L'échappement JSON suit le contrat de commons-text (DEMO-001)")
    void jsonEscapesSpecialCharacters() {
        // C'est l'assertion qui rend la mise à niveau DEMO-001 vérifiable : si
        // commons-text changeait sa façon d'échapper, le candidat casserait ce test et
        // serait rejeté, même avec un build vert.
        assertEquals("{\"k\":\"a\\\"b\"}", Json.write(Json.ordered("k", "a\"b")));
        assertEquals("{\"k\":\"a\\\\b\"}", Json.write(Json.ordered("k", "a\\b")));
        assertEquals("{\"k\":\"a\\nb\"}", Json.write(Json.ordered("k", "a\nb")));
    }

    @Test
    @DisplayName("Un type non sérialisable est refusé plutôt que converti")
    void unserializableTypeIsRejected() {
        // Un `toString()` de secours produirait un JSON faux mais syntaxiquement
        // valide — le pire des deux mondes.
        assertThrows(IllegalArgumentException.class, () -> Json.write(new Object()));
        assertThrows(IllegalArgumentException.class, () -> Json.ordered("impair"));
    }

    @Test
    @DisplayName("byId ignore les espaces de bord mais pas la casse")
    void byIdTrimsButIsCaseSensitive() {
        assertTrue(ProductCatalog.byId("  P-001  ").isPresent());
        assertTrue(ProductCatalog.byId("p-001").isEmpty());
        assertTrue(ProductCatalog.byId(null).isEmpty());
    }
}
