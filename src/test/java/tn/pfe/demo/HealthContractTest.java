package tn.pfe.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le contrat de santé est ce que la validation de déploiement interroge. Il doit être
 * exact à l'octet et stable dans le temps, sinon une comparaison de corps attendu est
 * impossible.
 */
class HealthContractTest {

    @Test
    @DisplayName("GET /health renvoie exactement {\"status\":\"UP\"}")
    void healthBodyIsExact() {
        assertEquals("{\"status\":\"UP\"}", App.health());
    }

    @Test
    @DisplayName("Le corps de santé est déterministe entre deux appels")
    void healthIsDeterministic() {
        // Un champ variable (horodatage, durée de fonctionnement) rendrait la validation
        // de déploiement non comparable. On le vérifie plutôt que de l'espérer.
        assertEquals(App.health(), App.health());
    }

    @Test
    @DisplayName("Le serveur démarre réellement et sert /health en 200")
    void serverServesHealthOverHttp() throws Exception {
        int port = freePort();
        HttpServer server = App.start("127.0.0.1", port);
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/health")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            assertEquals("{\"status\":\"UP\"}", response.body());
            assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("application/json"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("Le serveur refuse une adresse de liaison non autorisée")
    void serverRejectsForeignBindAddress() {
        // Un binding arbitraire exposerait une application volontairement vulnérable.
        // Le refus est une propriété de sûreté, pas une validation d'entrée cosmétique.
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
            () -> App.start("10.0.0.1", 18080));
    }

    private static int freePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) { return socket.getLocalPort(); }
    }
}
