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
                case "1" -> createBookingCategory(scanner);
                case "2" -> readBookingCategory(scanner);
                case "3" -> updateBookingCategory(scanner);
                case "4" -> deleteBookingCategory(scanner);
                default ->
                        System.out.println(
                                "Unknown action. Allowed: 1, 2, 3, 4"
                        );
            }
        } catch (SQLException exception) {
            System.out.println(
                    "Database error: " + exception.getMessage()
            );
        } catch (IllegalArgumentException exception) {
            System.out.println(
                    "Error: " + exception.getMessage()
            );
        }
    }

    // --------------------
    // CREATE
    // --------------------

    private static void createBookingCategory(
            Scanner scanner
    ) throws SQLException {

        System.out.print("Category Name: ");
        String name = scanner.nextLine().trim();

        System.out.print("Category Description: ");
        String description = scanner.nextLine().trim();

        validate(name, description);

        UUID categoryId = UUID.randomUUID();

        String sql = """
                INSERT INTO booking_category (
                    id,
                    name,
                    description
                )
                VALUES (?, ?, ?)
                """;

        try (Connection connection = Database.connect();
            PreparedStatement statement =
                    connection.prepareStatement(sql)) {

            statement.setString(1, categoryId.toString());
            statement.setString(2, name);
            statement.setString(3, description);

            statement.executeUpdate();
        }

        System.out.println();
        System.out.println("Festival Booking Category created.");
        System.out.println("Category ID: " + categoryId);
    }

    // --------------------
    // READ
    // --------------------

    private static void readBookingCategory(
            Scanner scanner
    ) throws SQLException {

        UUID categoryId = readCategoryId(scanner);

        // No categories currently exist.
        if (categoryId == null) {
            return;
        }

        String sql = """
                SELECT id, name, description
                FROM booking_category
                WHERE id = ?
                """;

        try (Connection connection = Database.connect();
            PreparedStatement statement =
                    connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    categoryId.toString()
            );

            try (ResultSet result = statement.executeQuery()) {

                if (!result.next()) {
                    System.out.println(
                            "Booking category not found."
                    );
                    return;
                }

                System.out.println();
                System.out.println("Festival Booking Category");
                System.out.println("-------------------------");

                System.out.println(
                        "Category ID: " +
                                result.getString("id")
                );

                System.out.println(
                        "Name: " +
                                result.getString("name")
                );

                System.out.println(
                        "Description: " +
                                result.getString("description")
                );

                printEventIds(
                        connection,
                        categoryId
                );
            }
        }
    }

    // --------------------
    // UPDATE
    // --------------------

    private static void updateBookingCategory(
            Scanner scanner
    ) throws SQLException {

        UUID categoryId = readCategoryId(scanner);

        if (categoryId == null) {
            return;
        }

        if (!exists(categoryId)) {
            System.out.println(
                    "Booking category not found."
            );
            return;
        }

        System.out.print("New category name: ");
        String name = scanner.nextLine().trim();

        System.out.print("New category description: ");
        String description = scanner.nextLine().trim();

        validate(name, description);

        String sql = """
                UPDATE booking_category
                SET name = ?,
                    description = ?
                WHERE id = ?
                """;

        try (Connection connection = Database.connect();
            PreparedStatement statement =
                    connection.prepareStatement(sql)) {

            statement.setString(1, name);
            statement.setString(2, description);
            statement.setString(
                    3,
                    categoryId.toString()
            );

            statement.executeUpdate();
        }

        System.out.println();
        System.out.println(
                "Festival Booking Category updated."
        );
    }

    // --------------------
    // DELETE
    // --------------------

    private static void deleteBookingCategory(
            Scanner scanner
    ) throws SQLException {

        UUID categoryId = readCategoryId(scanner);

        if (categoryId == null) {
            return;
        }

        String sql = """
                DELETE FROM booking_category
                WHERE id = ?
                """;

        try (Connection connection = Database.connect();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    categoryId.toString()
            );

            int rowsAffected = statement.executeUpdate();

            if (rowsAffected == 0) {
                System.out.println(
                        "Booking category not found."
                );
                return;
            }
        }

        System.out.println();
        System.out.println(
                "Festival Booking Category deleted."
        );
    }

    // --------------------
    // CATEGORY SELECTION
    // --------------------

    private static UUID readCategoryId(
            Scanner scanner
    ) throws SQLException {

        // Show the user all available categories first.
        try (Connection connection = Database.connect()) {

            if (!listBookingCategories(connection)) {
                return null;
            }
        }

        System.out.print(
                "Copy and paste Category ID: "
        );

        String input = scanner.nextLine().trim();

        if (input.isEmpty()) {
            throw new IllegalArgumentException(
                    "Category ID is required."
            );
        }

        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Invalid category UUID."
            );
        }
    }

    private static boolean listBookingCategories(
            Connection connection
    ) throws SQLException {

        String sql = """
                SELECT id, name
                FROM booking_category
                ORDER BY name
                """;

        try (PreparedStatement statement =
                    connection.prepareStatement(sql);
            ResultSet result = statement.executeQuery()) {

            boolean found = false;

            System.out.println();
            System.out.println(
                    "Available Booking Categories:"
            );
            System.out.println();

            while (result.next()) {

                found = true;

                System.out.println(
                        "Name: " +
                                result.getString("name")
                );

                System.out.println(
                        "ID:   " +
                                result.getString("id")
                );

                System.out.println();
            }

            if (!found) {
                System.out.println(
                        "No booking categories found."
                );
            }

            return found;
        }
    }

    // --------------------
    // EVENT RELATIONSHIPS
    // --------------------

    private static void printEventIds(
            Connection connection,
            UUID categoryId
    ) throws SQLException {

        String sql = """
                SELECT event_id
                FROM event_category
                WHERE category_id = ?
                """;

        try (PreparedStatement statement =
                    connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    categoryId.toString()
            );

            try (ResultSet result =
                        statement.executeQuery()) {

                System.out.println("Event IDs:");

                boolean found = false;

                while (result.next()) {

                    found = true;

                    System.out.println(
                            "  - " +
                                    result.getString("event_id")
                    );
                }

                if (!found) {
                    System.out.println("  None");
                }
            }
        }
    }

    // --------------------
    // VALIDATION
    // --------------------

    private static void validate(
            String name,
            String description
    ) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Category name cannot be empty."
            );
        }

        if (name.length() > 2000) {
            throw new IllegalArgumentException(
                    "Category name cannot exceed 2000 characters."
            );
        }

        if (description == null) {
            throw new IllegalArgumentException(
                    "Category description cannot be null."
            );
        }

        if (description.length() > 10000) {
            throw new IllegalArgumentException(
                    "Category description cannot exceed 10000 characters."
            );
        }
    }

    // --------------------
    // DATABASE HELPERS
    // --------------------

    private static boolean exists(
            UUID categoryId
    ) throws SQLException {

        String sql = """
                SELECT 1
                FROM booking_category
                WHERE id = ?
                """;

        try (Connection connection = Database.connect();
            PreparedStatement statement =
                    connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    categoryId.toString()
            );

            try (ResultSet result =
                        statement.executeQuery()) {

                return result.next();
            }
        }
    }
}