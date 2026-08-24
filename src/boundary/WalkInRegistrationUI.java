// Author: <your name>
package boundary;

import adt.ListInterface;
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
            System.out.println("\n***************************************************");
            System.out.println("*    WALK-IN REGISTRATION & BOOKING SUBSYSTEM     *");
            System.out.println("***************************************************");
            System.out.println("* 1. Register New Walk-In Booking                 *");
            System.out.println("* 2. Process Next Booking (Assign Room)           *");
            System.out.println("* 3. Cancel Booking                               *");
            System.out.println("* 4. View Pending Queue                           *");
            System.out.println("* 5. View Confirmed Bookings                      *");
            System.out.println("* 6. Report: Bookings Sorted by Room Type         *");
            System.out.println("* 7. Report: Filter Bookings by Room Type         *");
            System.out.println("* 0. Return to Main Menu                          *");
            System.out.println("***************************************************");
            System.out.print("Please enter choice: ");
            choice = readMenuChoice();

            switch (choice) {
                case 1 -> registerBooking();
                case 2 -> processNextBooking();
                case 3 -> cancelBooking();
                case 4 -> viewQueue();
                case 5 -> viewProcessedLog();
                case 6 -> showSortedReport();
                case 7 -> showFilterReport();
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

    public void registerBooking() {
        String promptName = promptGuestName();
        String contact = promptContactNumber();
        String icPassport = promptICPassport();
        String roomType = promptRoomType();
        int numGuests = promptIntInRange("Number of Guests (1-6): ", 1, 6);

        // 1. Walk-in automatically targets TODAY
        LocalDateTime checkInNow = LocalDateTime.now();
        System.out.println("\n[WALK-IN] Check-In date automatically set to today: " + checkInNow.format(DATETIME_FORMAT));

        // 2. Prompt ONLY for Check-Out date
        LocalDateTime checkOut;
        while (true) {
            checkOut = promptDateTime("Check-Out Date & Time (yyyy-MM-dd HH:mm): ", checkInNow);
            if (checkOut.isAfter(checkInNow)) {
                break;
            }
            System.out.println("Check-out must be after check-in.");
        }

        // 3. Check if current day has a free room slot
        if (control.isRoomAvailable(roomType, checkInNow, checkOut)) {
            Booking booking = control.registerBooking(promptName, contact, icPassport, roomType, numGuests, checkInNow, checkOut);
            System.out.println("\n[SUCCESS] Room available for today! Booking confirmed.");
            printInvoiceCard(booking);
        } else {
            // 4. NO room for present day -> Offer Advance Booking for future dates
            System.out.println("\n[NO VACANCY] No " + roomType + " rooms available for today.");
            System.out.print("Would you like to make an advance booking for a future date instead? (Y/N): ");
            String choice = sc.nextLine().trim();

            if (choice.equalsIgnoreCase("Y")) {
                attemptAdvanceBooking(promptName, contact, roomType, numGuests);
            } else {
                System.out.println("Customer declined advance booking. Transaction ended.");
            }
        }
    }

    private void printInvoiceCard(Booking booking) {
        System.out.println("\n============================================================");
        System.out.println("            RESORT BOOKING REGISTRATION INVOICE             ");
        System.out.println("============================================================");
        System.out.printf(" Confirmation No : %s%n", booking.getConfirmationNo());
        System.out.printf(" Guest Name      : %s%n", booking.getGuest().getGuestName());
        System.out.printf(" Contact Number  : %s%n", booking.getGuest().getContactNumber());
        System.out.printf(" IC / Passport   : %s%n", booking.getGuest().getIcPassport());
        System.out.println("------------------------------------------------------------");
        System.out.printf(" Room Type       : %s (RM %.2f / night)%n", booking.getRoomType(),
                booking.getPricePerNight());
        System.out.printf(" Number of Guests: %d Person(s)%n", booking.getNumGuests());
        System.out.printf(" Check-In Date   : %s%n", booking.getCheckInDateTime().format(DATETIME_FORMAT));
        System.out.printf(" Check-Out Date  : %s%n", booking.getCheckOutDateTime().format(DATETIME_FORMAT));
        System.out.printf(" Duration        : %d Night(s)%n", booking.getNights());
        System.out.println("------------------------------------------------------------");
        System.out.printf(" TOTAL AMOUNT    : RM %.2f%n", booking.getBill().getGrandTotal());
        System.out.printf(" STATUS          : %s%n", booking.getStatus());
        System.out.println("============================================================\n");
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
            System.out.println("[!] Queue is empty - no booking to process.");
            return;
        }
        Booking next = control.getNextBooking();
        System.out.printf("%nNext in queue: %s | Guest: %s | Room Type: %s%n",
                next.getConfirmationNo(), next.getGuest().getGuestName(), next.getRoomType());

        ListInterface<Room> availableRooms = control.getAvailableRoomsByType(next.getRoomType());
        if (availableRooms.isEmpty()) {
            System.out.println("[!] No available " + next.getRoomType() + " rooms currently.");
            return;
        }

        Room room = chooseRoom(next.getRoomType(), availableRooms);
        if (room == null) {
            System.out.println("Cancelled - booking remains at the front of the queue.");
            return;
        }

        Booking result = control.processNextBooking(room);
        printConfirmationCard(result, room);
    }

    // Lets staff auto-assign the next available room or manually pick one from
    // the list (built using the CustomLinkedList linear ADT).
    private Room chooseRoom(String roomType, ListInterface<Room> availableRooms) {
        while (true) {
            System.out.println("1. Auto-assign next available room");
            System.out.println("2. Manually select a room");
            System.out.println("0. Cancel");
            System.out.print("Enter choice: ");
            String input = sc.nextLine().trim();

            switch (input) {
                case "1":
                    return availableRooms.removeFirst();
                case "2":
                    return selectRoomManually(roomType, availableRooms);
                case "0":
                    return null;
                default:
                    System.out.println("Invalid choice - enter 1, 2 or 0.");
            }
        }
    }

    private Room selectRoomManually(String roomType, ListInterface<Room> availableRooms) {
        System.out.println("Available " + roomType + " rooms:");
        for (int i = 0; i < availableRooms.size(); i++) {
            System.out.printf("  %d. Room %s%n", i + 1, availableRooms.get(i).getRoomNo());
        }
        while (true) {
            System.out.print("Enter room number to assign (0 to cancel): ");
            String roomInput = sc.nextLine().trim();
            if (roomInput.equals("0")) {
                return null;
            }
            Room room = control.getAvailableRoomByRoomNo(roomType, roomInput);
            if (room != null) {
                return room;
            }
            System.out.println("[!] That room isn't currently available. Try again.");
        }
    }

    private void printConfirmationCard(Booking booking, Room room) {
        System.out.println("\n============================================================");
        System.out.println("            ROOM ASSIGNED & BOOKING CONFIRMED               ");
        System.out.println("============================================================");
        System.out.printf(" Confirmation No : %s%n", booking.getConfirmationNo());
        System.out.printf(" Guest Name      : %s%n", booking.getGuest().getGuestName());
        System.out.printf(" Assigned Room   : Room %s (%s)%n", room.getRoomNo(), booking.getRoomType());
        System.out.printf(" Room Rate       : RM %.2f / night%n", booking.getPricePerNight());
        System.out.println("------------------------------------------------------------");
        System.out.printf(" Number of Guests: %d Person(s)%n", booking.getNumGuests());
        System.out.printf(" Check-In Date   : %s%n", booking.getCheckInDateTime().format(DATETIME_FORMAT));
        System.out.printf(" Check-Out Date  : %s%n", booking.getCheckOutDateTime().format(DATETIME_FORMAT));
        System.out.printf(" Duration        : %d Night(s)%n", booking.getNights());
        System.out.printf(" Total Amount    : RM %.2f%n", booking.getBill().getGrandTotal());
        System.out.println("------------------------------------------------------------");
        System.out.printf(" STATUS          : %s%n", booking.getStatus());
        System.out.println("============================================================\n");
    }

    private void cancelBooking() {
        if (control.getQueueSize() == 0) {
            System.out.println("[!] Queue is empty - nothing to cancel.");
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
            System.out.println("[!] No booking with that ID is currently in the queue. Try again.");
        }

        Booking cancelled = control.cancelBooking(id);
        System.out.println("\n[√] BOOKING CANCELLED SUCCESSFULLY:");
        System.out.printf("    Confirmation No: %s | Guest: %s | Status: %s%n\n",
                cancelled.getConfirmationNo(), cancelled.getGuest().getGuestName(), cancelled.getStatus());
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
            total += b.getBill().getGrandTotal();
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
                    b.getNights(), b.getBill().getGrandTotal(), b.getStatus());
        }
        System.out.println(TABLE_LINE);
    }
}
