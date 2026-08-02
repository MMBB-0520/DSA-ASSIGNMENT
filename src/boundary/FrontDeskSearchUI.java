package boundary;

import control.FrontDeskSearchControl;
import entity.Booking;
import entity.Guest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

/**
 * Boundary UI class for Front-Desk Search module.
 * Provides interactive terminal menus for instant guest lookup using Hash
 * Search.
 */
public class FrontDeskSearchUI {

    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String DIVIDER = "================================================================================";
    private static final String SUB_DIVIDER = "--------------------------------------------------------------------------------";

    private FrontDeskSearchControl control;
    private Scanner scanner;

    public FrontDeskSearchUI() {
        this.control = new FrontDeskSearchControl();
        this.scanner = new Scanner(System.in);
    }

    public void runMenu() {
        int choice;
        do {
            System.out.println("\n" + DIVIDER);
            System.out.println(" FRONT-DESK INQUIRY SERVICE (Hash Table ADT & O(1) Hash Search)");
            System.out.println(DIVIDER);
            System.out.println("1. Instant Search Guest Record (by 8-digit Confirmation No)");
            System.out.println("2. Add New Guest Record to Hash Table");
            System.out.println("3. View ADT & Algorithm Performance Summary");
            System.out.println("0. Return to Main Menu");
            System.out.println(DIVIDER);
            System.out.print("Enter Choice: ");

            choice = readChoice();

            switch (choice) {
                case 1 -> searchByConfirmationNo();
                case 2 -> addNewBookingRecord();
                case 3 -> showAdtSummary();
                case 0 -> System.out.println("\nReturning to Main Menu...");
                default -> System.out.println("\nInvalid choice! Please enter a valid menu option.");
            }
        } while (choice != 0);
    }

    private int readChoice() {
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Executes instant O(1) search by 8-digit confirmation number.
     */
    private void searchByConfirmationNo() {
        System.out.println("\n" + SUB_DIVIDER);
        System.out.println(" INSTANT GUEST LOOKUP (HASH SEARCH)");
        System.out.println(SUB_DIVIDER);
        System.out.print("Enter 8-Digit Confirmation Number (e.g. 84920183): ");
        String confirmationNo = scanner.nextLine().trim();

        if (!confirmationNo.matches("\\d{8}")) {
            System.out.println("\n[!] Error: Confirmation Number must be exactly 8 digits.");
            System.out.println("    (Sample valid numbers for testing: 84920183, 10293847, 59302148, 77621094)");
            return;
        }

        long startTime = System.nanoTime();
        Booking booking = control.searchByConfirmationNo(confirmationNo);
        long endTime = System.nanoTime();
        double elapsedMs = (endTime - startTime) / 1_000_000.0;

        if (booking == null) {
            System.out.println("\n[X] No record found for Confirmation Number: " + confirmationNo);
            System.out.printf("    Search completed in %.4f ms (Hash Search: Miss)%n", elapsedMs);
            return;
        }

        System.out.println("\n[√] RECORD FOUND! (Search Time: " + String.format("%.4f", elapsedMs) + " ms)");
        displayCompleteGuestDetails(booking);
    }

    /**
     * Formats and prints complete guest, room, stay, and billing information.
     */
    private void displayCompleteGuestDetails(Booking booking) {
        Guest guest = booking.getGuest();
        String roomInfo = (booking.getRoomNo() != null) ? booking.getRoomNo() : "Not Assigned Yet";

        System.out.println(DIVIDER);
        System.out.printf("  CONFIRMATION NO : %-20s  STATUS : %s%n", booking.getConfirmationNo(), booking.getStatus());
        System.out.println(SUB_DIVIDER);
        System.out.println(" GUEST IDENTIFICATION DETAILS:");
        System.out.printf("  - Guest ID      : %s%n", guest.getGuestId());
        System.out.printf("  - Full Name     : %s%n", guest.getGuestName());
        System.out.printf("  - Contact No    : %s%n", guest.getContactNumber());
        System.out.printf("  - IC / Passport : %s%n", guest.getIcPassport());
        System.out.println(SUB_DIVIDER);
        System.out.println(" ROOM & STAY INFORMATION:");
        System.out.printf("  - Room Type     : %s%n", booking.getRoomType());
        System.out.printf("  - Assigned Room : %s%n", roomInfo);
        System.out.printf("  - Guests Count  : %d person(s)%n", booking.getNumGuests());
        System.out.printf("  - Check-In Time : %s%n", booking.getCheckInDateTime().format(DATETIME_FORMAT));
        System.out.printf("  - Check-Out Time: %s%n", booking.getCheckOutDateTime().format(DATETIME_FORMAT));
        System.out.printf("  - Duration      : %d night(s)%n", booking.getNights());
        System.out.println(SUB_DIVIDER);
        System.out.println(" BILLING SUMMARY:");
        System.out.printf("  - Rate per Night: RM %.2f%n", booking.getPricePerNight());
        System.out.printf("  - Total Amount  : RM %.2f%n", booking.getTotalPrice());
        System.out.println(DIVIDER);
    }

    /**
     * Allows user to manually insert a new booking entry with an 8-digit
     * confirmation number into the Hash Table.
     */
    private void addNewBookingRecord() {
        System.out.println("\n" + SUB_DIVIDER);
        System.out.println(" ADD NEW RECORD TO HASH TABLE");
        System.out.println(SUB_DIVIDER);

        String confirmationNo;
        while (true) {
            System.out.print("Enter Unique 8-Digit Confirmation Number: ");
            confirmationNo = scanner.nextLine().trim();
            if (!confirmationNo.matches("\\d{8}")) {
                System.out.println("Invalid format. Must be exactly 8 numeric digits.");
                continue;
            }
            if (control.containsConfirmationNo(confirmationNo)) {
                System.out.println("Confirmation number " + confirmationNo + " already exists in the Hash Table!");
                return;
            }
            break;
        }

        System.out.print("Guest Full Name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty())
            name = "Valued Guest";

        System.out.print("Contact Number: ");
        String contact = scanner.nextLine().trim();
        if (contact.isEmpty())
            contact = "0100000000";

        System.out.print("IC / Passport Number: ");
        String icPassport = scanner.nextLine().trim();
        if (icPassport.isEmpty())
            icPassport = "N/A";

        System.out.println("Select Room Type: 1. Standard (RM150) | 2. Deluxe (RM250) | 3. Suite (RM400)");
        System.out.print("Choice: ");
        String typeChoice = scanner.nextLine().trim();
        String roomType = switch (typeChoice) {
            case "2" -> "Deluxe";
            case "3" -> "Suite";
            default -> "Standard";
        };
        double price = switch (roomType) {
            case "Deluxe" -> 250.00;
            case "Suite" -> 400.00;
            default -> 150.00;
        };

        Guest guest = new Guest("G" + (System.currentTimeMillis() % 10000), name, contact, icPassport);
        Booking booking = new Booking(
                confirmationNo, guest, roomType, price, 1,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), LocalDateTime.now());
        booking.setStatus(Booking.STATUS_CONFIRMED);

        control.addBooking(booking);
        System.out.println("\n[√] Booking record successfully added to Hash Table ADT!");
        System.out.println("    Confirmation No: " + confirmationNo + " | Key indexed for O(1) search.");
    }

    /**
     * Displays summary of the underlying ADT and search algorithm.
     */
    private void showAdtSummary() {
        System.out.println("\n" + DIVIDER);
        System.out.println(" ADT & SEARCH ALGORITHM TECHNICAL SUMMARY");
        System.out.println(DIVIDER);
        System.out.println(" ADT Implemented     : Custom Non-Linear Hash Table (adt.MyHashMap)");
        System.out.println(" Collision Handler   : Separate Chaining (LinkedList buckets)");
        System.out.println(" Rehash Mechanism    : Dynamic Resizing (Load Factor Threshold = 0.75)");
        System.out.println(" Search Algorithm    : Hash Search (Key = 8-Digit Confirmation Number)");
        System.out.println(" Time Complexity     : O(1) Average Lookup / O(1) Average Insertion");
        System.out.println(" Total Records Count : " + control.getTotalIndexedCount() + " indexed booking(s)");
        System.out.println(DIVIDER);
    }
}
