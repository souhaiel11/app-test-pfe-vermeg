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

    /**
     * Aiguillage par type. Chaque forme composée est déléguée, de sorte que
     * cette méthode reste une liste de cas lisible d'un seul coup d'œil — et
     * que la complexité de chaque branche vive à côté de son propre code.
     */
    private static void append(StringBuilder out, Object value) {
        if (value == null) { out.append("null"); return; }
        if (value instanceof String text) { appendString(out, text); return; }
        if (isPrimitive(value)) { out.append(value); return; }
        if (value instanceof Map<?, ?> map) { appendObject(out, map); return; }
        if (value instanceof Collection<?> items) { appendArray(out, items); return; }
        throw new IllegalArgumentException("Type non sérialisable : " + value.getClass().getName());
    }

    /** Les seuls scalaires acceptés. Tout le reste est refusé par `append`. */
    private static boolean isPrimitive(Object value) {
        return value instanceof Integer || value instanceof Long || value instanceof Boolean;
    }

    private static void appendString(StringBuilder out, String text) {
        out.append('"').append(StringEscapeUtils.escapeJson(text)).append('"');
    }

    private static void appendObject(StringBuilder out, Map<?, ?> map) {
        out.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) out.append(',');
            first = false;
            appendString(out, String.valueOf(entry.getKey()));
            out.append(':');
            append(out, entry.getValue());
        }
        out.append('}');
    }

    private static void appendArray(StringBuilder out, Collection<?> items) {
        out.append('[');
        boolean first = true;
        for (Object item : items) {
            if (!first) out.append(',');
            first = false;
            append(out, item);
        }
        out.append(']');
    }

    /** Vue sérialisable d'un produit : champs explicites, jamais de réflexion implicite. */
    public static Map<String, Object> product(Product p) {
        return ordered(
            "id", p.id(),
            "label", ProductCatalog.displayLabel(p.label()),
            "priceCents", p.priceCents(),
            "stock", p.stock(),
            "stockValueCents", p.stockValueCents());
    }
}
