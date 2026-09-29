package org.festhub;

class PromoMedia {
    // Checks if there are enough parameters for operation. Note: 2 is placeholder
    private static boolean hasArgs(String[] a, int n) {
        if (a.length < n) {
            System.out.println("Error: this action requires " + n + " argument(s).");
            return false;
        }
        return true;
    }

    static void run(String action, String[] a) {
        switch (action) {
            case "1" -> {
                if (!hasArgs(a, 2)) return;
                System.out.println("Promotional Media created.");
            }
            case "2" -> {
                if (!hasArgs(a, 2)) return;
                System.out.println("Promotional Media read.");
            }
            case "3" -> {
                if (!hasArgs(a, 2)) return;
                System.out.println("Promotional Media updated.");
            }
            case "4" -> {
                if (!hasArgs(a, 2)) return;
                System.out.println("Promotional Media deleted.");
            }
            default -> System.out.println("Unknown action. Allowed: 1, 2, 3, 4");
        }
    }
}