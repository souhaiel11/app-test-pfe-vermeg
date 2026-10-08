package tn.pfe.demo;

import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

/**
 * Catalogue en mémoire, synthétique et immuable.
 *
 * `commons-lang3` est utilisé ici volontairement : la dépendance DEMO-002 doit être
 * réellement employée par l'application, sinon sa mise à niveau ne prouverait rien sur
 * la préservation du comportement. `StringUtils.normalizeSpace` est le point de contact.
 */
public final class ProductCatalog {

    private static final List<Product> ITEMS = List.of(
        new Product("P-001", "Carte  bancaire   virtuelle", 1250, 40),
        new Product("P-002", "Mandat de  gestion", 9900, 7),
        new Product("P-003", "Rapport   de conformité", 4500, 12));

    private ProductCatalog() { }

    public static List<Product> all() { return ITEMS; }

    public static Optional<Product> byId(String id) {
        if (id == null) return Optional.empty();
        String wanted = id.trim();
        return ITEMS.stream().filter(p -> p.getId().equals(wanted)).findFirst();
    }

    /**
     * Libellé d'affichage : espaces internes réduits à un seul, bords supprimés.
     * Délégué à commons-lang3 — c'est le comportement que la mise à niveau DEMO-002
     * doit préserver à l'identique.
     */
    public static String displayLabel(String rawLabel) {
        return StringUtils.normalizeSpace(rawLabel);
    }

    /** Somme des valeurs immobilisées, en centimes, calculée en mémoire. */
    public static int totalStockValueCents() {
        return ITEMS.stream().mapToInt(Product::getStockValueCents).sum();
    }

    /**
     * Même somme, calculée par agrégation SQL (DEMO-003, dépendance `h2`).
     *
     * En cas d'échec de la base, on ne renvoie PAS le calcul en mémoire : une
     * valorisation indisponible doit se voir, pas se déguiser en valeur juste.
     * L'appelant décide quoi afficher.
     */
    public static java.util.OptionalInt totalStockValueCentsFromDatabase() {
        try {
            return java.util.OptionalInt.of(StockValuation.totalStockValueCents(ITEMS));
        } catch (Exception e) {
            return java.util.OptionalInt.empty();
        }
    }
}
