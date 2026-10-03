package org.festhub;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {
    private static final String DATABASE_URL = "jdbc:sqlite:data/festhub.db";

    private Database(){}

    static Connection connect() throws SQLException {
        //Makes a new data directory if it doesn't exist
        new File("data").mkdirs();

        //connect to lite
        Connection connection =
                DriverManager.getConnection(DATABASE_URL);

        try (Statement statement = connection.createStatement()){
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    public static void initialize(){
        try (Connection connection = connect()){
            createConcertEventTable(connection);
            createArtistTable(connection);
            createCategoryTable(connection);
            createMediaTable(connection);
            createEventArtistTable(connection);
            createEventCategoryTable(connection);
            System.out.println("Connected to FestHub database.");
        } catch (SQLException exception){
            throw new RuntimeException(
                "Failed to initialize FestHub database.",
                exception
            );
        }
    }

    private static void createConcertEventTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()){
            statement.execute(
                "CREATE TABLE IF NOT EXISTS concert_event (" +
                    "id TEXT PRIMARY KEY," +
                    "name TEXT NOT NULL CHECK (length(name) <= 2000)," +
                    "description TEXT NOT NULL CHECK (length(description) <= 10000)," +
                    "available_tickets INTEGER NOT NULL CHECK (available_tickets >= 0)," +
                    // storing ticket price in cents to avoid floating point precision issues
                    "ticket_price_cents INTEGER NOT NULL CHECK (ticket_price_cents > 0)," +
                    // constraint to ensure that if there are no available tickets, the price must be less than or equal to $100.00 (10000 cents)
                    "CHECK (available_tickets > 0 OR ticket_price_cents <= 10000)" +
                ")"
            );
        }
    }

    private static void createArtistTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()){
            statement.execute(
                "CREATE TABLE IF NOT EXISTS artist (" +
                    "id TEXT PRIMARY KEY," +
                    "name TEXT NOT NULL CHECK (length(name) <= 2000)," +
                    "booking_contact TEXT NOT NULL" + // enforce email format in java code
                ")"
            );
        }
    }

    private static void createCategoryTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()){
            statement.execute(
                "CREATE TABLE IF NOT EXISTS booking_category (" +
                    "id TEXT PRIMARY KEY," +
                    "name TEXT NOT NULL CHECK (length(name) <= 2000)," +
                    "description TEXT NOT NULL CHECK (length(description) <= 10000)" +
                ")"
            );
        }
    }

    private static void createMediaTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()){
            statement.execute(
                "CREATE TABLE IF NOT EXISTS promo_media (" +
                    "id TEXT PRIMARY KEY," +
                    "event_id TEXT NOT NULL," +
                    "image_url TEXT NOT NULL," + // enforce url format in java code
                    "FOREIGN KEY (event_id) REFERENCES concert_event(id) ON DELETE CASCADE" +
                ")"
            );
        }
    }


    // join tables for many to many relationships

    private static void createEventArtistTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()){
            statement.execute(
                "CREATE TABLE IF NOT EXISTS event_artist (" +
                    "event_id TEXT NOT NULL," +
                    "artist_id TEXT NOT NULL," +
                    "PRIMARY KEY (event_id, artist_id)," +
                    "FOREIGN KEY (event_id) REFERENCES concert_event(id) ON DELETE CASCADE," +
                    "FOREIGN KEY (artist_id) REFERENCES artist(id) ON DELETE CASCADE" +
                ")"
            );
        }
    }

    private static void createEventCategoryTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()){
            statement.execute(
                "CREATE TABLE IF NOT EXISTS event_category (" +
                    "event_id TEXT NOT NULL," +
                    "category_id TEXT NOT NULL," +
                    "PRIMARY KEY (event_id, category_id)," +
                    "FOREIGN KEY (event_id) REFERENCES concert_event(id) ON DELETE CASCADE," +
                    "FOREIGN KEY (category_id) REFERENCES booking_category(id) ON DELETE CASCADE" +
                ")"
            );
        }
    }
}
