package boundary;

import adt.ListInterface;
import control.WalkInRegistrationControl;
import entity.Booking;
import entity.Room;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class WalkInRegistrationUI {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private WalkInRegistrationControl control;
    private Scanner scanner;

    public WalkInRegistrationUI() {
        this.control = new WalkInRegistrationControl();
        this.scanner = new Scanner(System.in);
    }

    public void displayMenu() {
        int choice = -1;
        do {
            System.out.println("\n=== Walk-In Registration & Queue Management System ===");
            System.out.println("1. Register New Walk-In Customer");
            System.out.println("2. Process Next Queue Item");
            System.out.println("3. View Current Queue");
            System.out.println("4. Cancel Registration in Queue");
            System.out.println("5. View Processed Registrations Log");
            System.out.println("6. Report: Bookings Sorted by Room Type & Time");
            System.out.println("7. Report: Filter Bookings by Room Type");
            System.out.println("8. Report: Queue Summary & Revenue Projections");
            System.out.println("0. Exit");
            System.out.print("Enter choice: ");

            try {
                choice = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
                continue;
            }

            switch (choice) {
                case 1 -> registerNewCustomer();
                case 2 -> processNextBooking();
                case 3 -> viewQueue();
                case 4 -> cancelBooking();
                case 5 -> viewProcessedLog();
                case 6 -> showSortedReport();
                case 7 -> showFilterReport();
                case 8 -> showQueueSummaryReport();
                case 0 -> System.out.println("Exiting Walk-In Registration System.");
                default -> System.out.println("Invalid option. Try again.");
            }
        } while (choice != 0);
    }

    private void registerNewCustomer() {
        System.out.println("\n--- Register Walk-In Customer ---");
        String guestName = promptString("Guest Name: ");
        String contact = promptString("Contact Number: ");
        String icPassport = promptString("IC / Passport: ");

        String roomType = promptRoomType();
        int numGuests = promptInt("Number of Guests: ", 1, 10);
        int nights = promptInt("Number of Nights: ", 1, 365);

        LocalDateTime checkIn = LocalDateTime.now();
        LocalDateTime checkOut = checkIn.plusDays(nights);

        if (!control.isRoomAvailable(roomType, checkIn, checkOut)) {
            System.out.println("[!] Warning: No " + roomType + " rooms are currently set to AVAILABLE.");
            System.out.print("Proceed adding to queue anyway? (Y/N): ");
            String ans = scanner.nextLine().trim();
            if (!ans.equalsIgnoreCase("Y")) {
                System.out.println("Registration aborted.");
                return;
            }
        }

        Booking booking = control.registerWalkInBooking(guestName, contact, icPassport, roomType, numGuests, nights);
        System.out.println("\n[+] Guest added to queue successfully!");
        System.out.println("    Confirmation No : " + booking.getConfirmationNo());
        System.out.println("    Queue Position  : #" + control.getQueueSize());
        System.out.println("    Check-In Time   : " + booking.getCheckInDateTime().format(FORMATTER));
        System.out.println("    Check-Out Time  : " + booking.getCheckOutDateTime().format(FORMATTER));
        System.out.println("    Est. Nightly Rate: RM " + String.format("%.2f", booking.getPricePerNight()));
    }

    private void processNextBooking() {
        if (control.getQueueSize() == 0) {
            System.out.println("[!] Queue is empty - no booking to process.");
            return;
        }

        Booking next = control.getNextBooking();
        if (next == null) {
            System.out.println("[!] Queue is empty - no booking to process.");
            return;
        }

        System.out.printf("\nNext in queue: %s | Guest: %s | Room Type: %s\n",
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

    private Room chooseRoom(String roomType, ListInterface<Room> availableRooms) {
        System.out.println("\nAvailable " + roomType + " rooms:");
        for (int i = 0; i < availableRooms.size(); i++) {
            Room r = availableRooms.get(i);
            System.out.printf("  - Room %s\n", r.getRoomNo());
        }

        while (true) {
            System.out.print("Enter Room No to assign (or '0' to cancel): ");
            String input = scanner.nextLine().trim();
            if (input.equals("0")) return null;

            Room matched = control.getAvailableRoomByRoomNo(roomType, input);
            if (matched != null) {
                return matched;
            }
            System.out.println("[!] Invalid room number or not available in list. Try again.");
        }
    }

    private void printConfirmationCard(Booking b, Room r) {
        System.out.println("\n========================================");
        System.out.println("     REGISTRATION CONFIRMED             ");
        System.out.println("========================================");
        System.out.println(" Confirmation No : " + b.getConfirmationNo());
        System.out.println(" Guest Name      : " + b.getGuest().getGuestName());
        System.out.println(" Room Assigned   : " + r.getRoomNo() + " (" + r.getRoomType() + ")");
        System.out.println(" Check-In        : " + b.getCheckInDateTime().format(FORMATTER));
        System.out.println(" Check-Out       : " + b.getCheckOutDateTime().format(FORMATTER));
        System.out.println(" Status          : " + b.getStatus());
        System.out.println("========================================\n");
    }

    private void viewQueue() {
        Booking[] queue = control.viewQueue();
        if (queue.length == 0) {
            System.out.println("Queue is empty.");
            return;
        }
        System.out.println("\n--- Current Pending Queue (" + queue.length + " items) ---");
        printTable(queue);
    }

    private void cancelBooking() {
        String confNo = promptString("Enter Confirmation No to cancel: ");
        if (!control.bookingExists(confNo)) {
            System.out.println("[!] Confirmation No not found in queue.");
            return;
        }

        Booking cancelled = control.cancelBooking(confNo);
        if (cancelled != null) {
            System.out.println("[+] Booking " + confNo + " successfully cancelled and removed from queue.");
        } else {
            System.out.println("[!] Could not cancel booking.");
        }
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
        ListInterface<Booking> list = control.getBookingsSortedByRoomType();
        Booking[] sortedBookings = new Booking[list.size()];
        for (int i = 0; i < list.size(); i++) {
            sortedBookings[i] = list.get(i);
        }
        System.out.println("--- Bookings Sorted by Room Type, then Check-In Time ---");
        if (sortedBookings.length == 0) {
            System.out.println("No bookings to show.");
            return;
        }
        printTable(sortedBookings);
    }

    private void showFilterReport() {
        String roomType = promptRoomType();
        ListInterface<Booking> list = control.filterByRoomType(roomType);
        Booking[] filteredBookings = new Booking[list.size()];
        for (int i = 0; i < list.size(); i++) {
            filteredBookings[i] = list.get(i);
        }
        System.out.println("--- Bookings for Room Type: " + roomType + " ---");
        if (filteredBookings.length == 0) {
            System.out.println("No bookings found for this room type.");
            return;
        }
        printTable(filteredBookings);
    }

    private void showQueueSummaryReport() {
        System.out.println("\n--- Queue Summary & Revenue Projection ---");
        System.out.println(" Pending Queue Items : " + control.getQueueSize());
        System.out.printf(" Est. Total Revenue  : RM %.2f\n", control.getTotalQueueRevenue());
    }

    private void printTable(Booking[] bookings) {
        System.out.printf("%-15s %-20s %-12s %-8s %-18s %-10s\n",
                "Confirm No", "Guest Name", "Room Type", "Guests", "Check-In", "Status");
        System.out.println("----------------------------------------------------------------------------------");
        for (Booking b : bookings) {
            if (b != null) {
                System.out.printf("%-15s %-20s %-12s %-8d %-18s %-10s\n",
                        b.getConfirmationNo(),
                        b.getGuest().getGuestName(),
                        b.getRoomType(),
                        b.getNumGuests(),
                        b.getCheckInDateTime().format(FORMATTER),
                        b.getStatus());
            }
        }
    }

    private String promptString(String label) {
        String input;
        do {
            System.out.print(label);
            input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                System.out.println("Input cannot be empty.");
            }
        } while (input.isEmpty());
        return input;
    }

    private String promptRoomType() {
        System.out.println("Select Room Type:");
        for (int i = 0; i < WalkInRegistrationControl.ROOM_TYPES.length; i++) {
            System.out.printf("  %d. %s\n", i + 1, WalkInRegistrationControl.ROOM_TYPES[i]);
        }
        int sel = promptInt("Choice: ", 1, WalkInRegistrationControl.ROOM_TYPES.length);
        return WalkInRegistrationControl.ROOM_TYPES[sel - 1];
    }

    private int promptInt(String label, int min, int max) {
        while (true) {
            System.out.print(label);
            try {
                int val = Integer.parseInt(scanner.nextLine().trim());
                if (val >= min && val <= max) return val;
                System.out.printf("Please enter a value between %d and %d.\n", min, max);
            } catch (NumberFormatException e) {
                System.out.println("Invalid integer.");
            }
        }
    }
}