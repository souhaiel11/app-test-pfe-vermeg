package tn.pfe.demo;

/**
 * Un produit du catalogue de démonstration.
 *
 * Données synthétiques uniquement : aucun identifiant réel, aucun prix réel, aucune
 * donnée de production. Les montants sont en centimes pour éviter toute arithmétique
 * flottante — un prix doit être exact, pas approché.
 */
public final class Product {

    private final String id;
    private final String label;
    private final int priceCents;
    private final int stock;

    public Product(String id, String label, int priceCents, int stock) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id requis");
        if (label == null) throw new IllegalArgumentException("label requis");
        if (priceCents < 0) throw new IllegalArgumentException("priceCents négatif");
        if (stock < 0) throw new IllegalArgumentException("stock négatif");
        this.id = id;
        this.label = label;
        this.priceCents = priceCents;
        this.stock = stock;
    }

    public String getId() { return id; }
    public String getLabel() { return label; }
    public int getPriceCents() { return priceCents; }
    public int getStock() { return stock; }

    /** Valeur immobilisée par ce produit, en centimes. */
    public int getStockValueCents() { return priceCents * stock; }
}
