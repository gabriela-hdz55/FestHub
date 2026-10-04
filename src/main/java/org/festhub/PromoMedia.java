package org.festhub;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.UUID;

class PromoMedia {

        static void run(String action, Scanner scanner) {
                try {
                        switch (action) {
                                case "1" -> create(scanner);
                                case "2" -> read(scanner);
                                case "3" -> update(scanner);
                                case "4" -> delete(scanner);
                                default -> System.out.println("Allowed actions: 1, 2, 3, 4");
                        }
                } catch (SQLException | IllegalArgumentException e) {
                        System.out.println("Error: " + e.getMessage());
                }
        }

        // ---------------------------------------------------------
        // CRUD
        // ---------------------------------------------------------

        private static void create(Scanner scanner) throws SQLException {
                System.out.println();
                System.out.println("Available Events:");

                if (!Database.list("concert_event")) {
                        System.out.println("No concert events found.");
                        return;
                }

                System.out.print("Event ID: ");
                String eventId = scanner.nextLine().trim();

                System.out.print("Image URL: ");
                String imageUrl = scanner.nextLine().trim();

                validate(eventId, imageUrl);

                UUID id = UUID.randomUUID();

                Database.execute(
                                "INSERT INTO promo_media(id, event_id, image_url) VALUES (?, ?, ?)",
                                id.toString(), eventId, imageUrl);

                System.out.println("Promo media created: " + id);
        }

        private static void read(Scanner scanner) throws SQLException {
                UUID id = chooseMedia(scanner);
                if (id == null)
                        return;

                String sql = """
                                SELECT pm.event_id, pm.image_url, ce.name AS event_name
                                FROM promo_media pm
                                JOIN concert_event ce ON ce.id = pm.event_id
                                WHERE pm.id = ?
                                """;

                try (Connection connection = Database.connect();
                                PreparedStatement statement = connection.prepareStatement(sql)) {

                        statement.setString(1, id.toString());

                        try (ResultSet result = statement.executeQuery()) {
                                if (!result.next()) {
                                        System.out.println("Promo media not found.");
                                        return;
                                }

                                System.out.println("Media ID: " + id);
                                System.out.println("Event: " + result.getString("event_name")
                                                + " - " + result.getString("event_id"));
                                System.out.println("Image URL: " + result.getString("image_url"));
                        }
                }
        }

        private static void update(Scanner scanner) throws SQLException {
                UUID id = chooseMedia(scanner);
                if (id == null)
                        return;

                System.out.println();
                System.out.println("Available Events:");

                if (!Database.list("concert_event")) {
                        System.out.println("No concert events found.");
                        return;
                }

                System.out.print("New event ID: ");
                String eventId = scanner.nextLine().trim();

                System.out.print("New image URL: ");
                String imageUrl = scanner.nextLine().trim();

                validate(eventId, imageUrl);

                int updated = Database.execute(
                                """
                                                UPDATE promo_media
                                                SET event_id = ?, image_url = ?
                                                WHERE id = ?
                                                """,
                                eventId, imageUrl, id.toString());

                if (updated == 0) {
                        System.out.println("Promo media not found.");
                        return;
                }

                System.out.println("Promo media updated.");
        }

        private static void delete(Scanner scanner) throws SQLException {
                UUID id = chooseMedia(scanner);
                if (id == null)
                        return;

                int deleted = Database.execute(
                                "DELETE FROM promo_media WHERE id = ?",
                                id.toString());

                if (deleted == 0) {
                        System.out.println("Promo media not found.");
                        return;
                }

                System.out.println("Promo media deleted.");
        }

        // ---------------------------------------------------------
        // HELPERS
        // ---------------------------------------------------------

        private static UUID chooseMedia(Scanner scanner) throws SQLException {
                System.out.println();
                System.out.println("Promo Media:");

                if (!listMedia()) {
                        System.out.println("No promo media found.");
                        return null;
                }

                System.out.print("Media ID: ");
                String input = scanner.nextLine().trim();

                try {
                        return UUID.fromString(input);
                } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Invalid media UUID.");
                }
        }

        private static boolean listMedia() throws SQLException {
                String sql = """
                                SELECT pm.id, pm.image_url, ce.name AS event_name
                                FROM promo_media pm
                                JOIN concert_event ce ON ce.id = pm.event_id
                                ORDER BY ce.name, pm.image_url
                                """;

                try (Connection connection = Database.connect();
                                PreparedStatement statement = connection.prepareStatement(sql);
                                ResultSet result = statement.executeQuery()) {

                        boolean found = false;

                        while (result.next()) {
                                found = true;
                                System.out.println(
                                                result.getString("event_name")
                                                                + " - "
                                                                + result.getString("image_url")
                                                                + " - "
                                                                + result.getString("id"));
                        }

                        return found;
                }
        }

        private static void validate(String eventId, String imageUrl) {
                if (eventId.isBlank()) {
                        throw new IllegalArgumentException("Event ID cannot be empty.");
                }

                try {
                        UUID.fromString(eventId);
                } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Invalid event UUID.");
                }

                if (imageUrl.isBlank()) {
                        throw new IllegalArgumentException("Image URL cannot be empty.");
                }

                if (!isWebUrl(imageUrl)) {
                        throw new IllegalArgumentException("Image URL must be a valid HTTP or HTTPS URL.");
                }
        }

        private static boolean isWebUrl(String value) {
                return value.matches(
                                "https?://[A-Za-z0-9.-]+\\.[A-Za-z]{2,}(:[0-9]{1,5})?(/[A-Za-z0-9._~:/?#\\[\\]@!$&'()*+,;=%-]*)?");
        }
}
