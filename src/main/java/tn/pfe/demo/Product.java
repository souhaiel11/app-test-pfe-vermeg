package tn.pfe.demo;

/**
 * Un produit du catalogue de démonstration.
 *
 * Déclaré comme `record` : c'est une valeur immuable, et le record exprime
 * exactement cela — pas de setter possible, `equals`/`hashCode`/`toString`
 * cohérents par construction. Le constructeur compact porte les invariants,
 * donc un produit invalide ne peut pas exister.
 *
 * Données synthétiques uniquement : aucun identifiant réel, aucun prix réel,
 * aucune donnée de production. Les montants sont en centimes pour éviter toute
 * arithmétique flottante — un prix doit être exact, pas approché.
 */
public record Product(String id, String label, int priceCents, int stock) {

    public Product {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id requis");
        if (label == null) throw new IllegalArgumentException("label requis");
        if (priceCents < 0) throw new IllegalArgumentException("priceCents négatif");
        if (stock < 0) throw new IllegalArgumentException("stock négatif");
    }

    /** Valeur immobilisée par ce produit, en centimes. */
    public int stockValueCents() {
        return priceCents * stock;
    }
}
