package org.festhub;
import java.util.Scanner;

class ConcertEvent {
    // Checks if there are enough parameters for operation. Note: 2 is placeholder
    private static boolean hasArgs(String[] a, int n) {
        if (a.length < n) {
            System.out.println("Error: this action requires " + n + " argument(s).");
            return false;
        }
        return true;
    }

    static void run(String action, Scanner scanner) {
        switch (action) {
            case "1" -> {
                createConcertEvent(scanner);
                System.out.println("Concert Event created.");
            }
            case "2" -> {
                System.out.println("Concert Event read.");
            }
            case "3" -> {
                System.out.println("Concert Event updated.");
            }
            case "4" -> {
                System.out.println("Concert Event deleted.");
            }
            default -> System.out.println("Unknown action. Allowed: 1, 2, 3, 4");
        }
    }

    private static void createConcertEvent(Scanner scanner) {
        // tbd
    }

    private static void readConcertEvent(Scanner scanner) {
        // tbd
    }   

    private static void updateConcertEvent(Scanner scanner) {
        // tbd
    }

    private static void deleteConcertEvent(Scanner scanner) {
        // tbd
    }
}