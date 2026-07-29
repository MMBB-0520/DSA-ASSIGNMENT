// Author: <your name>
package boundary;

import control.WalkInRegistrationControl;
import entity.Booking;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class WalkInRegistrationUI {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String TABLE_LINE =
            "----------------------------------------------------------------------------------------------------";
    private static final String TABLE_HEADER_FMT = "%-10s %-15s %-10s %-7s %-12s %-7s %-10s %-10s%n";
    private static final String TABLE_ROW_FMT    = "%-10s %-15s %-10s %-7d %-12s %-7d RM%-8.2f %-10s%n";

    private WalkInRegistrationControl control;
    private Scanner sc;

    public WalkInRegistrationUI() {
        control = new WalkInRegistrationControl();
        sc = new Scanner(System.in);
    }

    public void runMenu() {
        int choice;
        do {
            System.out.println("\n=== Walk-In Registration & Standard Booking ===");
            System.out.println("1. Register new booking");
            System.out.println("2. Process next booking (assign room)");
            System.out.println("3. Cancel a booking");
            System.out.println("4. View current queue");
            System.out.println("5. Report: Bookings sorted by room type");
            System.out.println("6. Report: Filter bookings by room type");
            System.out.println("0. Exit");
            System.out.print("Enter choice: ");
            choice = readMenuChoice();

            switch (choice) {
                case 1 -> registerBooking();
                case 2 -> processNextBooking();
                case 3 -> cancelBooking();
                case 4 -> viewQueue();
                case 5 -> showSortedReport();
                case 6 -> showFilterReport();
                case 0 -> System.out.println("Exiting...");
                default -> System.out.println("Invalid choice.");
            }
        } while (choice != 0);
    }

    private int readMenuChoice() {
        try {
            return Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1; // falls into "Invalid choice" in the switch
        }
    }

    private void registerBooking() {
        String name;
        do {
            System.out.print("Guest Name: ");
            name = sc.nextLine().trim();
            if (name.isEmpty() || !name.matches("[a-zA-Z .'-]+")) {
                System.out.println("Invalid name - letters only, cannot be empty.");
                name = "";
            }
        } while (name.isEmpty());

        String contact;
        do {
            System.out.print("Contact Number (digits only): ");
            contact = sc.nextLine().trim();
            if (!contact.matches("\\d{7,15}")) {
                System.out.println("Invalid contact number - enter 7 to 15 digits, no letters or symbols.");
                contact = "";
            }
        } while (contact.isEmpty());

        String roomType = promptRoomType();
        int numGuests = promptIntInRange("Number of Guests (1-6): ", 1, 6);
        LocalDate checkInDate = promptFutureDate("Check-In Date (yyyy-MM-dd, today or later): ");
        int nights = promptIntInRange("Number of Nights (1-30): ", 1, 30);

        Booking booking = control.registerBooking(name, contact, roomType, numGuests, checkInDate, nights);
        System.out.printf("Registered: %s (Total: RM%.2f)%n", booking, booking.getTotalPrice());
    }

    // Forces the user to pick from a fixed menu instead of free-typing a room type.
    private String promptRoomType() {
        String[] roomTypes = WalkInRegistrationControl.ROOM_TYPES;
        while (true) {
            System.out.println("Select Room Type:");
            for (int i = 0; i < roomTypes.length; i++) {
                double price = control.getPriceForRoomType(roomTypes[i]);
                System.out.printf("%d. %-10s (RM%.2f/night)%n", i + 1, roomTypes[i], price);
            }
            System.out.print("Enter choice: ");
            String input = sc.nextLine().trim();
            try {
                int choice = Integer.parseInt(input);
                if (choice >= 1 && choice <= roomTypes.length) {
                    return roomTypes[choice - 1];
                }
            } catch (NumberFormatException e) {
                // fall through to error message below
            }
            System.out.println("Invalid choice - enter a number from the list.");
        }
    }

    private int promptIntInRange(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.println("Enter a number between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid whole number.");
            }
        }
    }

    private LocalDate promptFutureDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            try {
                LocalDate date = LocalDate.parse(input, DATE_FORMAT);
                if (date.isBefore(LocalDate.now())) {
                    System.out.println("Check-in date cannot be in the past.");
                    continue;
                }
                return date;
            } catch (DateTimeParseException e) {
                System.out.println("Invalid format - use yyyy-MM-dd, e.g. 2026-08-15.");
            }
        }
    }

    private void processNextBooking() {
        if (control.getQueueSize() == 0) {
            System.out.println("Queue is empty - no booking to process.");
            return;
        }
        Booking next = control.processNextBooking();
        System.out.println("Processed and assigned: " + next);
    }

    private void cancelBooking() {
        if (control.getQueueSize() == 0) {
            System.out.println("Queue is empty - nothing to cancel.");
            return;
        }

        // Show the valid IDs first so the user picks from what actually exists.
        viewQueue();

        String id;
        while (true) {
            System.out.print("Booking ID to cancel (or 0 to go back): ");
            id = sc.nextLine().trim();
            if (id.equals("0")) {
                return;
            }
            if (control.bookingExists(id)) {
                break;
            }
            System.out.println("No booking with that ID is currently in the queue. Try again.");
        }

        Booking cancelled = control.cancelBooking(id);
        System.out.println("Cancelled: " + cancelled);
    }

    private void viewQueue() {
        Booking[] queue = control.viewQueue();
        if (queue.length == 0) {
            System.out.println("Queue is empty.");
            return;
        }
        System.out.println("Current queue (arrival order):");
        printTable(queue);
        System.out.printf("Total potential revenue: RM%.2f%n", control.getTotalQueueRevenue());
    }

    private void showSortedReport() {
        Booking[] sorted = control.getBookingsSortedByRoomType();
        System.out.println("--- Bookings Sorted by Room Type, then Check-In Date ---");
        if (sorted.length == 0) {
            System.out.println("No bookings to show.");
            return;
        }
        printTable(sorted);
    }

    private void showFilterReport() {
        String roomType = promptRoomType();
        Booking[] filtered = control.filterByRoomType(roomType);
        System.out.println("--- Bookings for Room Type: " + roomType + " ---");
        if (filtered.length == 0) {
            System.out.println("No bookings found for this room type.");
            return;
        }
        printTable(filtered);
        double total = 0;
        for (Booking b : filtered) {
            total += b.getTotalPrice();
        }
        System.out.printf("Subtotal for %s: RM%.2f%n", roomType, total);
    }

    private void printTable(Booking[] bookings) {
        System.out.println(TABLE_LINE);
        System.out.printf(TABLE_HEADER_FMT, "BookingID", "Guest", "RoomType", "Guests", "CheckIn", "Nights", "Total", "Status");
        System.out.println(TABLE_LINE);
        for (Booking b : bookings) {
            System.out.printf(TABLE_ROW_FMT,
                    b.getBookingId(), b.getGuest().getName(), b.getRoomType(),
                    b.getNumGuests(), b.getCheckInDate(), b.getNights(),
                    b.getTotalPrice(), b.getStatus());
        }
        System.out.println(TABLE_LINE);
    }

    public static void main(String[] args) {
        WalkInRegistrationUI ui = new WalkInRegistrationUI();
        ui.runMenu();
    }
}
