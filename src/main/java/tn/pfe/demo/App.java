package tn.pfe.demo;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * APPLICATION DE DÉMONSTRATION INTENTIONNELLEMENT VULNÉRABLE — USAGE DE TEST UNIQUEMENT.
 *
 * Serveur HTTP minimal bâti sur `com.sun.net.httpserver`, fourni par le JDK : aucune
 * dépendance de framework. Ce choix est délibéré — les seules dépendances déclarées sont
 * celles qui portent les scénarios de remédiation, de sorte qu'un scan ne mélange jamais
 * les vulnérabilités de démonstration avec celles d'un framework.
 *
 * Périmètre de sûreté : aucune exécution de code fournie par l'appelant, aucun accès au
 * système de fichiers, aucun appel sortant, aucun identifiant, données synthétiques
 * uniquement. Les seules méthodes acceptées sont GET et HEAD.
 */
public final class App {

    public static final String NOTICE =
        "APPLICATION DE DEMONSTRATION INTENTIONNELLEMENT VULNERABLE — USAGE DE TEST UNIQUEMENT";
    public static final String APPLICATION_NAME = "app-test-pfe-vermeg";
    public static final String APPLICATION_VERSION = "1.0.0";

    private static final String JSON_TYPE = "application/json; charset=utf-8";

    private App() { }

    // ── Réponses, construites séparément du transport pour être testables ──────────

    /**
     * Contrat de santé. Déterministe : exactement les mêmes octets à chaque appel.
     * Aucun horodatage, aucune durée de fonctionnement, aucun champ variable — la
     * validation de déploiement compare un corps attendu, elle ne peut pas comparer
     * une valeur qui change.
     */
    public static String health() {
        return Json.write(Json.ordered("status", "UP"));
    }

    public static String info() {
        return Json.write(Json.ordered(
            "application", APPLICATION_NAME,
            "version", APPLICATION_VERSION,
            "notice", NOTICE,
            "dataSource", "synthetic-in-memory"));
    }

    public static String products() {
        List<Map<String, Object>> items = ProductCatalog.all().stream()
            .map(Json::product).collect(Collectors.toList());
        // Deux sources pour la même grandeur, nommées séparément : le calcul en
        // mémoire est toujours disponible ; l'agrégat SQL (DEMO-003) peut être
        // absent, et l'est alors explicitement — jamais remplacé en silence.
        java.util.OptionalInt sql = ProductCatalog.totalStockValueCentsFromDatabase();
        return Json.write(Json.ordered(
            "count", items.size(),
            "totalStockValueCents", ProductCatalog.totalStockValueCents(),
            "totalStockValueCentsSql", sql.isPresent() ? sql.getAsInt() : null,
            "items", items));
    }

    /** `Optional.empty()` signifie « inconnu » : l'appelant répond 404, jamais un objet vide. */
    public static Optional<String> product(String id) {
        return ProductCatalog.byId(id).map(p -> Json.write(Json.product(p)));
    }

    public static String error(String code) {
        return Json.write(Json.ordered("error", code));
    }

    // ── Transport ─────────────────────────────────────────────────────────────────

    /** Résultat d'un routage : statut et corps, sans dépendance au serveur. */
    public record Response(int status, String body) { }

    /**
     * Routage pur. Isolé du serveur HTTP pour que les tests exercent exactement le même
     * code que la production, sans ouvrir de port.
     */
    public static Response route(String method, String path) {
        if (!"GET".equals(method) && !"HEAD".equals(method)) {
            return new Response(405, error("method_not_allowed"));
        }
        if ("/health".equals(path)) return new Response(200, health());
        if ("/api/info".equals(path)) return new Response(200, info());
        if ("/api/products".equals(path)) return new Response(200, products());

        if (path.startsWith("/api/products/")) {
            String id = path.substring("/api/products/".length());
            // Un identifiant vide n'est pas « le premier produit » : c'est une requête
            // mal formée, et on le dit.
            if (id.isBlank() || id.contains("/")) return new Response(404, error("not_found"));
            return product(id)
                .map(body -> new Response(200, body))
                .orElseGet(() -> new Response(404, error("not_found")));
        }
        return new Response(404, error("not_found"));
    }

    public static HttpServer start(String bind, int port) throws IOException {
        // Boucle locale par défaut. `0.0.0.0` n'est accepté que dans un conteneur, où le
        // port n'est publié que sur la boucle locale de l'hôte (voir compose.yaml).
        if (!"127.0.0.1".equals(bind) && !"0.0.0.0".equals(bind)) {
            throw new IllegalArgumentException("Adresse de liaison non autorisée : " + bind);
        }
        HttpServer server = HttpServer.create(new InetSocketAddress(bind, port), 0);
        server.createContext("/", App::handle);
        server.start();
        return server;
    }

    private static void handle(HttpExchange exchange) throws IOException {
        try {
            Response response = route(exchange.getRequestMethod(), exchange.getRequestURI().getPath());
            byte[] bytes = response.body().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", JSON_TYPE);
            exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            if ("HEAD".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(response.status(), -1);
            } else {
                exchange.sendResponseHeaders(response.status(), bytes.length);
                try (var out = exchange.getResponseBody()) { out.write(bytes); }
            }
        } finally {
            exchange.close();
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println("AVERTISSEMENT — " + NOTICE);
        String bind = System.getProperty("app.bind", "127.0.0.1");
        int port = Integer.parseInt(System.getProperty("app.port", "8080"));
        HttpServer server = start(bind, port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(0)));
        System.out.println(APPLICATION_NAME + " écoute sur " + bind + ":" + port);
    }
}
