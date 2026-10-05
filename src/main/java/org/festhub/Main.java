package org.festhub;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.UUID;

public class Main {
    private static final int MAX_NAME_LENGTH = 2000;
    private static final int MAX_DESCRIPTION_LENGTH = 10000;

    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"; // regex expression to validate standard email format

    public static void main(String[] args) {
        Database.initialize();
        System.out.println("Welcome to FestHub! Type 'help' or 'exit'.");
        printHelp();

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("<FestHub> ");
                String input = scanner.nextLine().trim();

                if (input.isEmpty())
                    continue;
                if (input.equalsIgnoreCase("exit"))
                    break;
                if (input.equalsIgnoreCase("help")) {
                    printHelp();
                    continue;
                }

                String[] t = input.split("\\s+");
                if (t.length < 1) {
                    System.out.println("Expected: [type] [action]");
                    continue;
                }

                try {
                    switch (t[0]) {
                        case "1" -> runConcertEvent(t[1], scanner);
                        case "2" -> runArtist(t[1], scanner);
                        case "3" -> runBookingCategory(t[1], scanner);
                        case "4" -> runPromoMedia(t[1], scanner);
                        default -> System.out.println("Unknown record type. Allowed: 1, 2, 3, 4");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Error: expected a number (" + e.getMessage() + ")");
                }
                // catch (SQLException e) {
                // System.out.println("Database error: " + e.getMessage());
                // }
            }
        }
    }

    private static void printHelp() {
        System.out.println(
                "Types: 1=Concert Events, 2=Artists, " +
                        "3=Booking Categories, 4=Promo Media");

        System.out.println(
                "Actions: 1=Create, 2=Read, " +
                        "3=Update, 4=Delete");

        System.out.println(
                "Booking Categories: 5=Manage Events");
    }

    // ---------------------------------------------------------
    // CRUD: Concert Events
    // ---------------------------------------------------------

    // ---------------------------------------------------------
    // CRUD: Concert Events
    // ---------------------------------------------------------

    static void runConcertEvent(String action, Scanner scanner) {
        try {
            switch (action) {
                case "1" -> createConcertEvent(scanner);
                case "2" -> readConcertEvent(scanner);
                case "3" -> updateConcertEvent(scanner);
                case "4" -> deleteConcertEvent(scanner);
                case "5" -> {
                    // manageArtists(scanner);
                }
                case "6" -> manageEventCategories(scanner);
                default -> System.out.println("Unknown action. Allowed: 1, 2, 3, 4, 5, 6");
            }
        } catch (SQLException | IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void createConcertEvent(Scanner scanner) throws SQLException {
        String id = inputId(scanner);
        String name = inputText(scanner, "Concert Event Name", MAX_NAME_LENGTH, false);
        String description = inputText(scanner, "Concert Event Description", MAX_DESCRIPTION_LENGTH, false);
        int availableTickets = inputAvailableTickets(scanner, false);
        long priceCents = inputPriceCents(scanner, false);

        validateSoldOut(availableTickets, priceCents);

        String sql = """
                INSERT INTO concert_event
                (id, name, description, available_tickets, ticket_price_cents)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection = Database.connect();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, id);
            statement.setString(2, name);
            statement.setString(3, description);
            statement.setInt(4, availableTickets);
            statement.setLong(5, priceCents);
            statement.executeUpdate();
        }

        System.out.println("Concert Event created: " + id);
    }

    private static void readConcertEvent(Scanner scanner) throws SQLException {
        UUID id = chooseEvent(scanner);
        if (id == null) {
            return;
        }

        String sql = """
                SELECT id, name, description, available_tickets, ticket_price_cents
                FROM concert_event
                WHERE id = ?
                """;

        try (Connection connection = Database.connect();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, id.toString());

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalArgumentException("Concert Event not found.");
                }

                System.out.println("Concert Event Details:");
                System.out.println("ID: " + result.getString("id"));
                System.out.println("Name: " + result.getString("name"));
                System.out.println("Description: " + result.getString("description"));
                System.out.println("Available Tickets: " + result.getInt("available_tickets"));

                long priceCents = result.getLong("ticket_price_cents");
                BigDecimal priceDollars = BigDecimal.valueOf(priceCents).movePointLeft(2).setScale(2,
                        RoundingMode.UNNECESSARY);

                System.out.println("Ticket Price: $" + priceDollars);
            }

            readAssociatedIds(connection, id.toString(), "event_artist", "artist_id");
            readAssociatedIds(connection, id.toString(), "event_category", "category_id");
            readAssociatedIds(connection, id.toString(), "promo_media", "id");
        }
    }

    private static void updateConcertEvent(Scanner scanner) throws SQLException {
        UUID id = chooseEvent(scanner);
        if (id == null) {
            return;
        }

        System.out.println("Press Enter to keep the current value.");

        String name = inputText(scanner, "Concert Event Name", MAX_NAME_LENGTH, true);
        String description = inputText(scanner, "Concert Event Description", MAX_DESCRIPTION_LENGTH, true);
        Integer availableTickets = inputAvailableTickets(scanner, true);
        Long priceCents = inputPriceCents(scanner, true);

        try (Connection connection = Database.connect()) {
            connection.setAutoCommit(false);

            try {
                String currentSql = """
                        SELECT name, description, available_tickets, ticket_price_cents
                        FROM concert_event
                        WHERE id = ?
                        """;

                String currentName;
                String currentDescription;
                int currentTickets;
                long currentPrice;

                try (PreparedStatement statement = connection.prepareStatement(currentSql)) {
                    statement.setString(1, id.toString());

                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new IllegalArgumentException("Concert Event not found.");
                        }

                        currentName = result.getString("name");
                        currentDescription = result.getString("description");
                        currentTickets = result.getInt("available_tickets");
                        currentPrice = result.getLong("ticket_price_cents");
                    }
                }

                String finalName = name != null ? name : currentName;
                String finalDescription = description != null ? description : currentDescription;
                int finalTickets = availableTickets != null ? availableTickets : currentTickets;
                long finalPrice = priceCents != null ? priceCents : currentPrice;

                validateSoldOut(finalTickets, finalPrice);

                String updateSql = """
                        UPDATE concert_event
                        SET name = ?, description = ?, available_tickets = ?, ticket_price_cents = ?
                        WHERE id = ?
                        """;

                try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
                    statement.setString(1, finalName);
                    statement.setString(2, finalDescription);
                    statement.setInt(3, finalTickets);
                    statement.setLong(4, finalPrice);
                    statement.setString(5, id.toString());

                    int updated = statement.executeUpdate();

                    if (updated == 0) {
                        throw new IllegalArgumentException("Concert Event not found.");
                    }
                }

                connection.commit();

            } catch (SQLException | IllegalArgumentException e) {
                connection.rollback();
                throw e;
            }
        }

        System.out.println("Concert Event updated.");
    }

    private static void deleteConcertEvent(Scanner scanner) throws SQLException {
        UUID id = chooseEvent(scanner);
        if (id == null) {
            return;
        }

        try (Connection connection = Database.connect()) {
            connection.setAutoCommit(false);

            try {
                String sql = "DELETE FROM concert_event WHERE id = ?";
                int deleted;

                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, id.toString());
                    deleted = statement.executeUpdate();
                }

                if (deleted == 0) {
                    throw new IllegalArgumentException("Concert Event not found.");
                }

                deleteOrphanedBookingCategories(connection);
                connection.commit();

            } catch (SQLException | IllegalArgumentException e) {
                connection.rollback();
                throw e;
            }
        }

        System.out.println("Concert Event deleted.");
    }

    // ---------------------------------------------------------
    // CRUD: Artists
    // ---------------------------------------------------------

    static void runArtist(String action, Scanner scanner) {
        try {
            switch (action) {
                case "1" -> createArtist(scanner);
                case "2" -> readArtist(scanner);
                case "3" -> updateArtist(scanner);
                case "4" -> deleteArtist(scanner);
                case "5" -> manageEvents(scanner);
                default -> System.out.println("Allowed actions: 1, 2, 3, 4, 5");
            }
        } catch (SQLException | IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // Create an Artist instance/object
    public static void createArtist(Scanner scanner) throws SQLException{
        System.out.print("Artist Name: ");
        String name = scanner.nextLine().trim();

        System.out.print("Booking Contact (Email): ");
        String contact = scanner.nextLine().trim();

        validate (name, contact);

        UUID id = UUID.randomUUID();

        Database.execute(
            "INSERT INTO artist (id, name, booking_contact) VALUES (?, ?, ?)",
            id.toString(), name, contact);
        
            System.out.println("Artist created: " + id);
    }

    // Read out an Artist object
    public static void readArtist(Scanner scanner) throws SQLException {
        
        UUID id = chooseArtist(scanner);
        if (id == null) return;

        String sql = """
                SELECT name, booking_contact
                FROM artist
                WHERE id = ?
                """;

        try (Connection connection = Database.connect();
            PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, id.toString());

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    System.out.println("Artist not found.");
                    return;
                }

                System.out.println("Artist Name: " + result.getString("name"));
                System.out.println("Booking Contact: " + result.getString("booking_contact"));
            }
        }
    }

    // Update an Artist's database entry
    public static void updateArtist(Scanner scanner) throws SQLException {
        UUID id = chooseArtist(scanner);
        if (id == null) return;

        System.out.print("New Artist Name: ");
        String name = scanner.nextLine().trim();

        System.out.print("New Booking Contact (Email): ");
        String contact = scanner.nextLine().trim();

        validate(name, contact);

        int updated = Database.execute(
                """
                UPDATE artist
                SET name = ?, booking_contact = ?
                WHERE id = ?
                """,
                name, contact, id.toString());

        if (updated == 0) {
            System.out.println("Artist not found.");
            return;
        }

        System.out.println("Artist updated.");
    }

    // Delete an Artist's database entry
    public static void deleteArtist(Scanner scanner) throws SQLException {
        UUID id = chooseArtist(scanner);
        if (id == null) return;

        int deleted = Database.execute(
                "DELETE FROM artist WHERE id = ?",
                id.toString());

        if (deleted == 0) {
            System.out.println("Artist not found.");
            return;
        }

        System.out.println("Artist deleted.");
    }

    // ---------------------------------------------------------
    // CRUD: Booking Categories
    // ---------------------------------------------------------

    private static void runBookingCategory(String action, Scanner scanner) {
        try {
            switch (action) {
                case "1" -> createBookingCategory(scanner);
                case "2" -> readBookingCategory(scanner);
                case "3" -> updateBookingCategory(scanner);
                case "4" -> deleteBookingCategory(scanner);
                case "5" -> manageEventCategories(scanner);
                default -> System.out.println("Allowed actions: 1, 2, 3, 4, 5");
            }
        } catch (SQLException | IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void createBookingCategory(Scanner scanner) throws SQLException {
        System.out.print("Category name: ");
        String name = scanner.nextLine().trim();

        System.out.print("Description: ");
        String description = scanner.nextLine().trim();

        validateBookingCategory(name, description);

        UUID id = UUID.randomUUID();

        Database.execute(
                "INSERT INTO booking_category(id, name, description) VALUES (?, ?, ?)",
                id.toString(), name, description);

        System.out.println("Category created: " + id);
    }

    private static void readBookingCategory(Scanner scanner) throws SQLException {
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

    private static void updateBookingCategory(Scanner scanner) throws SQLException {
        UUID id = chooseCategory(scanner);
        if (id == null)
            return;

        System.out.print("New name: ");
        String name = scanner.nextLine().trim();

        System.out.print("New description: ");
        String description = scanner.nextLine().trim();

        validateBookingCategory(name, description);

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

    private static void deleteBookingCategory(Scanner scanner) throws SQLException {
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

    private static void manageEventCategories(Scanner scanner) throws SQLException {

        UUID eventId = chooseEvent(scanner);

        if (eventId == null) {
            return;
        }

        UUID categoryId = chooseCategory(scanner);

        if (categoryId == null) {
            return;
        }

        System.out.println();
        System.out.println("1 = Add Booking Category to Event");
        System.out.println("2 = Remove Booking Category from Event");
        System.out.println("3 = Cancel");
        System.out.print("Choose: ");

        switch (scanner.nextLine().trim()) {

            case "1" -> addEventCategory(eventId, categoryId);
            case "2" -> removeEventCategory(eventId, categoryId);
            case "3" -> {
            }
            default -> System.out.println("Invalid option.");
        }
    }

    private static void addEventCategory(UUID eventId, UUID categoryId) throws SQLException {
        Database.execute(
                """
                        INSERT INTO event_category(event_id, category_id)
                        VALUES (?, ?)
                        """,
                eventId.toString(),
                categoryId.toString());

        System.out.println("Booking Category added to Event.");
    }

    private static void removeEventCategory(UUID eventId, UUID categoryId) throws SQLException {
        int removed = Database.execute(
                """
                        DELETE FROM event_category
                        WHERE event_id = ? AND category_id = ?
                        """,
                eventId.toString(),
                categoryId.toString());

        if (removed == 0) {
            System.out.println("That Booking Category is not linked to this Event.");
            return;
        }

        // If the user removed the category's final event,
        // the category is now an orphan and is deleted.
        try (Connection connection = Database.connect()) {
            deleteOrphanedBookingCategories(connection);
        }

        System.out.println("Booking Category removed from Event.");
    }

    // Manage Events (add/delete artist-event relationships)
    public static void manageEvents(Scanner scanner) throws SQLException {
        UUID artistId = chooseArtist(scanner);
        if (artistId == null) return;

        System.out.println();
        System.out.println("1 = Add Artist to Event");
        System.out.println("2 = Remove Artist from Event");
        System.out.println("3 = Cancel");
        System.out.print("Choose: ");

        switch (scanner.nextLine().trim()) {
            case "1" -> addEvent(scanner, artistId);
            case "2" -> removeEvent(scanner, artistId);
            case "3" -> {}
            default -> System.out.println("Invalid option.");
        }
    }

    // Separate methods for adding and removing event-artist relationships
        private static void addEvent(Scanner scanner, UUID artistId) throws SQLException {
            System.out.println();
            System.out.println("Available Events:");

            if (!Database.list("concert_event")) {
                System.out.println("No concert events found.");
                return;
            }

            System.out.print("Event ID: ");
            String eventId = scanner.nextLine().trim();

            Database.execute(
                    "INSERT INTO event_artist(artist_id, event_id) VALUES (?, ?)",
                    artistId.toString(), eventId);

            System.out.println("Artist linked to event.");
        }

        private static void removeEvent(Scanner scanner, UUID artistId) throws SQLException {
            System.out.print("Event ID to remove from this artist: ");
            String eventId = scanner.nextLine().trim();

            int removed = Database.execute(
                    """
                    DELETE FROM event_artist
                    WHERE artist_id = ? AND event_id = ?
                    """,
                    artistId.toString(), eventId);

            if (removed == 0) {
                System.out.println("That artist is not linked to this event.");
                return;
            }

            System.out.println("Artist removed from event.");
        }

    // ---------------------------------------------------------
    // CRUD: Promotional Media
    // ---------------------------------------------------------
    private static void runPromoMedia(String action, Scanner scanner) {
        try {
            switch (action) {
                case "1" -> createPromoMedia(scanner);
                case "2" -> readPromoMedia(scanner);
                case "3" -> updatePromoMedia(scanner);
                case "4" -> deletePromoMedia(scanner);
                default -> System.out.println("Allowed actions: 1, 2, 3, 4");
            }
        } catch (SQLException | IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void createPromoMedia(Scanner scanner) throws SQLException {
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

        validatePromoMedia(eventId, imageUrl);

        UUID id = UUID.randomUUID();

        Database.execute(
                "INSERT INTO promo_media(id, event_id, image_url) VALUES (?, ?, ?)",
                id.toString(), eventId, imageUrl);

        System.out.println("Promo media created: " + id);
    }

    private static void readPromoMedia(Scanner scanner) throws SQLException {
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

    private static void updatePromoMedia(Scanner scanner) throws SQLException {
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

        validatePromoMedia(eventId, imageUrl);

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

    private static void deletePromoMedia(Scanner scanner) throws SQLException {
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

    // ---------------------------------------------------------
    // ARTIST HELPERS
    // ---------------------------------------------------------

    // checks name's character length AND validates the inputted email
    private static void validate(String name, String contact){
        if (name == null || name.length() > 2000) {
            throw new IllegalArgumentException("Artist Name cannot exceed 2000 characters.");
        }
        if (contact == null || !contact.matches(EMAIL_REGEX)){
            throw new IllegalArgumentException("Booking contact must be a valid email address.");
        }
    }

    // helper for Artist selection via UUID
    private static UUID chooseArtist(Scanner scanner) throws SQLException {
        System.out.println("\nArtists:");

        if (!Database.list("artist")) {
            System.out.println("No artists found.");
            return null;
        }

        System.out.print("Artist ID: ");
        String input = scanner.nextLine().trim();

        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid artist UUID.");
        }
    }

    // ---------------------------------------------------------
    // CONCERT EVENT HELPERS
    // ---------------------------------------------------------

    private static String inputId(Scanner scanner) throws SQLException {
        while (true) {
            System.out.print("Enter Concert Event ID (UUID). Leave empty to auto generate: ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                String generatedId = UUID.randomUUID().toString();
                System.out.println("Generated ID: " + generatedId);
                return generatedId;
            }

            String validatedId = validateUuid(input);

            if (validatedId == null) {
                System.out.println("Invalid UUID format. Please enter a valid UUID.");
                continue;
            }

            if (checkIdExists(validatedId, "concert_event")) {
                System.out.println("That Concert Event ID is already in use.");
                continue;
            }

            return validatedId;
        }
    }

    private static String validateUuid(String input) {
        try {
            return UUID.fromString(input).toString();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String inputText(Scanner scanner, String fieldName, int maxLength, boolean allowEmpty) {
        while (true) {
            System.out.print("Enter " + fieldName + ": ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                if (allowEmpty) {
                    return null;
                }

                System.out.println(fieldName + " cannot be empty.");
                continue;
            }

            if (input.length() > maxLength) {
                System.out.println(fieldName + " cannot exceed " + maxLength + " characters.");
                continue;
            }

            return input;
        }
    }

    private static Integer inputAvailableTickets(Scanner scanner, boolean allowEmpty) {
        while (true) {
            System.out.print("Enter Available Tickets: ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                if (allowEmpty) {
                    return null;
                }

                System.out.println("Available Tickets cannot be empty.");
                continue;
            }

            try {
                int availableTickets = Integer.parseInt(input);

                if (availableTickets < 0) {
                    System.out.println("Available Tickets cannot be negative.");
                    continue;
                }

                return availableTickets;

            } catch (NumberFormatException e) {
                System.out.println("Invalid number format. Please enter a valid integer.");
            }
        }
    }

    private static Long inputPriceCents(Scanner scanner, boolean allowEmpty) {
        while (true) {
            System.out.print("Enter Price (e.g., 19.99): ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                if (allowEmpty) {
                    return null;
                }

                System.out.println("Price cannot be empty.");
                continue;
            }

            try {
                BigDecimal price = new BigDecimal(input);

                if (price.compareTo(BigDecimal.ZERO) <= 0) {
                    System.out.println("Price must be greater than zero.");
                    continue;
                }

                price = price.setScale(2, RoundingMode.UNNECESSARY);
                return price.movePointRight(2).longValueExact();

            } catch (NumberFormatException | ArithmeticException e) {
                System.out.println(
                        "Invalid number format. Please enter a valid decimal number with at most two decimal places.");
            }
        }
    }

    private static boolean checkIdExists(String id, String tableName) throws SQLException {
        String sql = "SELECT COUNT(1) FROM " + tableName + " WHERE id = ?";

        try (Connection connection = Database.connect();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, id);

            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getInt(1) > 0;
            }
        }
    }

    private static void readAssociatedIds(Connection connection, String eventId, String tableName, String columnName)
            throws SQLException {

        String sql = "SELECT " + columnName + " FROM " + tableName + " WHERE event_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, eventId);

            try (ResultSet result = statement.executeQuery()) {
                System.out.println("Associated IDs from " + tableName + ":");

                boolean found = false;

                while (result.next()) {
                    found = true;
                    System.out.println(result.getString(columnName));
                }

                if (!found) {
                    System.out.println("None");
                }
            }
        }
    }

    private static void deleteOrphanedBookingCategories(Connection connection) throws SQLException {
        String sql = """
                DELETE FROM booking_category
                WHERE id NOT IN (
                    SELECT category_id
                    FROM event_category
                )
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int rowsAffected = statement.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Deleted " + rowsAffected + " orphaned booking categories.");
            }
        }
    }

    private static void validateSoldOut(int availableTickets, long priceCents) {
        if (availableTickets == 0 && priceCents > 10000) {
            throw new IllegalArgumentException("A sold-out event cannot have a ticket price over $100.");
        }
    }

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

    private static UUID chooseEvent(Scanner scanner) throws SQLException {
        System.out.println();
        System.out.println("Concert Events:");

        if (!Database.list("concert_event")) {
            System.out.println("No concert events found.");
            return null;
        }

        System.out.print("Event ID: ");
        String input = scanner.nextLine().trim();

        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Event UUID.");
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

    private static void validateBookingCategory(String name, String description) {
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

    private static void validatePromoMedia(String eventId, String imageUrl) {
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