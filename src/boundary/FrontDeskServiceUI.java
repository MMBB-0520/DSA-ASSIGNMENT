package boundary;

import control.FrontDeskServiceControl;
import entity.Bill;
import entity.Booking;
import entity.Guest;
import entity.Room;
import adt.ListInterface;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Scanner;
import util.InputUtil;

/**
 * Boundary UI class for Front-Desk Search & Service module.
 * Provides interactive terminal interfaces for:
 * 1. Instant Guest & Booking Search by Confirmation Number
 * 2. Search & Filter Available Rooms (Iterate -> Filter -> Sort pipeline)
 * 3. Search Bill & Process Cash Payment
 *
 * @author Ng Yuen Qi
 */
public class FrontDeskServiceUI {

    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String DIVIDER = "═════════════════════════════════════════════════════════════════════════════════";
    private static final String SUB_DIVIDER = "─────────────────────────────────────────────────────────────────────────────────";

    private final FrontDeskServiceControl control;
    private final Scanner scanner;

    public FrontDeskServiceUI() {
        this.control = new FrontDeskServiceControl();
        this.scanner = new Scanner(System.in);
    }

    public void runMenu() {
        int choice;
        do {
            control.reloadData();
            System.out.println("\n" + DIVIDER);
            System.out.println(" FRONT-DESK SERVICE & INQUIRY");
            System.out.println(DIVIDER);
            System.out.println("[1] Guest Lookup & Check-In");
            System.out.println("[2] Room Availability Search & Filtering");
            System.out.println("[3] Advance Reservation (Future Booking)");
            System.out.println("[4] Check-Out Processing");
            System.out.println("[5] Cancel Booking & Refund Policy");
            System.out.println("[6] Pre-Request Late Check-Out");
            System.out.println("[7] Management Reports & Operational Analytics");
            System.out.println("[0] Return to Main Menu");
            System.out.println(DIVIDER);
            System.out.print("Enter Choice: ");

            choice = InputUtil.readIntInRange(scanner, "Enter Choice [0-7]: ", 0, 7);

            switch (choice) {
                case 1 -> searchByConfirmationNo();
                case 2 -> searchAvailableRoomsUI();
                case 3 -> processFutureReservationUI();
                case 4 -> searchBillAndCheckOutUI();
                case 5 -> cancelBookingUI();
                case 6 -> preRequestLateCheckOutUI();
                case 7 -> showReportsMenu();
                case 0 -> System.out.println("\nReturning to Main Menu...");
                default -> System.out.println("\nInvalid choice! Please enter a valid menu option.");
            }
        } while (choice != 0);
    }

    /**
     * Instantly retrieves complete guest information using BST Search by 8-digit
     * confirmation number.
     */
    private void searchByConfirmationNo() {
        System.out.println("\n" + SUB_DIVIDER);
        System.out.println(" INSTANT GUEST LOOKUP (BST SEARCH)");
        System.out.println(SUB_DIVIDER);
        String confirmationNo = InputUtil.readConfirmationNo(scanner,
                "Enter 8-Digit Confirmation Number (e.g. 84920183): ");

        long startTime = System.nanoTime();
        Booking booking = control.searchByConfirmationNo(confirmationNo);
        long endTime = System.nanoTime();
        double elapsedMs = (endTime - startTime) / 1_000_000.0;

        if (booking == null) {
            System.out.println("\n[X] No record found for Confirmation Number: " + confirmationNo);
            System.out.printf("    Search completed in %.4f ms (BST Search: Miss)%n", elapsedMs);
            return;
        }

        System.out.println("\n[√] RECORD FOUND! (Search Time: " + String.format("%.4f", elapsedMs) + " ms)");
        displayCompleteGuestDetails(booking);

        // Handle actions based on current booking status
        if (Booking.STATUS_CHECKED_IN.equalsIgnoreCase(booking.getStatus())) {
            System.out.println("\n[i] Notice: This guest is currently CHECKED-IN.");
            int currentLateOpt = control.determineLateCheckOutOption(booking);
            if (currentLateOpt > 0) {
                System.out.println("    Active Late Check-Out Option: Tier " + currentLateOpt);
            }

            boolean ans = InputUtil.readYesNo(scanner,
                    "\nDo you want to PRE-REQUEST a Late Check-Out for this guest? (Y/N): ");
            if (ans) {
                promptPreRequestLateCheckOut(booking);
            }
            return;
        }
        if (Booking.STATUS_COMPLETED.equalsIgnoreCase(booking.getStatus())) {
            System.out.println(
                    "\n[i] Notice: Booking status is " + booking.getStatus() + ". No further action required.");
            System.out.print("Press any key to continue...");
            scanner.nextLine();
            return;
        }
        if (Booking.STATUS_CONFIRMED.equalsIgnoreCase(booking.getStatus())) {
            boolean answer = InputUtil.readYesNo(scanner,
                    "\nDo you want to process Check-In and collect deposit for this guest now? (Y/N): ");
            if (!answer) {
                System.out.println("Check-In skipped. Returning to menu.");
                return;
            }
            processCheckInFlow(booking);
        } else {
            System.out.println("\n[i] Notice: Booking status is " + booking.getStatus() + ". No action required.");
            System.out.print("Press any key to continue...");
            scanner.nextLine();
        }
    }

    /**
     * Handles pre-requesting Late Check-Out duration selection and pre-charging
     * late fee.
     */
    private void promptPreRequestLateCheckOut(Booking booking) {
        double pricePerNight = booking.getPricePerNight();
        System.out.println("\n" + SUB_DIVIDER);
        System.out.println(" LATE CHECK-OUT DURATION SELECTION (PRE-REQUEST)");
        System.out.println(SUB_DIVIDER);
        System.out.println(
                " [1] 30 mins (until 12:30) : +25% Price/Night (+RM " + String.format("%.2f", pricePerNight * 0.25)
                        + ")");
        System.out.println(
                " [2] 1 hr (until 13:00)    : +50% Price/Night (+RM " + String.format("%.2f", pricePerNight * 0.50)
                        + ")");
        System.out.println(" [3] 2 hrs (until 14:00)   : +100% Price/Night (+RM "
                + String.format("%.2f", pricePerNight * 1.00) + ")");
        System.out.println(" [0] Cancel / Reset");
        System.out.println(SUB_DIVIDER);

        int opt = InputUtil.readIntInRange(scanner, "Select Requested Late Check-Out Option [0-3]: ", 0, 3);
        double lateFee = booking.getBill().calculateLateCheckOutCharge(opt);
        double cashPaidForLate = 0.0;

        if (opt > 0) {
            System.out.printf("Pre-Requested Late Fee: RM %.2f%n", lateFee);
            boolean payAns = InputUtil.readYesNo(scanner,
                    "Collect payment for pre-requested late fee now? (Y/N): ");
            if (payAns) {
                double cash = InputUtil.readDoubleMin(scanner,
                        String.format("Enter Amount Received (Min RM %.2f): ", lateFee), lateFee);
                cashPaidForLate = lateFee;
                System.out.printf("Change Due: RM %.2f%n", cash - lateFee);
            }
        }
        boolean ok = control.requestLateCheckOut(booking.getConfirmationNo(), opt, cashPaidForLate);
        if (ok) {
            System.out.println("\n[√] SUCCESS: Late Check-Out pre-request recorded!");
        } else {
            System.out.println("\n[!] Error saving pre-requested late check-out.");
        }
    }

    /**
     * Dedicated UI entry for pre-requesting Late Check-Out options for a checked-in
     * guest.
     */
    private void preRequestLateCheckOutUI() {
        System.out.println("\n" + SUB_DIVIDER);
        System.out.println(" PRE-REQUEST LATE CHECK-OUT");
        System.out.println(SUB_DIVIDER);
        String confirmationNo = InputUtil.readConfirmationNo(scanner,
                "Enter 8-Digit Confirmation Number (e.g. 84920183): ");

        Booking booking = control.searchByConfirmationNo(confirmationNo);
        if (booking == null) {
            System.out.println("\n[X] No record found for Confirmation Number: " + confirmationNo);
            return;
        }

        if (!Booking.STATUS_CHECKED_IN.equalsIgnoreCase(booking.getStatus())) {
            System.out.println(
                    "\n[!] Notice: Late Check-Out can only be pre-requested for guests who are currently CHECKED_IN.");
            System.out.println("    Current Booking Status: " + booking.getStatus());
            return;
        }

        promptPreRequestLateCheckOut(booking);
    }

    /**
     * Dedicated UI entry for cancelling a booking with 24-hour refund/forfeit
     * evaluation.
     */
    private void cancelBookingUI() {
        System.out.println("\n" + SUB_DIVIDER);
        System.out.println(" CANCEL BOOKING & REFUND PROCESSING (24-HOUR CANCELLATION POLICY)");
        System.out.println(SUB_DIVIDER);
        String confirmationNo = InputUtil.readConfirmationNo(scanner,
                "Enter 8-Digit Confirmation Number to Cancel (e.g. 84920183): ");

        Booking booking = control.searchByConfirmationNo(confirmationNo);
        if (booking == null) {
            System.out.println("\n[X] No record found for Confirmation Number: " + confirmationNo);
            return;
        }

        displayCompleteGuestDetails(booking);

        if (Booking.STATUS_CANCELLED.equalsIgnoreCase(booking.getStatus())
                || Booking.STATUS_CHECKED_IN.equalsIgnoreCase(booking.getStatus())
                || Booking.STATUS_COMPLETED.equalsIgnoreCase(booking.getStatus())) {
            System.out.println(
                    "\n[!] Notice: Booking status is '" + booking.getStatus() + "'. Cannot cancel this booking.");
            return;
        }

        boolean confirm = InputUtil.readYesNo(scanner,
                "\nAre you sure you want to CANCEL this booking? (Y/N): ");
        if (!confirm) {
            System.out.println("Cancellation process aborted.");
            return;
        }

        String resultMsg = control.cancelBooking(confirmationNo);
        if (resultMsg.startsWith("SUCCESS_REFUND:")) {
            System.out.println("\n [√] CANCELLATION COMPLETED - FULL REFUND GRANTED");
            System.out.println("     " + resultMsg.substring("SUCCESS_REFUND:".length()));
        } else if (resultMsg.startsWith("SUCCESS_FORFEIT:")) {
            System.out.println("\n [!] CANCELLATION COMPLETED - DEPOSIT FORFEITED (< 24 hrs rule)");
            System.out.println("     " + resultMsg.substring("SUCCESS_FORFEIT:".length()));
        } else {
            System.out.println(" " + resultMsg);
        }
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }

    /**
     * Formats and prints complete guest identification, room details, dates,
     * invoice, and payment status.
     */
    private void displayCompleteGuestDetails(Booking booking) {
        Guest guest = booking.getGuest();
        String roomInfo = (booking.getRoomNo() != null) ? booking.getRoomNo() : "Not Assigned Yet";
        Bill bill = booking.getBill();
        System.out.println(DIVIDER);
        System.out.printf("  CONFIRMATION NO : %-18s INVOICE NO : %s%n", booking.getConfirmationNo(),
                bill.getInvoiceNo());
        System.out.printf("  BOOKING STATUS  : %-18s PAYMENT    : %s%n", booking.getStatus(),
                bill.getPaymentStatus());
        System.out.println(SUB_DIVIDER);
        System.out.println(" GUEST IDENTIFICATION DETAILS:");
        System.out.printf("  - Guest ID      : %s%n", guest.getGuestId());
        System.out.printf("  - Full Name     : %s%n", guest.getGuestName());
        System.out.printf("  - Contact No    : %s%n", guest.getContactNumber());
        System.out.printf("  - IC / Passport : %s%n", guest.getIcPassport());
        System.out.println(SUB_DIVIDER);
        System.out.println(" ROOM & STAY INFORMATION:");
        System.out.printf("  - Room Type     : %s%n", booking.getRoomType());
        System.out.printf("  - Number of Rooms: %d room(s)%n", booking.getNumOfRooms());
        System.out.printf("  - Assigned Room : %s%n", roomInfo);
        System.out.printf("  - Guests Count  : %d person(s)%n", booking.getNumGuests());
        System.out.printf("  - Check-In Time : %s%n", booking.getCheckInDateTime().format(DATETIME_FORMAT));
        System.out.printf("  - Check-Out Time: %s%n", booking.getCheckOutDateTime().format(DATETIME_FORMAT));
        System.out.printf("  - Duration      : %d night(s)%n", booking.getNights());
        System.out.println(SUB_DIVIDER);
        System.out.println(" BILLING SUMMARY:");
        System.out.printf("  - Room Charge   : RM %8.2f  (%d room(s) @ RM %.2f / night)%n",
                bill.getRoomCharge(), booking.getNumOfRooms(), booking.getPricePerNight());
        System.out.printf("  - Service Charge: RM %8.2f  (10%%)%n", bill.getServiceCharge());
        System.out.printf("  - Other Charges : RM %8.2f  (Tourism & Taxes)%n", bill.getOtherCharges());
        System.out.printf("  - Total Amount  : RM %8.2f%n", bill.getGrandTotal());
        System.out.println(DIVIDER);
    }

    /**
     * Executes Room Availability Search, Filtering, and Sorting Pipeline.
     * Displays Real-Time Availability Summary breakdown and supports instant
     * Walk-In / Future Bookings.
     */
    private void searchAvailableRoomsUI() {
        int[] stdCounts = control.getRoomTypeCounts("Standard");
        int[] dlxCounts = control.getRoomTypeCounts("Deluxe");
        int[] steCounts = control.getRoomTypeCounts("Suite");

        double[] stdPrice = control.getRoomTypePriceRange("Standard");
        double[] dlxPrice = control.getRoomTypePriceRange("Deluxe");
        double[] stePrice = control.getRoomTypePriceRange("Suite");
        double[] overallPrice = control.getOverallPriceRange();

        String stdPriceStr = (stdPrice[0] == stdPrice[1]) ? String.format("RM %.2f", stdPrice[0])
                : String.format("RM %.2f - RM %.2f", stdPrice[0], stdPrice[1]);
        String dlxPriceStr = (dlxPrice[0] == dlxPrice[1]) ? String.format("RM %.2f", dlxPrice[0])
                : String.format("RM %.2f - RM %.2f", dlxPrice[0], dlxPrice[1]);
        String stePriceStr = (stePrice[0] == stePrice[1]) ? String.format("RM %.2f", stePrice[0])
                : String.format("RM %.2f - RM %.2f", stePrice[0], stePrice[1]);

        System.out.println("\n" + DIVIDER);
        System.out.println(" TODAY'S ROOM AVAILABILITY OVERVIEW & PRICE RANGES");
        System.out.println(DIVIDER);
        System.out.printf("  - Standard : %d / %d available  (%s / night)%n", stdCounts[0], stdCounts[1], stdPriceStr);
        System.out.printf("  - Deluxe   : %d / %d available  (%s / night)%n", dlxCounts[0], dlxCounts[1], dlxPriceStr);
        System.out.printf("  - Suite    : %d / %d available  (%s / night)%n", steCounts[0], steCounts[1], stePriceStr);
        System.out.printf("\n* Overall Price Range: RM %.2f - RM %.2f / night%n", overallPrice[0], overallPrice[1]);
        System.out.println(DIVIDER);

        System.out.println("\nSelect Room Type Filter:");
        System.out.println(" 1. Standard");
        System.out.println(" 2. Deluxe");
        System.out.println(" 3. Suite");
        System.out.println(" 0. All Room Types");
        int typeChoiceOpt = InputUtil.readIntInRange(scanner, "Enter Choice [0-3]: ", 0, 3);

        String roomType = switch (typeChoiceOpt) {
            case 1 -> "Standard";
            case 2 -> "Deluxe";
            case 3 -> "Suite";
            default -> "";
        };

        System.out.println("\nSelect Sorting Option:");
        int sortOpt = InputUtil.readIntInRangeWithDefault(scanner,
                "1. By Room Number | 2. By Price per Night: ", 1, 2, 1);
        String sortBy = (sortOpt == 2) ? "Price" : "RoomNo";

        Room[] results = control.searchAvailableRooms(roomType, sortBy);

        System.out.println("\n" + SUB_DIVIDER);
        System.out.printf(" AVAILABLE ROOMS RESULT (%d Match(es) Found)%n", results.length);
        System.out.println(SUB_DIVIDER);
        System.out.printf(" %-10s %-15s %-20s %-15s%n", "Room No", "Room Type", "Price/Night (RM)", "Status");
        System.out.println(SUB_DIVIDER);

        if (results.length == 0) {
            System.out.println(" [X] No available rooms matching the specified filter criteria.");
        } else {
            for (Room r : results) {
                System.out.printf(" %-10s %-15s RM %-17.2f %-15s%n", r.getRoomNo(), r.getRoomType(),
                        r.getPricePerNight(), r.getStatus());
            }
        }
        System.out.println(SUB_DIVIDER);

        boolean proceedWalkIn = InputUtil.readYesNo(scanner,
                "\nWould you like to proceed with Walk-In Registration for an available room now? (Y/N): ");
        if (proceedWalkIn) {
            System.out.println("\nRedirecting to Walk-In Registration Module...");
            WalkInRegistrationUI walkInUI = new WalkInRegistrationUI();
            walkInUI.displayMenu();
            control.reloadData();
        }
    }

    /**
     * Handles Advance Booking for future dates, guest lookup by contact number, and
     * 30% deposit collection.
     */
    private void processFutureReservationUI() {
        System.out.println("\n" + SUB_DIVIDER);
        System.out.println(" ADVANCE RESERVATION (FUTURE BOOKING)");
        System.out.println(SUB_DIVIDER);

        LocalDate checkInDate = InputUtil.readFutureDate(scanner,
                "Enter Future Check-In Date (yyyy-MM-dd): ");
        LocalTime arrivalTime = InputUtil.readTimeMinWithDefault(scanner,
                "Enter Estimated Arrival Time (HH:mm) [Press Enter for Standard Check-In Time (13:00)]: ",
                LocalTime.of(13, 0), LocalTime.of(13, 0));
        LocalDateTime checkIn = LocalDateTime.of(checkInDate, arrivalTime);
        int nights = InputUtil.readIntInRangeWithDefault(scanner,
                "Enter Number of Nights Staying (1-30) [Default 1]: ", 1, 30, 1);
        LocalDateTime checkOut = checkIn.plusDays(nights).withHour(12).withMinute(0);

        int stdAvail = control.getAvailableRoomCountForPeriod("Standard", checkIn, checkOut);
        int dlxAvail = control.getAvailableRoomCountForPeriod("Deluxe", checkIn, checkOut);
        int steAvail = control.getAvailableRoomCountForPeriod("Suite", checkIn, checkOut);

        double[] stdPrice = control.getRoomTypePriceRange("Standard");
        double[] dlxPrice = control.getRoomTypePriceRange("Deluxe");
        double[] stePrice = control.getRoomTypePriceRange("Suite");

        String stdPriceStr = (stdPrice[0] == stdPrice[1]) ? String.format("RM %.2f", stdPrice[0])
                : String.format("RM %.2f - RM %.2f", stdPrice[0], stdPrice[1]);
        String dlxPriceStr = (dlxPrice[0] == dlxPrice[1]) ? String.format("RM %.2f", dlxPrice[0])
                : String.format("RM %.2f - RM %.2f", dlxPrice[0], dlxPrice[1]);
        String stePriceStr = (stePrice[0] == stePrice[1]) ? String.format("RM %.2f", stePrice[0])
                : String.format("RM %.2f - RM %.2f", stePrice[0], stePrice[1]);

        System.out.println("\n" + SUB_DIVIDER);
        System.out.printf(" AVAILABILITY FOR DATES: %s to %s%n", checkIn.format(DATETIME_FORMAT),
                checkOut.format(DATETIME_FORMAT));
        System.out.println(SUB_DIVIDER);
        System.out.printf(" 1. Standard : %d room(s) available (%s / night)%n", stdAvail, stdPriceStr);
        System.out.printf(" 2. Deluxe   : %d room(s) available (%s / night)%n", dlxAvail, dlxPriceStr);
        System.out.printf(" 3. Suite    : %d room(s) available (%s / night)%n", steAvail, stePriceStr);
        System.out.println(SUB_DIVIDER);

        int typeChoiceOpt = InputUtil.readIntInRange(scanner, "Select Room Type [1-3]: ", 1, 3);

        String roomType = switch (typeChoiceOpt) {
            case 2 -> "Deluxe";
            case 3 -> "Suite";
            default -> "Standard";
        };

        int availableForSelected = control.getAvailableRoomCountForPeriod(roomType, checkIn, checkOut);
        if (availableForSelected <= 0) {
            System.out.printf("%n[X] Error: %s is FULLY BOOKED for the requested dates (%s to %s).%n",
                    roomType, checkIn.format(DATETIME_FORMAT), checkOut.format(DATETIME_FORMAT));
            System.out.println("    Reservation cancelled to prevent overbooking.");
            return;
        }

        int numOfRooms = InputUtil.readIntInRangeWithDefault(scanner,
                String.format("Enter Number of Rooms to Book (1-%d) [Default 1]: ", availableForSelected), 1,
                availableForSelected, 1);

        // Base rate for calculating advance booking deposit
        double[] priceRange = control.getRoomTypePriceRange(roomType);
        double pricePerNight = (priceRange != null && priceRange.length > 0 && priceRange[0] > 0) ? priceRange[0]
                : 150.0;

        int numGuests = InputUtil.readIntInRangeWithDefault(scanner,
                "Enter Number of Guests (1-6) [Default 1]: ", 1, 6, 1);

        // Guest Phone Lookup & Reuse
        String contact = InputUtil.readContactNumber(scanner);

        Guest existingGuest = control.findGuestByContact(contact);
        String name;
        String icPassport;

        if (existingGuest != null) {
            System.out.println("\n[√] Existing Guest Found!");
            System.out.printf("  Guest Name    : %s%n", existingGuest.getGuestName());
            System.out.printf("  IC / Passport : %s%n", existingGuest.getIcPassport());
            name = existingGuest.getGuestName();
            icPassport = existingGuest.getIcPassport();
        } else {
            System.out.println("\nNew Guest Registration:");
            name = InputUtil.readGuestName(scanner);
            icPassport = InputUtil.readIcPassport(scanner);
        }

        double roomChargeTotal = pricePerNight * nights * numOfRooms;
        double depositRequired = roomChargeTotal * 0.30;

        System.out.println("\n" + SUB_DIVIDER);
        System.out.println(" RESERVATION SUMMARY & 30% DEPOSIT DUE");
        System.out.println(SUB_DIVIDER);
        System.out.printf(" Room Type         : %s (Base Rate: RM %.2f / night)%n", roomType, pricePerNight);
        System.out.printf(" Number of Rooms   : %d room(s)%n", numOfRooms);
        System.out.printf(" Duration          : %s to %s (%d Night(s))%n", checkIn.format(DATETIME_FORMAT),
                checkOut.format(DATETIME_FORMAT), nights);
        System.out.printf(" %-65s RM %10.2f%n",
                String.format("Est. Room Charge (%d room(s) @ RM %.2f/night)", numOfRooms, pricePerNight),
                roomChargeTotal);
        System.out.printf(" %-65s RM %10.2f%n", "30% Deposit Due", depositRequired);
        System.out.println(SUB_DIVIDER);

        double cashTendered = InputUtil.readDoubleMin(scanner,
                String.format("Enter Deposit Amount Received (Min RM %.2f): ", depositRequired), depositRequired);

        Booking booking = control.registerFutureBooking(name, contact, icPassport, roomType, pricePerNight, numGuests,
                numOfRooms, checkIn, checkOut, depositRequired);
        double change = cashTendered - depositRequired;

        System.out.println("\n" + DIVIDER);
        System.out.println(" ADVANCE RESERVATION SUCCESSFUL!");
        System.out.println(DIVIDER);
        System.out.printf("  Confirmation No   : %s%n", booking.getConfirmationNo());
        System.out.printf("  Booking Status    : %s%n", booking.getStatus());
        System.out.printf("  Rooms Booked      : %d room(s) (%s)%n", numOfRooms, roomType);
        System.out.printf(" %-65s RM %10.2f%n", "30% Deposit Due", depositRequired);
        System.out.printf(" %-65s RM %10.2f%n", "Amount Received", cashTendered);
        System.out.printf(" %-65s RM %10.2f%n", "Change Due", change);
        System.out.println(DIVIDER);
    }

    /**
     * Searches booking by confirmation number, calculates Late Check-Out Charges
     * (options 0, 1, 2, 3),
     * deducts pre-collected deposits (30% Booking Deposit + RM200 Security
     * Deposit),
     * handles net balance / refund, and marks booking as COMPLETED and room as
     * DIRTY.
     */
    private void searchBillAndCheckOutUI() {
        System.out.println("\n" + SUB_DIVIDER);
        System.out.println(" SEARCH BILL & PROCESS CHECK-OUT");
        System.out.println(SUB_DIVIDER);
        String confirmationNo = InputUtil.readConfirmationNo(scanner, "Enter 8-Digit Confirmation Number: ");

        Booking booking = control.searchByConfirmationNo(confirmationNo);
        if (booking == null) {
            System.out.println("\n[X] Booking record not found for Confirmation Number: " + confirmationNo);
            return;
        }

        // Status validation
        if (Booking.STATUS_COMPLETED.equalsIgnoreCase(booking.getStatus())) {
            System.out.println("\n[!] Notice: This booking is already CHECKED-OUT.");
            return;
        }
        if (Booking.STATUS_CANCELLED.equalsIgnoreCase(booking.getStatus())) {
            System.out.println("\n[!] Notice: Cannot check-out a CANCELLED booking.");
            return;
        }
        if (Booking.STATUS_NO_SHOW.equalsIgnoreCase(booking.getStatus())) {
            System.out.println("\n[!] Notice: Cannot check-out a NO-SHOW booking (Status: NO_SHOW).");
            return;
        }
        if (Booking.STATUS_PENDING.equalsIgnoreCase(booking.getStatus())) {
            System.out.println("\n[!] Notice: Guest has not checked in yet.");
            return;
        }

        Guest guest = booking.getGuest();
        String roomInfo = (booking.getRoomNo() != null) ? booking.getRoomNo() : "Unassigned";
        Bill bill = booking.getBill();
        int lateOption = control.determineLateCheckOutOption(booking);
        double roomCharge = bill.getRoomCharge();
        double serviceCharge = bill.getServiceCharge();
        double lateCharge = bill.calculateLateCheckOutCharge(lateOption);
        double otherCharges = bill.getOtherCharges();

        double subtotalBill = bill.getSubtotalBill(lateCharge);
        double netDue = bill.getNetDue(lateCharge);

        String lateOptLabel = switch (lateOption) {
            case 1 -> "30 mins (until 12:30) (+25% Auto)";
            case 2 -> "1 hr (until 13:00) (+50% Auto)";
            case 3 -> "2 hrs (until 14:00) (+100% Auto)";
            default -> "Grace Period / On Time (Free)";
        };

        if (bill.getRequestedLateOption() > 0) {
            lateOptLabel = switch (bill.getRequestedLateOption()) {
                case 1 -> "30 mins (until 12:30) (+25% Pre-Requested)";
                case 2 -> "1 hr (until 13:00) (+50% Pre-Requested)";
                case 3 -> "2 hrs (until 14:00) (+100% Pre-Requested)";
                default -> lateOptLabel;
            };
        }

        System.out.println("\n" + DIVIDER);
        System.out.println("              HOTEL CHECK-OUT STATEMENT & DEPOSIT SETTLEMENT");
        System.out.println("                         Invoice: " + bill.getInvoiceNo());
        System.out.println(DIVIDER);

        System.out.println(" GUEST & STAY INFORMATION");
        System.out.println(SUB_DIVIDER);
        System.out.printf(" Confirmation No : %s%n", booking.getConfirmationNo());
        System.out.printf(" Guest Name      : %s%n",
                (guest != null ? guest.getGuestName() : "N/A"));
        System.out.printf(" Contact Number  : %s%n",
                (guest != null ? guest.getContactNumber() : "N/A"));
        System.out.printf(" Room Details    : %s (Room %s)%n",
                booking.getRoomType(), roomInfo);
        System.out.printf(" Check-In Date   : %s%n",
                booking.getCheckInDateTime().format(DATETIME_FORMAT));
        System.out.printf(" Check-Out Date  : %s (%d Night(s))%n",
                booking.getCheckOutDateTime().format(DATETIME_FORMAT),
                booking.getNights());

        System.out.println(SUB_DIVIDER);
        System.out.println(" CHARGES");
        System.out.println(SUB_DIVIDER);

        System.out.printf(" %-65s RM %10.2f%n",
                "Room Charge", roomCharge);
        System.out.printf(" %-65s RM %10.2f%n",
                "Service Charge (10%)", serviceCharge);
        System.out.printf(" %-65s RM %10.2f%n",
                "Late Check-out", lateCharge);
        System.out.printf(" %-65s RM %10.2f%n",
                "Other Charges", otherCharges);

        System.out.println(SUB_DIVIDER);

        System.out.printf(" %-65s RM %10.2f%n",
                "SUBTOTAL BILL", subtotalBill);

        System.out.println(SUB_DIVIDER);
        System.out.println(" DEPOSIT SETTLEMENT");
        System.out.println(SUB_DIVIDER);

        System.out.printf(" %-65s RM %10.2f%n",
                "Less: 30% Booking Deposit", -bill.getBookingDeposit());
        System.out.printf(" %-65s RM %10.2f%n",
                "Less: Security Deposit", -bill.getSecurityDeposit());

        System.out.println(SUB_DIVIDER);

        if (netDue > 0) {
            System.out.printf(" %-65s RM %10.2f%n",
                    "NET BALANCE DUE", netDue);
            System.out.println(" Customer to Pay");
        } else if (netDue < 0) {
            System.out.printf(" %-65s RM %10.2f%n",
                    "NET REFUND DUE", Math.abs(netDue));
            System.out.println(" To Refund to Customer");
        } else {
            System.out.printf(" %-65s RM %10.2f%n",
                    "NET BALANCE DUE", 0.0);
            System.out.println(" Fully Settled by Deposit");
        }

        System.out.println(DIVIDER);

        boolean proceed = InputUtil.readYesNo(scanner,
                "\nProceed to complete Check-Out & final settlement? (Y/N): ");
        if (!proceed) {
            System.out.println("Check-Out processing cancelled.");
            return;
        }

        double cashTendered = 0.0;
        if (netDue > 0) {
            cashTendered = InputUtil.readDoubleMin(scanner,
                    String.format("Enter Payment Amount Received (Min RM %.2f): ", netDue), netDue);
        }

        boolean success = control.processCheckOut(confirmationNo, cashTendered);
        if (success) {
            System.out.println("\n" + SUB_DIVIDER);
            System.out.println(" CHECK-OUT COMPLETED SUCCESSFULLY!");
            System.out.println(SUB_DIVIDER);
            System.out.printf("  Confirmation No   : %s%n", booking.getConfirmationNo());
            System.out.printf("  New Booking Status: %s%n", Booking.STATUS_COMPLETED);
            if (netDue > 0) {
                System.out.printf(" %-65s RM %10.2f%n", "Net Payment Recv'd", cashTendered);
                System.out.printf(" %-65s RM %10.2f%n", "Change Returned", cashTendered - netDue);
            } else if (netDue < 0) {
                System.out.printf(" %-65s RM %10.2f%n", "Refund Returned", Math.abs(netDue));
            } else {
                System.out.printf(" %-65s RM %10.2f%n", "Final Settlement", 0.0);
                System.out.println(" Fully Paid & Settled");
            }
            System.out.println(SUB_DIVIDER);
        } else {
            System.out.println("\n[!] Error processing check-out.");
        }
    }

    /**
     * Processes check-in and deposit collection flow for a found booking.
     */
    private void processCheckInFlow(Booking booking) {
        Bill bill = booking.getBill();
        double roomCharge = bill.getRoomCharge();
        double bookingDeposit = bill.getBookingDeposit();
        double securityDeposit = bill.getSecurityDeposit();
        boolean depositAlreadyPaid = bill.isDepositPaid();

        double totalRequiredDeposit = bill.getRequiredCheckInDeposit();

        System.out.println("\n" + DIVIDER);
        System.out.println(" CHECK-IN STATEMENT & DEPOSIT BREAKDOWN");
        System.out.println(DIVIDER);
        System.out.printf(" Room Charge Total    : RM %10.2f (%d Room(s), %d Night(s) @ RM %.2f/night)%n", roomCharge,
                booking.getNumOfRooms(), booking.getNights(), booking.getPricePerNight());

        if (depositAlreadyPaid) {
            System.out.printf(" 30%% Booking Deposit   : RM %10.2f  (ALREADY PAID AT RESERVATION)%n", bookingDeposit);
            System.out.printf(" Security Deposit     : RM %10.2f  (DUE NOW - Refundable upon Check-out)%n",
                    securityDeposit);
            System.out.println(SUB_DIVIDER);
            System.out.printf(" TOTAL DUE AT CHECK-IN: RM %10.2f  (Security Deposit Only)%n", totalRequiredDeposit);
        } else {
            System.out.printf(" 30%% Booking Deposit   : RM %10.2f  (DUE NOW)%n", bookingDeposit);
            System.out.printf(" Security Deposit     : RM %10.2f  (DUE NOW - Refundable upon Check-out)%n",
                    securityDeposit);
            System.out.println(SUB_DIVIDER);
            System.out.printf(" TOTAL DUE AT CHECK-IN: RM %10.2f  (Booking Dep + Security Dep)%n",
                    totalRequiredDeposit);
        }
        System.out.println(DIVIDER);

        double cashTendered = InputUtil.readDoubleMin(scanner,
                String.format("Enter Deposit Amount Received for Check-In (Min RM %.2f): ", totalRequiredDeposit),
                totalRequiredDeposit);
        double change = cashTendered - totalRequiredDeposit;
        boolean success = control.processCheckIn(booking.getConfirmationNo(), cashTendered);

        if (success) {
            System.out.println("\n" + SUB_DIVIDER);
            System.out.println(" CHECK-IN SUCCESSFUL!");
            System.out.println(SUB_DIVIDER);
            System.out.printf("  Confirmation No   : %s%n", booking.getConfirmationNo());
            System.out.printf("  New Booking Status: %s%n", Booking.STATUS_CHECKED_IN);
            System.out.printf(" %-65s RM %10.2f%n", "Deposit Received Now", cashTendered);
            System.out.printf(" %-65s RM %10.2f%n", "Change Returned", change);
            System.out.println(SUB_DIVIDER);
        } else {
            System.out.println("\n[!] Check-in processing error.");
        }
    }

    /**
     * Displays technical ADT specs and generates a comprehensive Summary Analysis
     * Report
     * using BST getIterator(), getMin(), getMax(), and category aggregations.
     */
    private void showAdtSummary() {
        System.out.println("\n=================================================================================");
        System.out.println("                 TECHNICAL ADT & ALGORITHM PERFORMANCE SUMMARY");
        System.out.println("=================================================================================");

        // --------------------------------------------------------------------
        // PART 1: TECHNICAL ARCHITECTURE (The ADT Specs)
        // --------------------------------------------------------------------
        System.out.println(" [ PART 1: ABSTRACT DATA TYPE (ADT) ARCHITECTURE ]");
        System.out.println(" -------------------------------------------------------------------------------");

        System.out.println(" > LINEAR ADT STRUCTURE");
        System.out.println("   - Implementation : adt.CustomLinkedList<Room> (with Tail Pointer)");
        System.out.println("   - Purpose        : Dynamic candidate filtering & pipeline processing");
        System.out.println("   - Algorithm      : O(1) Insertion, O(1) In-place Sort on Pointers");
        System.out.println();

        System.out.println(" > NON-LINEAR ADT STRUCTURE");
        System.out.println("   - Implementation : adt.MyBinarySearchTree<Booking> (via BSTInterface)");
        System.out.println("   - Node Structure : Internal Node (data, left, right)");
        System.out.println("   - Search Key     : 8-Digit Confirmation Number");
        System.out.println("   - Complexity     : O(log n) Average Lookup & Insertion");
        System.out.println();

        System.out.println(" > BST SEARCH INDEX METRICS");
        System.out.printf("   - Total Indexed  : %d node(s)%n", control.getTotalIndexedCount());
        System.out.printf("   - Tree Height    : %d level(s) [Divide-and-Conquer Rebalanced]%n",
                control.getTreeHeight());

        Booking minBooking = control.getMinBooking();
        String minDesc = (minBooking != null)
                ? String.format("%s (%s - RM %.2f)", minBooking.getConfirmationNo(),
                        minBooking.getGuest() != null ? minBooking.getGuest().getGuestName() : "N/A",
                        minBooking.getBill().getGrandTotal())
                : "N/A";
        System.out.println("   - Min Node (Left): " + minDesc);

        Booking maxBooking = control.getMaxBooking();
        String maxDesc = (maxBooking != null)
                ? String.format("%s (%s - RM %.2f)", maxBooking.getConfirmationNo(),
                        maxBooking.getGuest() != null ? maxBooking.getGuest().getGuestName() : "N/A",
                        maxBooking.getBill().getGrandTotal())
                : "N/A";
        System.out.println("   - Max Node(Right): " + maxDesc);

        // --------------------------------------------------------------------
        // PART 2: BUSINESS ANALYTICS (The Aggregated Data)
        // --------------------------------------------------------------------
        System.out.println("\n [ PART 2: SYSTEM SUMMARY & BUSINESS ANALYTICS ]");
        System.out.println(" -------------------------------------------------------------------------------");

        System.out.println(" > FINANCIAL OVERVIEW");
        System.out.printf("   - Total Bookings Analyzed: %d booking(s)%n", control.getTotalBookingsCount());
        System.out.printf("   - Total System Revenue   : RM %.2f%n", control.getGrandTotalRevenue());
        System.out.printf("   - Average Booking Value  : RM %.2f%n", control.getAverageRevenuePerBooking());
        System.out.printf("   - Average Stay (ALOS)    : %.1f night(s)%n", control.getAverageStayNights());
        System.out.println();

        System.out.println(" > REVENUE BREAKDOWN BY ROOM TYPE");
        System.out.printf("   - Standard Rooms : %3d bookings | Revenue: RM %8.2f%n",
                control.getBookingCountByRoomType("Standard"), control.getRevenueByRoomType("Standard"));
        System.out.printf("   - Deluxe Rooms   : %3d bookings | Revenue: RM %8.2f%n",
                control.getBookingCountByRoomType("Deluxe"), control.getRevenueByRoomType("Deluxe"));
        System.out.printf("   - Suite Rooms    : %3d bookings | Revenue: RM %8.2f%n",
                control.getBookingCountByRoomType("Suite"), control.getRevenueByRoomType("Suite"));
        System.out.println();

        System.out.println(" > OPERATIONAL STATUS DISTRIBUTION");
        System.out.printf("   Confirmed: %-3d | Reserved: %-3d | Pending: %-3d | No-Show: %-3d | Paid: %-3d%n",
                control.getBookingCountByStatus(Booking.STATUS_CONFIRMED),
                control.getBookingCountByStatus(Booking.STATUS_RESERVED),
                control.getBookingCountByStatus(Booking.STATUS_PENDING),
                control.getBookingCountByStatus(Booking.STATUS_NO_SHOW),
                control.getPaidBookingCount());

        System.out.println("=================================================================================");
    }

    private static String generateProgressBar(double percentage, int barLength) {
        if (Double.isNaN(percentage) || percentage < 0)
            percentage = 0;
        if (percentage > 100)
            percentage = 100;
        int filled = (int) Math.round((percentage / 100.0) * barLength);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < barLength; i++) {
            if (i < filled) {
                sb.append("#");
            } else {
                sb.append("─");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Sub-menu for Management Reports & Operational Analytics.
     */
    private void showReportsMenu() {
        int reportChoice;
        do {
            System.out.println("\n" + DIVIDER);
            System.out.println(" MANAGEMENT REPORTS & OPERATIONAL ANALYTICS");
            System.out.println(DIVIDER);
            System.out.println("[1] Report 1: Room Availability & Booking Demand Analysis");
            System.out.println("[2] Report 2: Daily Arrivals, Departures & In-House Action Report");
            System.out.println("[3] Technical ADT & BST Algorithm Performance Summary");
            System.out.println("[0] Return to Front-Desk Menu");
            System.out.println(DIVIDER);

            reportChoice = InputUtil.readIntInRange(scanner, "Enter Choice [0-3]: ", 0, 3);
            switch (reportChoice) {
                case 1 -> generateRoomAvailabilityDemandReportUI();
                case 2 -> generateDailyArrivalsDeparturesActionReportUI();
                case 3 -> showAdtSummary();
                case 0 -> System.out.println("\nReturning to Front-Desk Menu...");
                default -> System.out.println("\nInvalid choice!");
            }
        } while (reportChoice != 0);
    }

    /**
     * Report 1 UI: Room Availability & Booking Demand Analysis
     * Pipeline: Filter -> Search -> Sort -> Analyze -> Management Recommendation
     */
    private void generateRoomAvailabilityDemandReportUI() {
        System.out.println("\n" + SUB_DIVIDER);
        System.out.println(" REPORT 1: ROOM AVAILABILITY & BOOKING DEMAND ANALYSIS");
        System.out.println(SUB_DIVIDER);

        // 1. Filter: Select Room Type
        System.out.println("Select Room Type Filter:");
        System.out.println(" [1] Deluxe");
        System.out.println(" [2] Standard");
        System.out.println(" [3] Suite");
        System.out.println(" [4] ALL Room Types (Default)");
        int roomOpt = InputUtil.readIntInRangeWithDefault(scanner, "Enter Choice [1-4] (Default 4): ", 1, 4, 4);

        String roomTypeFilter = switch (roomOpt) {
            case 1 -> "Deluxe";
            case 2 -> "Standard";
            case 3 -> "Suite";
            default -> "ALL";
        };

        // 2. Select Date Period Range
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime checkInPeriod = now.toLocalDate().atTime(14, 0);
        LocalDateTime checkOutPeriod = checkInPeriod.plusDays(7);

        System.out.println("\nSelected Date Analysis Period:");
        System.out.println(" Default Period: Today (" + checkInPeriod.format(DATETIME_FORMAT) + ") to +7 Days ("
                + checkOutPeriod.format(DATETIME_FORMAT) + ")");
        System.out.print("Customize Period? (Y/N, Default N): ");
        String customAns = scanner.nextLine().trim();
        if (customAns.equalsIgnoreCase("Y") || customAns.equalsIgnoreCase("YES")) {
            checkInPeriod = InputUtil.readFutureDateTime(scanner, "Enter Start Check-In Date/Time (yyyy-MM-dd HH:mm): ",
                    DATETIME_FORMAT);
            checkOutPeriod = InputUtil.readFutureDateTime(scanner, "Enter End Check-Out Date/Time (yyyy-MM-dd HH:mm): ",
                    DATETIME_FORMAT);
        }

        // 3. Status Filter
        System.out.println("\nSelect Room Status Filter:");
        System.out.println(" [1] AVAILABLE Only (Default)");
        System.out.println(" [2] ALL Room Statuses");
        int statusOpt = InputUtil.readIntInRangeWithDefault(scanner, "Enter Choice [1-2] (Default 1): ", 1, 2, 1);
        String statusFilter = (statusOpt == 2) ? "ALL" : Room.STATUS_AVAILABLE;

        // 4. Sort Options
        System.out.println("\nSelect Sort Criteria:");
        System.out.println(" [1] Price per Night (Low to High) [Default]");
        System.out.println(" [2] Price per Night (High to Low)");
        System.out.println(" [3] Room Number (Ascending)");
        int sortOpt = InputUtil.readIntInRangeWithDefault(scanner, "Enter Sort Option [1-3] (Default 1): ", 1, 3, 1);

        String sortBy = switch (sortOpt) {
            case 2 -> "Price_Desc";
            case 3 -> "RoomNo_Asc";
            default -> "Price_Asc";
        };

        ListInterface<Room> candidateRooms = control.generateRoomAvailabilityDemandReport(
                roomTypeFilter, checkInPeriod, checkOutPeriod, statusFilter, sortBy);

        // Ask Control layer to calculate occupancy metrics (No business logic/math in
        // UI)
        double[] occMetrics = control.calculatePeriodOccupancyMetrics(checkInPeriod, checkOutPeriod);
        int stdBooked = (int) occMetrics[0], stdTotal = (int) occMetrics[1];
        double stdOccPct = occMetrics[2];
        int delBooked = (int) occMetrics[3], delTotal = (int) occMetrics[4];
        double delOccPct = occMetrics[5];
        int steBooked = (int) occMetrics[6], steTotal = (int) occMetrics[7];
        double steOccPct = occMetrics[8];
        int totalAllBooked = (int) occMetrics[9], totalAllRooms = (int) occMetrics[10];
        double overallOccPct = occMetrics[11];

        // Ask Control layer to calculate demand target metrics
        double[] demandMetrics = control.calculateTargetDemandMetrics(roomTypeFilter, occMetrics,
                candidateRooms.size());
        int targetPhysicalRooms = (int) demandMetrics[0];
        int targetActiveBooked = (int) demandMetrics[1];
        int targetNetAvailable = (int) demandMetrics[2];
        double demandOccupancyRate = demandMetrics[3];

        String reportTitle = "ROOM AVAILABILITY & BOOKING DEMAND ANALYSIS REPORT";
        String filterCriteria = "Room Type = " + roomTypeFilter + " | Status = " + statusFilter + " | Sort = " + sortBy
                + " | Period = " + checkInPeriod.toLocalDate() + " to " + checkOutPeriod.toLocalDate();
        String executionPipeline = "Candidate Filter -> Period Search -> Insertion Sort";
        printReportHeader(reportTitle, filterCriteria, executionPipeline);

        System.out.println("\n PERIOD OCCUPANCY & DEMAND VISUALIZER (BAR CHART):");
        System.out.printf("  Standard : %s %5.1f%% Occupied (%d/%d Booked)%n", generateProgressBar(stdOccPct, 30),
                stdOccPct, stdBooked, stdTotal);
        System.out.printf("  Deluxe   : %s %5.1f%% Occupied (%d/%d Booked)%n", generateProgressBar(delOccPct, 30),
                delOccPct, delBooked, delTotal);
        System.out.printf("  Suite    : %s %5.1f%% Occupied (%d/%d Booked)%n", generateProgressBar(steOccPct, 30),
                steOccPct, steBooked, steTotal);
        System.out.println("  --------------------------------------------------------------------------------------");
        System.out.printf("  OVERALL  : %s %5.1f%% Occupied (%d/%d Booked)%n\n", generateProgressBar(overallOccPct, 30),
                overallOccPct, totalAllBooked, totalAllRooms);
        System.out.println(SUB_DIVIDER);

        System.out.println(" PERIOD AVAILABILITY & DEMAND METRICS SUMMARY:");
        System.out.printf("  - Total Physical Inventory Rooms : %d room(s)%n", targetPhysicalRooms);
        System.out.printf("  - Active Booked Rooms (Period)   : %d room(s)%n", targetActiveBooked);
        System.out.printf("  - Net Available Rooms (Period)   : %d room(s)%n", targetNetAvailable);
        System.out.printf("  - Period Demand Occupancy Rate  : %.1f%%%n", demandOccupancyRate);
        System.out.println(SUB_DIVIDER);

        if (candidateRooms.isEmpty()) {
            System.out.println(" [!] No rooms matched the specified search and filter criteria.");
            System.out.println(DIVIDER);
            return;
        }

        System.out.println(" CANDIDATE ROOMS AVAILABILITY TRAIL (Sorted):");
        String tableBorder = "┌────┬──────────┬───────────┬────────────────┬──────────────────┐";
        String tableHeader = "│No. │ Room No  │ Room Type │ Rate/Night(RM) │ Inventory Status │";
        String tableSep = "├────┼──────────┼───────────┼────────────────┼──────────────────┤";
        String tableFooter = "└────┴──────────┴───────────┴────────────────┴──────────────────┘";

        System.out.println(tableBorder);
        System.out.println(tableHeader);
        System.out.println(tableSep);

        int idx = 1;
        Iterator<Room> roomIt = candidateRooms.getIterator();
        while (roomIt != null && roomIt.hasNext()) {
            Room r = roomIt.next();
            if (r == null)
                continue;
            System.out.printf("│%3d │ %-8s │ %-9s │ %14.2f │ %-16s │%n",
                    idx++,
                    r.getRoomNo(),
                    r.getRoomType(),
                    r.getPricePerNight(),
                    r.getStatus());
        }
        System.out.println(tableFooter);

        // Ask Control layer for strategic recommendations & top occupied types
        String[] recommendation = control.generateDemandRecommendation(demandOccupancyRate);
        System.out.println("MANAGEMENT STRATEGIC RECOMMENDATIONS:");
        System.out.println(recommendation[0]);
        System.out.println(recommendation[1]);

        String[] topTypes = control.calculateTopOccupancyAndAvailabilityTypes(stdOccPct, delOccPct, steOccPct);
        System.out.println(SUB_DIVIDER);
        System.out.printf(" < Highest Occupancy Room Type : %s (%s%% Occupied) >%n", topTypes[0], topTypes[1]);
        System.out.printf(" < Highest Available Room Type : %s (%s%% Available) >%n", topTypes[2], topTypes[3]);
        System.out.println(SUB_DIVIDER);
        System.out.println("                               END OF THE REPORT");
        System.out.println("=================================================================================");
    }

    /**
     * Report 2 UI: Daily Arrivals, Departures & In-House Operational Action Report
     * Shift Handover Report: Dynamic Status Classification -> Date Filter ->
     * Multi-Criteria Sorting -> Action Plan
     */
    private void generateDailyArrivalsDeparturesActionReportUI() {
        System.out.println("\n" + SUB_DIVIDER);
        System.out.println(" REPORT 2: DAILY ARRIVALS, DEPARTURES & IN-HOUSE ACTION REPORT");
        System.out.println(SUB_DIVIDER);

        // 1. Shift Target Date Selection (Default: Today)
        LocalDate shiftDate = LocalDate.now();
        System.out.println("Target Shift Date: " + shiftDate + " (TODAY)");
        System.out.print("Customize Target Shift Date? (Y/N, Default N): ");
        String customAns = scanner.nextLine().trim();
        if (customAns.equalsIgnoreCase("Y") || customAns.equalsIgnoreCase("YES")) {
            shiftDate = InputUtil.readAnyDate(scanner, "Enter Target Shift Date (yyyy-MM-dd): ");
        }

        // 2. Select Operational Status Filter
        System.out.println("\nSelect Operational Mode Filter:");
        System.out.println(" [1] ALL Operational Statuses (Default)");
        System.out.println(" [2] Pending Arrivals (Check-In Due Today)");
        System.out.println(" [3] Pending Departures (Check-Out Due Today)");
        System.out.println(" [4] In-House Active Stays");
        System.out.println(" [5] Overdue / Late Check-Out Alert (!)");
        int opOpt = InputUtil.readIntInRangeWithDefault(scanner, "Enter Operational Option [1-5] (Default 1): ", 1, 5,
                1);

        String opModeFilter = switch (opOpt) {
            case 2 -> "ARRIVALS_DUE";
            case 3 -> "DEPARTURES_DUE";
            case 4 -> "IN_HOUSE_STAY";
            case 5 -> "OVERDUE_ALERT";
            default -> "ALL";
        };

        // 3. Room Type Filter
        System.out.println("\nSelect Room Type Filter:");
        System.out.println(" [1] ALL Room Types (Default)");
        System.out.println(" [2] Suite");
        System.out.println(" [3] Deluxe");
        System.out.println(" [4] Standard");
        int roomTypeOpt = InputUtil.readIntInRangeWithDefault(scanner, "Enter Room Type Option [1-4] (Default 1): ", 1,
                4, 1);

        String roomTypeFilter = switch (roomTypeOpt) {
            case 2 -> "Suite";
            case 3 -> "Deluxe";
            case 4 -> "Standard";
            default -> "ALL";
        };

        // 4. Select Sort Option
        System.out.println("\nSelect Sort Criteria:");
        System.out.println(" [1] Guest Name (Alphabetical A-Z) [Default]");
        System.out.println(" [2] Scheduled Check-In Time (Earliest First)");
        System.out.println(" [3] Scheduled Check-Out Time (Earliest First)");
        System.out.println(" [4] Confirmation Number");
        System.out.println(" [5] Room Number (Ascending)");
        int sortOpt = InputUtil.readIntInRangeWithDefault(scanner, "Enter Sort Option [1-5] (Default 1): ", 1, 5, 1);

        String sortBy = switch (sortOpt) {
            case 2 -> "Date_Asc";
            case 3 -> "CheckOut_Asc";
            case 4 -> "ConfirmationNo";
            case 5 -> "RoomNo_Asc";
            default -> "GuestName_Asc";
        };

        ListInterface<Booking> reportBookings = control.generateDailyOperationalActionReport(
                shiftDate, opModeFilter, roomTypeFilter, sortBy);

        // Ask Control layer to calculate operational shift metrics (No business math in
        // UI)
        double[] opMetrics = control.calculateOperationalActionMetrics(reportBookings, shiftDate);
        int totalCount = (int) opMetrics[0];
        int pendingArrivals = (int) opMetrics[1];
        int pendingDepartures = (int) opMetrics[2];
        int inHouseStays = (int) opMetrics[3];
        int overdueLateCheckouts = (int) opMetrics[4];

        String sampleBannerSep = "===============================================================================================================================";
        String sampleDashSep = "───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────";

        String reportTitle = "DAILY ARRIVALS, DEPARTURES & IN-HOUSE ACTION REPORT";
        String filterCriteria = "Shift Date = " + shiftDate + " | Mode = " + opModeFilter + " | Room Type = "
                + roomTypeFilter + " | Sort = " + sortBy;
        String executionPipeline = "Dynamic Status Classification -> Linear ADT Filter -> CustomLinkedList Sort";
        printReportHeader(reportTitle, filterCriteria, executionPipeline);

        if (totalCount == 0) {
            System.out.println(" [!] No operational bookings matched the specified shift criteria.");
            System.out.println(sampleBannerSep);
            System.out.println("                                         END OF THE REPORT");
            System.out.println(sampleBannerSep);
            return;
        }

        // Output Data Table matching TAR UMT sample with Receptionist Focus: Contact
        // No, Room No, Room Type, Shift Schedule, Balance / Status, Action Status
        String tableBorder = "┌──────────┬──────────────────────┬──────────────┬──────────┬────────┬────────────────┬──────────────────┬───────────────────┐";
        String tableHeader = "│ Conf. No │ Guest Name           │ Contact No   │ Room No  │ Type   │ Shift Schedule │ Balance/Payment  │ Action Status     │";
        String tableSep = "├──────────┼──────────────────────┼──────────────┼──────────┼────────┼────────────────┼──────────────────┼───────────────────┤";
        String tableFooter = "└──────────┴──────────────────────┴──────────────┴──────────┴────────┴────────────────┴──────────────────┴───────────────────┘";

        System.out.println(tableBorder);
        System.out.println(tableHeader);
        System.out.println(tableSep);

        Iterator<Booking> printIt = reportBookings.getIterator();
        while (printIt != null && printIt.hasNext()) {
            Booking b = printIt.next();
            if (b == null)
                continue;

            String gName = (b.getGuest() != null && b.getGuest().getGuestName() != null)
                    ? b.getGuest().getGuestName()
                    : "N/A";
            if (gName.length() > 20)
                gName = gName.substring(0, 17) + "...";

            String contactNo = control.getGuestContactNumber(b);
            if (contactNo.length() > 12)
                contactNo = contactNo.substring(0, 12);

            String roomNo = (b.getRoomNo() != null && !b.getRoomNo().isEmpty()) ? b.getRoomNo() : "Unassigned";
            if (roomNo.length() > 8)
                roomNo = roomNo.substring(0, 6) + "..";

            String rType = (b.getRoomType() != null) ? b.getRoomType() : "N/A";
            if ("Standard".equalsIgnoreCase(rType))
                rType = "Std";
            if (rType.length() > 6)
                rType = rType.substring(0, 6);

            String timeSched = control.getBookingShiftTimeSummary(b, shiftDate);

            String payBalanceTag = control.getBookingPaymentBalanceTag(b);
            if (payBalanceTag.length() > 16)
                payBalanceTag = payBalanceTag.substring(0, 16);

            String actionTag = control.getBookingOperationalActionTag(b, shiftDate);

            System.out.printf("│ %-8s │ %-20s │ %-12s │ %-8s │ %-6s │ %-14s │ %-16s │ %-17s │%n", b.getConfirmationNo(),
                    gName,
                    contactNo,
                    roomNo,
                    rType,
                    timeSched,
                    payBalanceTag,
                    actionTag);
        }
        System.out.println(tableFooter);

        // Section Summary Metrics
        double totalBalanceDue = (opMetrics.length > 5) ? opMetrics[5] : 0.0;
        System.out.printf("Total shift bookings analyzed        : %d%n", totalCount);
        System.out.printf("Total pending arrivals (Check-In)    : %d%n", pendingArrivals);
        System.out.printf("Total pending departures (Check-Out) : %d%n", pendingDepartures);
        System.out.printf("Total active in-house stays          : %d%n", inHouseStays);
        System.out.printf("Total overdue / late check-out alert : %d%n", overdueLateCheckouts);
        System.out.printf("Total shift balance outstanding      : RM %.2f%n", totalBalanceDue);
        System.out.println(sampleDashSep);

        // Vertical ASCII Histogram / Bar Chart Visualizer (Exact TAR UMT Sample Chart
        // Style)
        renderVerticalHistogram(pendingArrivals, pendingDepartures, inHouseStays, overdueLateCheckouts);
        System.out.println(sampleDashSep);

        // TAR UMT Highlight Summary Block (Max / Min Summary)
        renderTarumtHighlights(pendingArrivals, pendingDepartures, inHouseStays, overdueLateCheckouts);
        System.out.println(sampleDashSep);

        // Ask Control layer for Operational Recommendations
        String[] recommendation = control.generateOperationalActionRecommendation(opMetrics);
        System.out.println("FRONT-DESK SHIFT HANDOVER ACTION PLAN:");
        System.out.println(recommendation[0]);
        System.out.println(recommendation[1]);

        System.out.println(sampleDashSep);
        System.out.println("                                         END OF THE REPORT");
        System.out.println(sampleBannerSep);
    }

    /**
     * Renders a Vertical ASCII Histogram / Bar Chart matching the TAR UMT sample
     * report format.
     */
    private static void renderVerticalHistogram(int arrivals, int departures, int inHouse, int overdue) {
        int maxVal = Math.max(arrivals, Math.max(departures, Math.max(inHouse, overdue)));
        if (maxVal == 0)
            maxVal = 1;

        int step = Math.max(1, (int) Math.ceil((double) maxVal / 5.0));
        int topY = step * 5;

        System.out.println("No of bookings");
        for (int y = topY; y >= step; y -= step) {
            String arrChar = (arrivals >= y) ? "*" : " ";
            String depChar = (departures >= y) ? "*" : " ";
            String inhChar = (inHouse >= y) ? "*" : " ";
            String ovdChar = (overdue >= y) ? "*" : " ";
            System.out.printf("%2d │         %s                 %s                  %s                   %s%n", y,
                    arrChar,
                    depChar, inhChar,
                    ovdChar);
        }
        System.out.println(
                " ──┼──────────────────┴──────────────────┴──────────────────┴──────────────────┴───> Operational Statuses");
        System.out.println("         Arrivals          Departures          In-House            Overdue");
    }

    /**
     * Renders TAR UMT Highlight Summary blocks (Status with most / least bookings).
     */
    private static void renderTarumtHighlights(int arrivals, int departures, int inHouse, int overdue) {
        String[] labels = { "Pending Arrivals (Check-In)", "Pending Departures (Check-Out)", "In-House Active Stays",
                "Overdue / Late Check-Out Alert" };
        int[] values = { arrivals, departures, inHouse, overdue };

        int maxIdx = 0;
        int minIdx = 0;
        for (int i = 1; i < values.length; i++) {
            if (values[i] > values[maxIdx])
                maxIdx = i;
            if (values[i] < values[minIdx])
                minIdx = i;
        }

        System.out.println("Operational status with the most bookings (" + values[maxIdx] + "):");
        System.out.println("< " + labels[maxIdx] + " >");
        System.out.println();
        System.out.println("Operational status with the least bookings (" + values[minIdx] + "):");
        System.out.println("< " + labels[minIdx] + " >");
    }

    /**
     * Prints a unified TAR UMT Report Header across all management reports.
     */
    private void printReportHeader(String reportTitle, String filterCriteria, String executionPipeline) {
        System.out.println(
                "\n===============================================================================================================================");
        System.out.println("                              TUNKU ABDUL RAHMAN UNIVERSITY OF MANAGEMENT AND TECHNOLOGY");
        System.out.println("                                           FRONT-DESK MANAGEMENT SUBSYSTEM");
        System.out.println(
                "───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────");
        System.out.println(" REPORT : " + reportTitle);
        System.out.println(" DATE   : " + LocalDateTime.now().format(DATETIME_FORMAT));
        System.out.println(" FILTER : " + filterCriteria);
        System.out.println(" LOGIC  : " + executionPipeline);
        System.out.println(
                "===============================================================================================================================");
    }
}