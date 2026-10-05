package org.festhub;
import java.util.Scanner;
import java.util.UUID;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


class Artist {

    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"; // regex expression to validate standard email format


    static void run(String action, Scanner scanner) {
        try{
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

    //*_______________ ARTIST HELPERS ___________________*//

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

    //*________________ C-R-U-D OPERATIONS __________________*//
    
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
}



// -----------------------------------------------------------------------------//

