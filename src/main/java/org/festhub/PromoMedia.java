package org.festhub;

import java.util.Scanner;

class PromoMedia {
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
                System.out.println("Promotional Media created.");
            }
            case "2" -> {
                System.out.println("Promotional Media read.");
            }
            case "3" -> {
                System.out.println("Promotional Media updated.");
            }
            case "4" -> {
                System.out.println("Promotional Media deleted.");
            }
            default -> System.out.println("Unknown action. Allowed: 1, 2, 3, 4");
        }
    }
}