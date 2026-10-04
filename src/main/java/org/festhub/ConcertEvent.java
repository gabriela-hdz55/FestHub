package org.festhub;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

class ConcertEvent {
    private static final int MAX_NAME_LENGTH = 2000;
    private static final int MAX_DESCRIPTION_LENGTH = 10000;

    // HELPER METHODS FOR CRUD OPERATIONS -------------------------------------------------------------------------------------------------

    // input method for UUID with validation and auto-generation
    private static String inputId(Scanner scanner) {
        while (true) {
            System.out.print("Enter Concert Event ID (UUID). Leave empty to auto generate: ");
            String input = scanner.nextLine().trim();

            // if user input is empty, generate a new UUID
            if (input.isEmpty()) {
                String generatedId = UUID.randomUUID().toString();
                System.out.println("Generated ID: " + generatedId);
                return generatedId;
            }

            // validate UUID format
            String validatedId = validateUuid(input);
            if (validatedId == null) {
                System.out.println("Invalid UUID format. Please enter a valid UUID.");
                continue; // ask for input again
            }

            return validatedId;
        }
    }

    // general parse uuid method for validation (used in inputIds and inputId)
    private static String validateUuid(String input){
        // check if user input is a valid UUID
        try {
            return UUID.fromString(input).toString();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // input methods for text fields with validation
    private static String inputText(Scanner scanner, String fieldName, int maxLength, boolean allowEmpty) {
        while (true) {
            System.out.print("Enter " + fieldName + ": ");
            String input = scanner.nextLine().trim();

            // verify field is not empty
            if (input.isEmpty()) {
                if (allowEmpty) {
                    return null;
                } else {
                    System.out.println(fieldName + " cannot be empty.");
                    continue; // ask for input again
                }
            }

            // verify field does not exceed max length
            if (input.length() > maxLength) {
                System.out.println(fieldName + " cannot exceed " + maxLength + " characters.");
                continue; // ask for input again
            }

            return input;
        }
    }

    // input method for available tickets with validation
    private static Integer inputAvailableTickets(Scanner scanner, boolean allowEmpty) {
        while (true) {
            System.out.print("Enter Available Tickets: ");
            String input = scanner.nextLine().trim();

            // verify field is not empty
            if (input.isEmpty()) {
                if (allowEmpty) {
                    return null;
                } else {
                    System.out.println("Available Tickets cannot be empty.");
                    continue; // ask for input again
                }
            }

            // verify field is a valid integer
            try {
                int availableTickets = Integer.parseInt(input);
                // verify field is not negative
                if (availableTickets < 0) {
                    System.out.println("Available Tickets cannot be negative.");
                    continue; // ask for input again
                }
                return availableTickets;
            } catch (NumberFormatException e) {
                System.out.println("Invalid number format. Please enter a valid integer.");
            }
        }
    }

    // input method for price with validation
    private static Long inputPriceCents(Scanner scanner, boolean allowEmpty) {
        while (true) {
            System.out.print("Enter Price (e.g., 19.99): ");
            String input = scanner.nextLine().trim();

            // verify field is not empty
            if (input.isEmpty()) {
                if (allowEmpty) {
                    return null;
                } else {
                    System.out.println("Price cannot be empty.");
                    continue; // ask for input again
                }
            }

            // verify field is a valid decimal number
            try {
                BigDecimal price = new BigDecimal(input);
                // verify field is not negative
                if (price.compareTo(BigDecimal.ZERO) <= 0) {
                    System.out.println("Price cannot be negative or zero.");
                    continue; // ask for input again
                }
                
                // require at most two decimal places
                price = price.setScale(2, RoundingMode.UNNECESSARY);

                // convert to cents and return as long
                return price.movePointRight(2).longValueExact();

            } catch (NumberFormatException | ArithmeticException e) {
                System.out.println("Invalid number format. Please enter a valid decimal number.");
            }
        }
    }

    // input method for IDs with validation
    // private static List<String> inputIds(Scanner scanner, String fieldName, String tableName, boolean update) {
    //     System.out.println("Enter " + fieldName + " (comma-separated UUIDs). Press enter to skip: ");

    //     String input = scanner.nextLine().trim();

    //     // if empty return an empty list or null based on whether this is an update operation
    //     if (input.isEmpty()) {
    //         if (update) {
    //             return null; // null indicates no change for update operation
    //         } else {
    //             return new java.util.ArrayList<>(); // empty list indicates no IDs for create operation
    //         }
    //     }

    //     // split input by commas
    //     String[] idsArray = input.split(",");
    //     List<String> idsList = new java.util.ArrayList<>();

    //     for (String id : idsArray) {
    //         String trimmedId = id.trim();

    //         // check if UUID is valid format
    //         String validatedUuid = validateUuid(trimmedId);

    //         if (validatedUuid == null) {
    //             System.out.println("Skipping invalid UUID: " + trimmedId);
    //             continue;
    //         }

    //         // check if UUID exists in database
    //         if (!checkIdExists(validatedUuid, tableName)) {
    //             System.out.println("Skipping UUID that does not exist: " + validatedUuid);
    //             continue;
    //         }

    //         idsList.add(validatedUuid);
    //     }

    //     return idsList;
    // }

    // check if a given ID exists in the database for a specific table
    private static boolean checkIdExists(String id, String tableName) {
        String sql = "SELECT COUNT(1) FROM " + tableName + " WHERE id = ?";

        try (var connection = Database.getConnection();
             var preparedStatement = connection.prepareStatement(sql)) {
            // set the ID parameter in the prepared statement
            preparedStatement.setString(1, id);
            var resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getInt(1) > 0;
            }
        } catch (Exception e) {
            System.out.println("Database error while checking ID existence: " + e.getMessage());
        }
        return false;
    }

    private static void readAssociatedIds(Connection connection, String eventId, String tableName, String columnName) {
        String sql = "SELECT " + columnName + " FROM " + tableName + " WHERE event_id = ?";
        try (var preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, eventId);
            var resultSet = preparedStatement.executeQuery();

            System.out.println("Associated IDs from " + tableName + ":");
            while (resultSet.next()) {
                System.out.println(resultSet.getString(columnName));
            }
        } catch (Exception e) {
            System.out.println("Database error while reading associated IDs from " + tableName + ": " + e.getMessage());
        }
    }

    private static void deleteOrphanedBookingCategories(Connection connection) {
        String sql = "DELETE FROM booking_category WHERE id NOT IN (SELECT category_id FROM event_category)";
        try (var preparedStatement = connection.prepareStatement(sql)) {
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Deleted " + rowsAffected + " orphaned booking categories.");
            }
        } catch (Exception e) {
            System.out.println("Database error while deleting orphaned booking categories: " + e.getMessage());
        }
    }

    // CRUD OPERATIONS ----------------------------------------------------------------------------------------------

    private static void createConcertEvent(Scanner scanner) {
        String id = inputId(scanner);
        String name = inputText(scanner, "Concert Event Name", MAX_NAME_LENGTH, false);
        String description = inputText(scanner, "Concert Event Description", MAX_DESCRIPTION_LENGTH, false);
        Integer availableTickets = inputAvailableTickets(scanner, false);
        long priceCents = inputPriceCents(scanner, false);
        // List<String> artistIds = inputIds(scanner, "Artist IDs", "artist", false);
        // List<String> bookingCategoryIds = inputIds(scanner, "Booking Category IDs", "booking_category", false);
        // TODO: change event_id in promo_media to allow null? or leave as is 
        // if leave as is -> can't add promo media when creating event, must add after separately creating promo media, then link
        // to existing event
        // List<String> promoMediaIds = inputIds(scanner, "Promo Media IDs", "promo_media");

        // insert concert event into database
        String sql = "INSERT INTO concert_event (id, name, description, available_tickets, ticket_price_cents) VALUES (?, ?, ?, ?, ?)";
        try (var connection = Database.getConnection();
             var preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, id);
            preparedStatement.setString(2, name);
            preparedStatement.setString(3, description);
            preparedStatement.setInt(4, availableTickets);
            preparedStatement.setLong(5, priceCents);
            preparedStatement.executeUpdate();

            // // insert artist IDs into concert_event_artist table
            // for (String artistId : artistIds) {
            //     String artistSql = "INSERT INTO event_artist (event_id, artist_id) VALUES (?, ?)";
            //     try (var artistStatement = connection.prepareStatement(artistSql)) {
            //         artistStatement.setString(1, id);
            //         artistStatement.setString(2, artistId);
            //         artistStatement.executeUpdate();
            //     }
            // }

            // // insert booking category IDs into concert_event_booking_category table
            // for (String bookingCategoryId : bookingCategoryIds) {
            //     String bookingCategorySql = "INSERT INTO event_category (event_id, category_id) VALUES (?, ?)";
            //     try (var bookingCategoryStatement = connection.prepareStatement(bookingCategorySql)) {
            //         bookingCategoryStatement.setString(1, id);
            //         bookingCategoryStatement.setString(2, bookingCategoryId);
            //         bookingCategoryStatement.executeUpdate();
            //     }
            // }

            // // update promo media table to link to this concert event
            // // for (String promoMediaId : promoMediaIds) {
            // //     String promoMediaSql = "UPDATE promo_media SET event_id = ? WHERE id = ?";
            // //     try (var promoMediaStatement = connection.prepareStatement(promoMediaSql)) {
            // //         promoMediaStatement.setString(1, id);
            // //         promoMediaStatement.setString(2, promoMediaId);
            // //         promoMediaStatement.executeUpdate();
            // //     }
            // // }

            System.out.println("Concert Event created.");
        } catch (Exception e) {
            System.out.println("Database error while creating Concert Event: " + e.getMessage());
        }
    }

    private static void readConcertEvent(Scanner scanner) {
        while (true) {
            System.out.print("Enter Concert Event ID to read: ");
            String input = scanner.nextLine().trim();

            // if field is empty return to main menu
            if (input.isEmpty()) {
                System.out.println("No ID entered. Returning to main menu.");
                return;
            }

            // validate UUID format
            String validatedId = validateUuid(input);
            if (validatedId == null) {
                System.out.println("Invalid UUID format. Please enter a valid UUID.");
                continue; // ask for input again
            }

            // query the database for the concert event with the given ID
            String sql = "SELECT * FROM concert_event WHERE id = ?";
            try (var connection = Database.getConnection();
                 var preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setString(1, validatedId);
                var resultSet = preparedStatement.executeQuery();

                if (resultSet.next()) {
                    // display concert event details
                    System.out.println("Concert Event Details:");
                    System.out.println("ID: " + resultSet.getString("id"));
                    System.out.println("Name: " + resultSet.getString("name"));
                    System.out.println("Description: " + resultSet.getString("description"));
                    System.out.println("Available Tickets: " + resultSet.getInt("available_tickets"));
                    // convert ticket price from cents to dollars for display
                    Long priceCents = resultSet.getLong("ticket_price_cents");
                    BigDecimal priceDollars = BigDecimal.valueOf(priceCents).movePointLeft(2).setScale(2, RoundingMode.UNNECESSARY);
                    System.out.println("Ticket Price: $" + priceDollars);

                    // read associated artist IDs
                    readAssociatedIds(connection, validatedId, "event_artist", "artist_id");
                    // read associated booking category IDs
                    readAssociatedIds(connection, validatedId, "event_category", "category_id");
                    // read associated promo media IDs
                    readAssociatedIds(connection, validatedId, "promo_media", "id");
                } else {
                    System.out.println("No Concert Event found with ID: " + validatedId);
                }
                System.out.println("Finished reading Concert Event. Returning to main menu.");
            } catch (Exception e) {
                System.out.println("Database error while reading Concert Event: " + e.getMessage());
            }
            break;
        }
    }

    private static void updateConcertEvent(Scanner scanner) {
        while (true) {
            System.out.print("Enter Concert Event ID to update: ");
            String input = scanner.nextLine().trim();

            // if field is empty return to main menu
            if (input.isEmpty()) {
                System.out.println("No ID entered. Returning to main menu.");
                return;
            }

            // validate UUID format
            String validatedId = validateUuid(input);
            if (validatedId == null) {
                System.out.println("Invalid UUID format. Please enter a valid UUID.");
                continue;
            }

            // check if the concert event exists in the database
            if (!checkIdExists(validatedId, "concert_event")) {
                System.out.println("Concert Event with ID " + validatedId + " not found.");
                continue;
            }

            // prompt user for new values for each field
            System.out.println("Press Enter to keep the current value.");

            String name = inputText(scanner, "Concert Event Name", MAX_NAME_LENGTH, true);
            String description = inputText(scanner, "Concert Event Description", MAX_DESCRIPTION_LENGTH, true);
            Integer availableTickets = inputAvailableTickets(scanner, true);
            Long priceCents = inputPriceCents(scanner, true);
            String sql =
                    "UPDATE concert_event SET " +
                    "name = COALESCE(?, name), " +
                    "description = COALESCE(?, description), " +
                    "available_tickets = COALESCE(?, available_tickets), " +
                    "ticket_price_cents = COALESCE(?, ticket_price_cents) " +
                    "WHERE id = ?";

            try (var connection = Database.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    // update concert event fields
                    try (var preparedStatement = connection.prepareStatement(sql)) {
                        preparedStatement.setString(1, name);
                        preparedStatement.setString(2, description);
                        preparedStatement.setObject(3, availableTickets, java.sql.Types.INTEGER);
                        preparedStatement.setObject(4, priceCents, java.sql.Types.BIGINT);
                        preparedStatement.setString(5,validatedId);
                        preparedStatement.executeUpdate();
                    }

                    // everything succeeded
                    connection.commit();

                    System.out.println("Concert Event updated.");
                    return;

                } catch (Exception e) {
                    // undo all changes if any part of the update fails
                    connection.rollback();
                    throw e;
                }

            } catch (Exception e) {
                System.out.println("Database error while updating Concert Event: " + e.getMessage());
            }
        }
    }

    private static void deleteConcertEvent(Scanner scanner) {
        while (true) {
            System.out.print("Enter Concert Event ID to delete: ");
            String input = scanner.nextLine().trim();

            // if field is empty return to main menu
            if (input.isEmpty()) {
                System.out.println("No ID entered. Returning to main menu.");
                return;
            }

            // validate UUID format
            String validatedId = validateUuid(input);
            if (validatedId == null) {
                System.out.println("Invalid UUID format. Please enter a valid UUID.");
                continue; // ask for input again
            }

            // delete the concert event with the given ID from the database
            String sql = "DELETE FROM concert_event WHERE id = ?";
            try (var connection = Database.getConnection();
                 var preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setString(1, validatedId);
                int rowsAffected = preparedStatement.executeUpdate();

                if (rowsAffected > 0) {
                    System.out.println("Concert Event with ID " + validatedId + " deleted successfully.");
                } else {
                    System.out.println("No Concert Event found with ID: " + validatedId);
                }
                
                // delete orphaned records in booking_category (others are handled by foreign key constraints)
                deleteOrphanedBookingCategories(connection);

                System.out.println("Returning to main menu.");
            } catch (Exception e) {
                System.out.println("Database error while deleting Concert Event: " + e.getMessage());
            }
            break;
        }
    }

    // RUN CRUD OPERATIONS BASED ON USER INPUT ---------------------------------------------------
    static void run(String action, Scanner scanner) {
        switch (action) {
            case "1" -> {
                createConcertEvent(scanner);
            }
            case "2" -> {
                readConcertEvent(scanner);
            }
            case "3" -> {
                updateConcertEvent(scanner);
            }
            case "4" -> {
                deleteConcertEvent(scanner);
            }
            case "5" -> {
                // manageArtists(scanner);
            }
            case "6" -> {
                // manageBookingCategories(scanner);
            }
            default -> System.out.println("Unknown action. Allowed: 1, 2, 3, 4, 5");
        }
    }
}