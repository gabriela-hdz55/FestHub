package org.festhub;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.UUID;

class BookingCategory {

        static void run(String action, Scanner scanner) {
                try {
                        switch (action) {
                                case "1" -> create(scanner);
                                case "2" -> read(scanner);
                                case "3" -> update(scanner);
                                case "4" -> delete(scanner);
                                case "5" -> manageEvents(scanner);
                                default -> System.out.println("Allowed actions: 1, 2, 3, 4, 5");
                        }
                } catch (SQLException | IllegalArgumentException e) {
                        System.out.println("Error: " + e.getMessage());
                }
        }

        // ---------------------------------------------------------
        // CRUD
        // ---------------------------------------------------------

        private static void create(Scanner scanner) throws SQLException {
                System.out.print("Category name: ");
                String name = scanner.nextLine().trim();

                System.out.print("Description: ");
                String description = scanner.nextLine().trim();

                validate(name, description);

                UUID id = UUID.randomUUID();

                Database.execute(
                                "INSERT INTO booking_category(id, name, description) VALUES (?, ?, ?)",
                                id.toString(), name, description);

                System.out.println("Category created: " + id);
        }

        private static void read(Scanner scanner) throws SQLException {
                UUID id = chooseCategory(scanner);
                if (id == null)
                        return;

                String sql = """
                                SELECT name, description
                                FROM booking_category
                                WHERE id = ?
                                """;

                try (Connection connection = Database.connect();
                                PreparedStatement statement = connection.prepareStatement(sql)) {

                        statement.setString(1, id.toString());

                        try (ResultSet result = statement.executeQuery()) {
                                if (!result.next()) {
                                        System.out.println("Category not found.");
                                        return;
                                }

                                System.out.println("Name: " + result.getString("name"));
                                System.out.println("Description: " + result.getString("description"));
                        }

                        printEvents(connection, id);
                }
        }

        private static void update(Scanner scanner) throws SQLException {
                UUID id = chooseCategory(scanner);
                if (id == null)
                        return;

                System.out.print("New name: ");
                String name = scanner.nextLine().trim();

                System.out.print("New description: ");
                String description = scanner.nextLine().trim();

                validate(name, description);

                int updated = Database.execute(
                                """
                                                UPDATE booking_category
                                                SET name = ?, description = ?
                                                WHERE id = ?
                                                """,
                                name, description, id.toString());

                if (updated == 0) {
                        System.out.println("Category not found.");
                        return;
                }

                System.out.println("Category updated.");
        }

        private static void delete(Scanner scanner) throws SQLException {
                UUID id = chooseCategory(scanner);
                if (id == null)
                        return;

                int deleted = Database.execute(
                                "DELETE FROM booking_category WHERE id = ?",
                                id.toString());

                if (deleted == 0) {
                        System.out.println("Category not found.");
                        return;
                }

                System.out.println("Category deleted.");
        }

        // ---------------------------------------------------------
        // EVENT MANAGEMENT
        // ---------------------------------------------------------

        private static void manageEvents(Scanner scanner) throws SQLException {
                UUID categoryId = chooseCategory(scanner);
                if (categoryId == null)
                        return;

                System.out.println();
                System.out.println("1 = Add Event");
                System.out.println("2 = Remove Event");
                System.out.println("3 = Cancel");
                System.out.print("Choose: ");

                switch (scanner.nextLine().trim()) {
                        case "1" -> addEvent(scanner, categoryId);
                        case "2" -> removeEvent(scanner, categoryId);
                        case "3" -> {
                        }
                        default -> System.out.println("Invalid option.");
                }
        }

        private static void addEvent(Scanner scanner, UUID categoryId)
                        throws SQLException {

                System.out.println();
                System.out.println("Available Events:");

                if (!Database.list("concert_event")) {
                        System.out.println("No concert events found.");
                        return;
                }

                System.out.print("Event ID: ");
                String eventId = scanner.nextLine().trim();

                Database.execute(
                                "INSERT INTO event_category(event_id, category_id) VALUES (?, ?)",
                                eventId, categoryId.toString());

                System.out.println("Event added to category.");
        }

        private static void removeEvent(Scanner scanner, UUID categoryId)
                        throws SQLException {

                try (Connection connection = Database.connect()) {
                        if (!printEvents(connection, categoryId)) {
                                return;
                        }
                }

                System.out.print("Event ID to remove: ");
                String eventId = scanner.nextLine().trim();

                int removed = Database.execute(
                                """
                                                DELETE FROM event_category
                                                WHERE event_id = ? AND category_id = ?
                                                """,
                                eventId, categoryId.toString());

                if (removed == 0) {
                        System.out.println("That event is not in this category.");
                        return;
                }

                // If the user removed the category's final event,
                // the category is now an orphan and is deleted.
                int categoryDeleted = Database.execute(
                                """
                                                DELETE FROM booking_category
                                                WHERE id = ?
                                                AND NOT EXISTS (
                                                    SELECT 1
                                                    FROM event_category
                                                    WHERE category_id = ?
                                                )
                                                """,
                                categoryId.toString(), categoryId.toString());

                System.out.println("Event removed.");

                if (categoryDeleted > 0) {
                        System.out.println(
                                        "Category had no remaining events and was automatically deleted.");
                }
        }

        // ---------------------------------------------------------
        // HELPERS
        // ---------------------------------------------------------

        private static UUID chooseCategory(Scanner scanner) throws SQLException {
                System.out.println();
                System.out.println("Booking Categories:");

                if (!Database.list("booking_category")) {
                        System.out.println("No booking categories found.");
                        return null;
                }

                System.out.print("Category ID: ");
                String input = scanner.nextLine().trim();

                try {
                        return UUID.fromString(input);
                } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Invalid category UUID.");
                }
        }

        private static boolean printEvents(
                        Connection connection,
                        UUID categoryId) throws SQLException {

                String sql = """
                                SELECT ce.id, ce.name
                                FROM concert_event ce
                                JOIN event_category ec ON ce.id = ec.event_id
                                WHERE ec.category_id = ?
                                """;

                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        statement.setString(1, categoryId.toString());

                        try (ResultSet result = statement.executeQuery()) {
                                System.out.println();
                                System.out.println("Events:");

                                boolean found = false;

                                while (result.next()) {
                                        found = true;
                                        System.out.println(
                                                        result.getString("name") + " - " + result.getString("id"));
                                }

                                if (!found) {
                                        System.out.println("None");
                                }

                                return found;
                        }
                }
        }

        private static void validate(String name, String description) {
                if (name.isBlank()) {
                        throw new IllegalArgumentException("Name cannot be empty.");
                }

                if (name.length() > 2000) {
                        throw new IllegalArgumentException(
                                        "Name cannot exceed 2000 characters.");
                }

                if (description.length() > 10000) {
                        throw new IllegalArgumentException(
                                        "Description cannot exceed 10000 characters.");
                }
        }
}