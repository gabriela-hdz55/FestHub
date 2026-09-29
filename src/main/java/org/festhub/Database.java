package org.festhub;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {
    private static final String DATABASE_URL = "jdbc:sqlite:data/festhub.db";

    private Database(){}

    private static Connection connect() throws SQLException {
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
            System.out.println("Connected to FestHub database.");
        } catch (SQLException exception){
            throw new RuntimeException(
                "Failed to initialize FestHub database.",
                exception
            );
        }
    }
}
