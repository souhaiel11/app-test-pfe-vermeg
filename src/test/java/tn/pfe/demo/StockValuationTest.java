package tn.pfe.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalInt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Comportement porté par la dépendance DEMO-003 (`h2`).
 *
 * Ces tests existent pour que la mise à niveau de H2 soit vérifiable : si
 * l'agrégation SQL cessait de donner le même résultat que le calcul en
 * mémoire, un candidat qui monte la version serait rejeté — même avec la CVE
 * corrigée.
 */
class StockValuationTest {

    @Test
    @DisplayName("L'agrégat SQL donne exactement le même total que le calcul en mémoire")
    void sqlAggregateMatchesInMemorySum() throws Exception {
        assertEquals(ProductCatalog.totalStockValueCents(),
            StockValuation.totalStockValueCents(ProductCatalog.all()));
    }

    @Test
    @DisplayName("La valorisation SQL vaut 173 300 centimes")
    void sqlTotalIsExact() {
        OptionalInt total = ProductCatalog.totalStockValueCentsFromDatabase();
        assertTrue(total.isPresent(), "la valorisation SQL doit être disponible");
        assertEquals(173_300, total.getAsInt());
    }

    @Test
    @DisplayName("La réponse /api/products expose les deux valorisations")
    void responseCarriesBothValuations() {
        String body = App.products();
        assertTrue(body.contains("\"totalStockValueCents\":173300"), body);
        assertTrue(body.contains("\"totalStockValueCentsSql\":173300"), body);
    }

    @Test
    @DisplayName("La version de H2 réellement chargée est lisible")
    void loadedDatabaseVersionIsReadable() throws Exception {
        String version = StockValuation.databaseVersion();
        assertTrue(version != null && !version.isBlank(), "version H2 illisible");
    }
}
