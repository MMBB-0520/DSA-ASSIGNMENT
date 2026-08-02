// Author: <your name>
package boundary;

import control.WalkInRegistrationControl;
import entity.Booking;
import entity.Room;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class WalkInRegistrationUI {

    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String TABLE_LINE = "----------------------------------------------------------------------------------------------------------------------";
    private static final String TABLE_HEADER_FMT = "%-10s %-15s %-10s %-7s %-17s %-17s %-7s %-10s %-10s%n";
    private static final String TABLE_ROW_FMT = "%-10s %-15s %-10s %-7d %-17s %-17s %-7d RM%-8.2f %-10s%n";

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
            System.out.println("4. Check-out a booking (mark room dirty)");
            System.out.println("5. View pending queue");
            System.out.println("6. View confirmed bookings");
            System.out.println("7. Report: Bookings sorted by room type");
            System.out.println("8. Report: Filter bookings by room type");
            System.out.println("0. Return to Main Menu");
            System.out.print("Enter choice: ");
            choice = readMenuChoice();

            switch (choice) {
                case 1 -> registerBooking();
                case 2 -> processNextBooking();
                case 3 -> cancelBooking();
                case 4 -> checkOutBooking();
                case 5 -> viewQueue();
                case 6 -> viewProcessedLog();
                case 7 -> showSortedReport();
                case 8 -> showFilterReport();
                case 0 -> System.out.println("Returning to Main Menu...");
                default -> System.out.println("Invalid choice.");
            }
        } while (choice != 0);
    }

    private int readMenuChoice() {
        try {
            return Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
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

        String icPassport;
        do {
            System.out.print("IC/Passport No: ");
            icPassport = sc.nextLine().trim();

            if (icPassport.isEmpty()) {
                System.out.println("IC/Passport No cannot be empty.");

            } else if (icPassport.matches("\\d+")) {
                if (!icPassport.matches("\\d{12}")) {
                    System.out.println("IC must contain 12 digits.");
                    icPassport = "";
                }
            } else {
                if (!icPassport.matches("[A-Za-z0-9]{6,15}")) {
                    System.out.println("Invalid Passport format.");
                    icPassport = "";
                }
            }
        } while (icPassport.isEmpty());

        String roomType = promptRoomType();
        int numGuests = promptIntInRange("Number of Guests (1-6): ", 1, 6);

        LocalDateTime checkIn = promptDateTime("Check-In Date & Time (yyyy-MM-dd HH:mm, now or later): ", null);
        LocalDateTime checkOut;
        while (true) {
            checkOut = promptDateTime("Check-Out Date & Time (yyyy-MM-dd HH:mm): ", null);
            if (!checkOut.isAfter(checkIn)) {
                System.out.println("Check-out must be after check-in.");
                continue;
            }
            break;
        }

        Booking booking = control.registerBooking(name, contact, icPassport, roomType, numGuests, checkIn, checkOut);
        System.out.printf("Registered: %s (Total: RM%.2f)%n", booking, booking.getTotalPrice());
    }

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
                // fall through
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

    // requireAfter: if not null, the entered value must be after it (used for
    // check-in vs "now")
    private LocalDateTime promptDateTime(String prompt, LocalDateTime requireAfter) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            try {
                LocalDateTime dt = LocalDateTime.parse(input, DATETIME_FORMAT);
                if (dt.isBefore(LocalDateTime.now().withSecond(0).withNano(0))) {
                    System.out.println("Date/time cannot be in the past.");
                    continue;
                }
                return dt;
            } catch (DateTimeParseException e) {
                System.out.println("Invalid format - use yyyy-MM-dd HH:mm, e.g. 2026-08-15 14:00.");
            }
        }
    }

    private void processNextBooking() {
        if (control.getQueueSize() == 0) {
            System.out.println("Queue is empty - no booking to process.");
            return;
        }
        Booking next = control.getNextBooking();
        Room room = control.getAvailableRoom(next.getRoomType());
        if (room == null) {
            System.out.println("No available " + next.getRoomType() + " rooms.");
            return;
        }
        Booking result = control.processNextBooking(room);
        System.out.println(
                "Assigned Room " + room.getRoomNo()
                        + ": " + result);
    }

    private void cancelBooking() {
        if (control.getQueueSize() == 0) {
            System.out.println("Queue is empty - nothing to cancel.");
            return;
        }
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

    private void checkOutBooking() {
        if (control.getProcessedLogSize() == 0) {
            System.out.println("No booked rooms to check out.");
            return;
        }
        viewProcessedLog();

        String id;
        while (true) {
            System.out.print("Booking ID to check out (or 0 to go back): ");
            id = sc.nextLine().trim();
            if (id.equals("0")) {
                return;
            }
            if (control.processedBookingExists(id)) {
                break;
            }
            System.out.println("No BOOKED room with that ID found. Try again.");
        }

        Booking checkedOut = control.checkOutBooking(id);
        System.out.println("Checked out - room now DIRTY: " + checkedOut);
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

    private void viewProcessedLog() {
        Booking[] log = control.viewProcessedLog();
        if (log.length == 0) {
            System.out.println("No booked or checked-out rooms yet.");
            return;
        }
        System.out.println("Booked / checked-out rooms:");
        printTable(log);
    }

    private void showSortedReport() {
        Booking[] sorted = control.getBookingsSortedByRoomType();
        System.out.println("--- Bookings Sorted by Room Type, then Check-In Time ---");
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
        System.out.printf(TABLE_HEADER_FMT, "BookingID", "Guest", "RoomType", "Guests", "CheckIn", "CheckOut", "Nights",
                "Total", "Status");
        System.out.println(TABLE_LINE);
        for (Booking b : bookings) {
            System.out.printf(TABLE_ROW_FMT,
                    b.getConfirmationNo(), b.getGuest().getGuestName(), b.getRoomType(), b.getNumGuests(),
                    b.getCheckInDateTime().toString().replace("T", " "),
                    b.getCheckOutDateTime().toString().replace("T", " "),
                    b.getNights(), b.getTotalPrice(), b.getStatus());
        }
        System.out.println(TABLE_LINE);
    }
}
