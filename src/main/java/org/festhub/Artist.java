package org.festhub;
import java.util.Scanner;

class Artist {
    // Checks if there are enough parameters for operation. Note: 0 is placeholder
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
                System.out.println("Artist created.");
            }
            case "2" -> {
                System.out.println("Artist read.");
            }
            case "3" -> {
                System.out.println("Artist updated.");
            }
            case "4" -> {
                System.out.println("Artist deleted.");
            }
            default -> System.out.println("Unknown action. Allowed: 1, 2, 3, 4");
        }
    }
}