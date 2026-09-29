package org.festhub;

import java.sql.SQLException;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        Database.initialize();
        System.out.println("Welcome to FestHub! Type 'help' or 'exit'.");
        printHelp();

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("<FestHub> ");
                String input = scanner.nextLine().trim();

                if (input.isEmpty()) continue;
                if (input.equalsIgnoreCase("exit")) break;
                if (input.equalsIgnoreCase("help")) { printHelp(); continue; }

                String[] t = input.split("\\s+");
                if (t.length < 2) { System.out.println("Expected: [type] [action] [args...]"); continue; }
                String[] a = Arrays.copyOfRange(t, 2, t.length);

                try {
                    switch (t[0]) {
                        case "1" -> ConcertEvent.run(t[1], a);
                        case "2" -> Artist.run(t[1], a);
                        case "3" -> BookingCategory.run(t[1], a);
                        case "4" -> PromoMedia.run(t[1], a);
                        default  -> System.out.println("Unknown record type. Allowed: 1, 2, 3, 4");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Error: expected a number (" + e.getMessage() + ")");
                }
//                catch (SQLException e) {
//                    System.out.println("Database error: " + e.getMessage());
//                }
            }
        }
    }

    private static void printHelp() {
        System.out.println("Types: 1=Concert Events, 2=Artists, 3=Booking Categories, 4=Promo Media");
        System.out.println("Actions: 1=Create, 2=Read, 3=Update, 4=Delete");
    }
}