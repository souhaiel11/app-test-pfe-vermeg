package tn.pfe.demo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Valorisation du stock, calculée par une agrégation SQL.
 *
 * Pourquoi une base ici : la dépendance DEMO-003 (`h2`) doit être réellement
 * employée par l'application, sinon sa mise à niveau ne prouverait rien sur la
 * préservation du comportement. Le total affiché par `/api/products` passe donc
 * par un vrai `SUM(...)`, et un test compare ce résultat au calcul en mémoire :
 * si une mise à niveau de H2 changeait ce comportement, le test le verrait.
 *
 * Périmètre de sûreté, volontairement étroit :
 *  - base strictement en mémoire (`jdbc:h2:mem:`), jamais un fichier ;
 *  - aucune URL, aucun fragment de SQL ne vient de l'appelant ;
 *  - requêtes préparées uniquement ;
 *  - aucune console H2, aucun serveur, aucun port ouvert ;
 *  - données synthétiques, les mêmes que le catalogue en mémoire.
 */
public final class StockValuation {

    /** En mémoire et privée au processus : aucune persistance, aucun accès réseau. */
    private static final String URL = "jdbc:h2:mem:catalogue;DB_CLOSE_DELAY=-1";

    private StockValuation() { }

    /**
     * Somme des valeurs immobilisées, en centimes, calculée par SQL.
     *
     * Lève `IllegalStateException` plutôt que de renvoyer 0 si l'agrégat est
     * introuvable : un total absent ne doit pas se faire passer pour un total nul.
     */
    public static int totalStockValueCents(List<Product> products) throws SQLException {
        try (Connection connection = DriverManager.getConnection(URL)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("DROP TABLE IF EXISTS product");
                statement.execute("CREATE TABLE product ("
                    + "id VARCHAR(32) PRIMARY KEY, price_cents INT NOT NULL, stock INT NOT NULL)");
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO product (id, price_cents, stock) VALUES (?, ?, ?)")) {
                for (Product product : products) {
                    insert.setString(1, product.getId());
                    insert.setInt(2, product.getPriceCents());
                    insert.setInt(3, product.getStock());
                    insert.addBatch();
                }
                insert.executeBatch();
            }
            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery(
                     "SELECT SUM(price_cents * stock) FROM product")) {
                if (!result.next()) throw new IllegalStateException("Agrégat de valorisation introuvable");
                int total = result.getInt(1);
                if (result.wasNull()) throw new IllegalStateException("Valorisation nulle inattendue");
                return total;
            }
        }
    }

    /** Version de H2 réellement chargée, lue dans les métadonnées du pilote. */
    public static String databaseVersion() throws SQLException {
        try (Connection connection = DriverManager.getConnection(URL)) {
            return connection.getMetaData().getDatabaseProductVersion();
        }
    }
}
