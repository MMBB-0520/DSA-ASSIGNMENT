package util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/**
 * Utility class for robust console input reading and validation.
 * Encapsulates Scanner interaction, type conversion, and bounds validation (SRP).
 */
public class InputUtil {

    private InputUtil() {
        // Private constructor to prevent instantiation of utility class
    }

    public static boolean readYesNo(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("Y") || input.equalsIgnoreCase("YES")) {
                return true;
            } else if (input.equalsIgnoreCase("N") || input.equalsIgnoreCase("NO")) {
                return false;
            }
            System.out.println("[!] Invalid input. Please enter 'Y' for Yes or 'N' for No.");
        }
    }

    public static int readIntInRange(Scanner scanner, String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                int val = Integer.parseInt(input);
                if (val >= min && val <= max) {
                    return val;
                }
                System.out.printf("[!] Please enter a number between %d and %d.%n", min, max);
            } catch (NumberFormatException e) {
                System.out.println("[!] Invalid input. Please enter a valid integer.");
            }
        }
    }

    public static int readIntInRangeWithDefault(Scanner scanner, String prompt, int min, int max, int defaultVal) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                return defaultVal;
            }
            try {
                int val = Integer.parseInt(input);
                if (val >= min && val <= max) {
                    return val;
                }
                System.out.printf("[!] Please enter a number between %d and %d (or press Enter for default %d).%n", min,
                        max, defaultVal);
            } catch (NumberFormatException e) {
                System.out.println("[!] Invalid input. Please enter a valid integer.");
            }
        }
    }

    public static double readDoubleMin(Scanner scanner, String prompt, double minAmount) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                double val = Double.parseDouble(input);
                if (Double.isNaN(val) || Double.isInfinite(val)) {
                    System.out.println("[!] Invalid numeric amount.");
                    continue;
                }
                if (val < minAmount) {
                    System.out.printf("[!] Insufficient amount! Must be at least RM %.2f.%n", minAmount);
                    continue;
                }
                return val;
            } catch (NumberFormatException e) {
                System.out.println("[!] Invalid numeric format. Please enter a valid number (e.g., 150.00).");
            }
        }
    }

    public static String readGuestName(Scanner scanner) {
        while (true) {
            System.out.print("Enter Guest Name: ");
            String name = scanner.nextLine().trim();
            if (!name.isEmpty() && name.matches("[a-zA-Z .'-]+")) {
                return name;
            }
            System.out.println("[!] Invalid name - letters only, cannot be empty.");
        }
    }

    public static String readContactNumber(Scanner scanner) {
        while (true) {
            System.out.print("Enter Guest Contact Number (7-15 digits only): ");
            String contact = scanner.nextLine().trim();
            if (contact.matches("\\d{7,15}")) {
                return contact;
            }
            System.out.println("[!] Invalid contact number - enter 7 to 15 digits, no letters or symbols.");
        }
    }

    public static String readIcPassport(Scanner scanner) {
        while (true) {
            System.out.print("Enter IC/Passport Number: ");
            String icPassport = scanner.nextLine().trim();
            if (icPassport.isEmpty()) {
                System.out.println("[!] IC/Passport Number cannot be empty.");
            } else if (icPassport.matches("\\d+")) {
                if (icPassport.matches("\\d{12}")) {
                    return icPassport;
                } else {
                    System.out.println("[!] IC must contain exactly 12 digits.");
                }
            } else {
                if (icPassport.matches("[A-Za-z0-9]{6,15}")) {
                    return icPassport;
                } else {
                    System.out.println("[!] Invalid Passport format (6-15 alphanumeric characters).");
                }
            }
        }
    }

    public static LocalDateTime readFutureDateTime(Scanner scanner, String prompt, DateTimeFormatter formatter) {
        while (true) {
            System.out.print(prompt);
            String str = scanner.nextLine().trim();
            try {
                LocalDateTime dt = LocalDateTime.parse(str, formatter);
                if (dt.isBefore(LocalDateTime.now().minusMinutes(5))) {
                    System.out.println("[!] Check-In date and time cannot be in the past.");
                    continue;
                }
                return dt;
            } catch (DateTimeParseException e) {
                System.out.println("[!] Invalid date format. Use yyyy-MM-dd HH:mm (e.g. 2026-08-20 15:00).");
            }
        }
    }
}
