package org.festhub;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {
        private static final String DATABASE_URL = "jdbc:sqlite:data/festhub.db";

        private Database() {
        }

        static Connection connect() throws SQLException {
                // Makes a new data directory if it doesn't exist
                new File("data").mkdirs();

                // Connect to SQLite
                Connection connection = DriverManager.getConnection(DATABASE_URL);

                try (Statement statement = connection.createStatement()) {
                        statement.execute("PRAGMA foreign_keys = ON");
                }

                return connection;
        }

        // ---------------------------------------------------------
        // SHARED DATABASE METHODS
        // ---------------------------------------------------------

        /*
         * Used for INSERT, UPDATE, and DELETE statements.
         *
         * Example:
         *
         * Database.execute(
         * "DELETE FROM booking_category WHERE id = ?",
         * categoryId.toString()
         * );
         */
        static int execute(
                        String sql,
                        String... values) throws SQLException {

                try (Connection connection = connect();
                                PreparedStatement statement = connection.prepareStatement(sql)) {

                        for (int i = 0; i < values.length; i++) {
                                statement.setString(
                                                i + 1,
                                                values[i]);
                        }

                        return statement.executeUpdate();
                }
        }

        /*
         * Lists records containing an id and name.
         *
         * Examples:
         *
         * Database.list("concert_event");
         * Database.list("artist");
         * Database.list("booking_category");
         *
         * Only use hard-coded table names here.
         */
        static boolean list(
                        String table) throws SQLException {

                String sql = "SELECT id, name FROM "
                                + table
                                + " ORDER BY name";

                try (Connection connection = connect();
                                PreparedStatement statement = connection.prepareStatement(sql);
                                ResultSet result = statement.executeQuery()) {

                        boolean found = false;

                        while (result.next()) {
                                found = true;

                                System.out.println(
                                                result.getString("name")
                                                                + " - "
                                                                + result.getString("id"));
                        }

                        return found;
                }
        }

        // ---------------------------------------------------------
        // INITIALIZATION
        // ---------------------------------------------------------

        public static void initialize() {
                try (Connection connection = connect()) {
                        createConcertEventTable(connection);
                        createArtistTable(connection);
                        createCategoryTable(connection);
                        createMediaTable(connection);
                        createEventArtistTable(connection);
                        createEventCategoryTable(connection);

                        System.out.println(
                                        "Connected to FestHub database.");

                } catch (SQLException exception) {
                        throw new RuntimeException(
                                        "Failed to initialize FestHub database.",
                                        exception);
                }
        }

        // ---------------------------------------------------------
        // CONCERT EVENT TABLE
        // ---------------------------------------------------------

        private static void createConcertEventTable(
                        Connection connection) throws SQLException {

                try (Statement statement = connection.createStatement()) {

                        statement.execute(
                                        "CREATE TABLE IF NOT EXISTS concert_event (" +
                                                        "id TEXT PRIMARY KEY," +
                                                        "name TEXT NOT NULL CHECK (length(name) <= 2000)," +
                                                        "description TEXT NOT NULL CHECK (length(description) <= 10000),"
                                                        +
                                                        "available_tickets INTEGER NOT NULL CHECK (available_tickets >= 0),"
                                                        +

                                                        // Store ticket price in cents to avoid
                                                        // floating point precision issues
                                                        "ticket_price_cents INTEGER NOT NULL CHECK (ticket_price_cents > 0),"
                                                        +

                                                        // Sold-out events cannot cost more than $100
                                                        "CHECK (available_tickets > 0 OR ticket_price_cents <= 10000)" +
                                                        ")");
                }
        }

        // ---------------------------------------------------------
        // ARTIST TABLE
        // ---------------------------------------------------------

        private static void createArtistTable(
                        Connection connection) throws SQLException {

                try (Statement statement = connection.createStatement()) {

                        statement.execute(
                                        "CREATE TABLE IF NOT EXISTS artist (" +
                                                        "id TEXT PRIMARY KEY," +
                                                        "name TEXT NOT NULL CHECK (length(name) <= 2000)," +

                                                        // Enforce email format in Java code
                                                        "booking_contact TEXT NOT NULL" +
                                                        ")");
                }
        }

        // ---------------------------------------------------------
        // BOOKING CATEGORY TABLE
        // ---------------------------------------------------------

        private static void createCategoryTable(
                        Connection connection) throws SQLException {

                try (Statement statement = connection.createStatement()) {

                        statement.execute(
                                        "CREATE TABLE IF NOT EXISTS booking_category (" +
                                                        "id TEXT PRIMARY KEY," +
                                                        "name TEXT NOT NULL CHECK (length(name) <= 2000)," +
                                                        "description TEXT NOT NULL CHECK (length(description) <= 10000)"
                                                        +
                                                        ")");
                }
        }

        // ---------------------------------------------------------
        // PROMOTIONAL MEDIA TABLE
        // ---------------------------------------------------------

        private static void createMediaTable(
                        Connection connection) throws SQLException {

                try (Statement statement = connection.createStatement()) {

                        statement.execute(
                                        "CREATE TABLE IF NOT EXISTS promo_media (" +
                                                        "id TEXT PRIMARY KEY," +
                                                        "event_id TEXT NOT NULL," +

                                                        // Enforce URL format in Java code
                                                        "image_url TEXT NOT NULL," +

                                                        "FOREIGN KEY (event_id) " +
                                                        "REFERENCES concert_event(id) " +
                                                        "ON DELETE CASCADE" +
                                                        ")");
                }
        }

        // ---------------------------------------------------------
        // EVENT <-> ARTIST JOIN TABLE
        // ---------------------------------------------------------

        private static void createEventArtistTable(
                        Connection connection) throws SQLException {

                try (Statement statement = connection.createStatement()) {

                        statement.execute(
                                        "CREATE TABLE IF NOT EXISTS event_artist (" +
                                                        "event_id TEXT NOT NULL," +
                                                        "artist_id TEXT NOT NULL," +
                                                        "PRIMARY KEY (event_id, artist_id)," +

                                                        "FOREIGN KEY (event_id) " +
                                                        "REFERENCES concert_event(id) " +
                                                        "ON DELETE CASCADE," +

                                                        "FOREIGN KEY (artist_id) " +
                                                        "REFERENCES artist(id) " +
                                                        "ON DELETE CASCADE" +
                                                        ")");
                }
        }

        // ---------------------------------------------------------
        // EVENT <-> CATEGORY JOIN TABLE
        // ---------------------------------------------------------

        private static void createEventCategoryTable(
                        Connection connection) throws SQLException {

                try (Statement statement = connection.createStatement()) {

                        statement.execute(
                                        "CREATE TABLE IF NOT EXISTS event_category (" +
                                                        "event_id TEXT NOT NULL," +
                                                        "category_id TEXT NOT NULL," +
                                                        "PRIMARY KEY (event_id, category_id)," +

                                                        "FOREIGN KEY (event_id) " +
                                                        "REFERENCES concert_event(id) " +
                                                        "ON DELETE CASCADE," +

                                                        "FOREIGN KEY (category_id) " +
                                                        "REFERENCES booking_category(id) " +
                                                        "ON DELETE CASCADE" +
                                                        ")");
                }
        }
}