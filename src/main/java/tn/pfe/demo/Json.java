package tn.pfe.demo;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.text.StringEscapeUtils;

/**
 * Écriture JSON des réponses.
 *
 * Aucune bibliothèque de sérialisation : les réponses de cette application sont des
 * cartes ordonnées de types simples, et un écrivain de trente lignes suffit. Ce choix
 * évite d'embarquer un framework dont les vulnérabilités se mélangeraient à celles des
 * scénarios de démonstration — un scan doit rester lisible.
 *
 * L'échappement des chaînes est délégué à `commons-text`
 * (`StringEscapeUtils.escapeJson`) : c'est la dépendance DEMO-001, et elle est ainsi
 * réellement sur le chemin de chaque réponse. Une mise à niveau qui changerait son
 * comportement d'échappement ferait échouer les tests — ce qui est précisément ce que
 * la plateforme doit détecter.
 *
 * L'ordre des clés est préservé (`LinkedHashMap`) pour que les réponses soient
 * déterministes, donc comparables d'un build à l'autre.
 */
public final class Json {

    private Json() { }

    /** Fabrique une carte ordonnée, pour des réponses à clés stables. */
    public static Map<String, Object> ordered(Object... keyValues) {
        if (keyValues.length % 2 != 0) throw new IllegalArgumentException("paires clé/valeur attendues");
        Map<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) out.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        return out;
    }

    /**
     * Sérialise une carte, une collection, un nombre, un booléen, une chaîne ou `null`.
     * Tout autre type est refusé plutôt que converti en `toString()` : un JSON produit
     * par accident serait pire qu'une erreur franche.
     */
    public static String write(Object value) {
        StringBuilder out = new StringBuilder(128);
        append(out, value);
        return out.toString();
    }

    private static void append(StringBuilder out, Object value) {
        if (value == null) { out.append("null"); return; }
        if (value instanceof String s) { out.append('"').append(StringEscapeUtils.escapeJson(s)).append('"'); return; }
        if (value instanceof Integer || value instanceof Long || value instanceof Boolean) { out.append(value); return; }
        if (value instanceof Map<?, ?> map) {
            out.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (!first) out.append(',');
                first = false;
                out.append('"').append(StringEscapeUtils.escapeJson(String.valueOf(e.getKey()))).append("\":");
                append(out, e.getValue());
            }
            out.append('}');
            return;
        }
        if (value instanceof Collection<?> items) {
            out.append('[');
            boolean first = true;
            for (Object item : items) {
                if (!first) out.append(',');
                first = false;
                append(out, item);
            }
            out.append(']');
            return;
        }
        throw new IllegalArgumentException("Type non sérialisable : " + value.getClass().getName());
    }

    /** Vue sérialisable d'un produit : champs explicites, jamais de réflexion implicite. */
    public static Map<String, Object> product(Product p) {
        return ordered(
            "id", p.getId(),
            "label", ProductCatalog.displayLabel(p.getLabel()),
            "priceCents", p.getPriceCents(),
            "stock", p.getStock(),
            "stockValueCents", p.getStockValueCents());
    }
}
