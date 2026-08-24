// Author: <your name>
package boundary;

import control.WalkInRegistrationControl;
import entity.Booking;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class WalkInRegistrationUI {

    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String TABLE_LINE =
            "----------------------------------------------------------------------------------------------------------------------";
    private static final String TABLE_HEADER_FMT = "%-10s %-15s %-10s %-7s %-17s %-17s %-7s %-10s %-10s%n";
    private static final String TABLE_ROW_FMT    = "%-10s %-15s %-10s %-7d %-17s %-17s %-7d RM%-8.2f %-10s%n";

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
            System.out.println("1. Register new booking (Standard / advance reservation)");
            System.out.println("2. Register Walk-In (On The Spot - checking in right now)");
            System.out.println("3. Process next booking (assign room)");
            System.out.println("4. Cancel a booking");
            System.out.println("5. Check-out a booking (mark room dirty)");
            System.out.println("6. View current queue (Available)");
            System.out.println("7. View booked / checked-out rooms");
            System.out.println("8. Report: Bookings sorted by room type");
            System.out.println("9. Report: Filter bookings by room type");
            System.out.println("10. Assign room manually (optional - pick a specific booking)");
            System.out.println("0. Exit");
            System.out.print("Enter choice: ");
            choice = readMenuChoice();

            switch (choice) {
                case 1 -> registerBooking();
                case 2 -> registerWalkIn();
                case 3 -> processNextBooking();
                case 4 -> cancelBooking();
                case 5 -> checkOutBooking();
                case 6 -> viewQueue();
                case 7 -> viewProcessedLog();
                case 8 -> showSortedReport();
                case 9 -> showFilterReport();
                case 10 -> assignRoomManually();
                case 0 -> System.out.println("Exiting...");
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
        String name = promptGuestName();
        String contact = promptContactNumber();
        String roomType = promptRoomType();
        int numGuests = promptIntInRange("Number of Guests (1-6): ", 1, 6);

        // Advance booking: must be a future date (per policy, not the current day)
        attemptAdvanceBooking(name, contact, roomType, numGuests);
    }

    // Loops asking for a future date until a room is available, or the customer
    // declines to try another date (per policy: b. no -> ask other date, c. still no -> leave)
    private void attemptAdvanceBooking(String name, String contact, String roomType, int numGuests) {
        while (true) {
            LocalDateTime checkIn = promptFutureCheckInDateTime();
            LocalDateTime checkOut;
            while (true) {
                checkOut = promptDateTime("Check-Out Date & Time (yyyy-MM-dd HH:mm): ");
                if (!checkOut.isAfter(checkIn)) {
                    System.out.println("Check-out must be after check-in.");
                    continue;
                }
                break;
            }

            if (control.isRoomAvailable(roomType, checkIn, checkOut)) {
                Booking booking = control.registerBooking(name, contact, roomType, numGuests, checkIn, checkOut);
                System.out.printf("Advance booking confirmed: %s (Total: RM%.2f)%n", booking, booking.getTotalPrice());
                return;
            }

            System.out.println("No " + roomType + " rooms available for that date range.");
            System.out.print("Try a different date? (Y/N): ");
            if (!sc.nextLine().trim().equalsIgnoreCase("Y")) {
                System.out.println("No booking made - customer left.");
                return;
            }
            // loop back and ask for another date
        }
    }

    // ON THE SPOT: guest is standing at the counter right now, requesting a room
    // for TODAY. Per policy: check today's availability first - if full, offer
    // to redirect them into an advance booking for a future date instead.
    private void registerWalkIn() {
        String name = promptGuestName();
        String contact = promptContactNumber();
        String roomType = promptRoomType();
        int numGuests = promptIntInRange("Number of Guests (1-6): ", 1, 6);

        LocalDateTime checkInNow = LocalDateTime.now();
        System.out.println("Check-In: " + checkInNow.toString().replace("T", " ") + " (now)");

        LocalDateTime checkOut;
        while (true) {
            checkOut = promptDateTime("Check-Out Date & Time (yyyy-MM-dd HH:mm): ");
            if (!checkOut.isAfter(checkInNow)) {
                System.out.println("Check-out must be after check-in (now).");
                continue;
            }
            break;
        }

        if (control.isRoomAvailable(roomType, checkInNow, checkOut)) {
            Booking booking = control.registerWalkInOnTheSpot(name, contact, roomType, numGuests, checkOut);
            System.out.printf("Walk-in checked in immediately (BOOKED): %s (Total: RM%.2f)%n",
                    booking, booking.getTotalPrice());
            return;
        }

        System.out.println("No " + roomType + " rooms available for today.");
        System.out.print("Would you like to make an advance booking for a future date instead? (Y/N): ");
        if (sc.nextLine().trim().equalsIgnoreCase("Y")) {
            attemptAdvanceBooking(name, contact, roomType, numGuests);
        } else {
            System.out.println("No booking made - customer left.");
        }
    }

    private String promptGuestName() {
        String name;
        do {
            System.out.print("Guest Name: ");
            name = sc.nextLine().trim();
            if (name.isEmpty() || !name.matches("[a-zA-Z .'-]+")) {
                System.out.println("Invalid name - letters only, cannot be empty.");
                name = "";
            }
        } while (name.isEmpty());
        return name;
    }

    private String promptContactNumber() {
        String contact;
        do {
            System.out.print("Contact Number (digits only): ");
            contact = sc.nextLine().trim();
            if (!contact.matches("\\d{7,15}")) {
                System.out.println("Invalid contact number - enter 7 to 15 digits, no letters or symbols.");
                contact = "";
            }
        } while (contact.isEmpty());
        return contact;
    }

    // Advance bookings must be for a future date - NOT today (per policy: walk-in
    // only covers the present day, advance only covers future dates).
    private LocalDateTime promptFutureCheckInDateTime() {
    while (true) {
        System.out.print("Check-In Date & Time (yyyy-MM-dd HH:mm): ");
        String input = sc.nextLine().trim();
        try {
            LocalDateTime dt = LocalDateTime.parse(input, DATETIME_FORMAT);
// Must be AFTER today (tomorrow or later)
if (!dt.toLocalDate().isAfter(LocalDate.now())) {
    System.out.println("Advance bookings must be for a future date (tomorrow onwards), not today. Try again.");
    continue;
}
return dt;
        } catch (DateTimeParseException e) {
            System.out.println("Invalid format - use yyyy-MM-dd HH:mm, e.g. 2026-08-15 14:00.");
        }
    }
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

    private LocalDateTime promptDateTime(String prompt) {
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
        Booking next = control.processNextBooking();
        System.out.println("Processed and assigned (now BOOKED): " + next);
    }

    // OPTIONAL: front desk manually picks which waiting booking gets a room next,
    // instead of always taking the one at the front of the queue.
    private void assignRoomManually() {
        if (control.getQueueSize() == 0) {
            System.out.println("Queue is empty - nothing to assign.");
            return;
        }
        viewQueue();

        String id;
        while (true) {
            System.out.print("Booking ID to manually assign a room to (or 0 to go back): ");
            id = sc.nextLine().trim();
            if (id.equals("0")) {
                return;
            }
            if (control.bookingExists(id)) {
                break;
            }
            System.out.println("No booking with that ID is currently in the queue. Try again.");
        }

        Booking assigned = control.assignRoomManually(id);
        System.out.println("Manually assigned (now BOOKED): " + assigned);
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
        System.out.printf(TABLE_HEADER_FMT, "BookingID", "Guest", "RoomType", "Guests", "CheckIn", "CheckOut", "Nights", "Total", "Status");
        System.out.println(TABLE_LINE);
        for (Booking b : bookings) {
            System.out.printf(TABLE_ROW_FMT,
                    b.getBookingId(), b.getGuest().getName(), b.getRoomType(), b.getNumGuests(),
                    b.getCheckInDateTime().toString().replace("T", " "),
                    b.getCheckOutDateTime().toString().replace("T", " "),
                    b.getNights(), b.getTotalPrice(), b.getStatus());
        }
        System.out.println(TABLE_LINE);
    }

    public static void main(String[] args) {
        WalkInRegistrationUI ui = new WalkInRegistrationUI();
        ui.runMenu();
    }
}