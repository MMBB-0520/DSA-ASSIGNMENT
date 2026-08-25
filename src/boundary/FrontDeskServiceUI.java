package boundary;

import control.FrontDeskServiceControl;
import entity.Bill;
import entity.Booking;
import entity.Guest;
import entity.Room;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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
            System.out.println("[7] ADT & Algorithm Performance Summary");
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
                case 7 -> showAdtSummary();
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

        System.out.println("\nSelect Action:");
        System.out.println(" 1. View & Filter Available Rooms (By Type / Max Price / Sort)");
        System.out.println(" 2. Walk-In Registration");
        System.out.println(" 3. Future Reservation");
        System.out.println(" 0. Return to Menu");
        int actionChoice = InputUtil.readIntInRange(scanner, "Enter Choice [0-3]: ", 0, 3);

        if (actionChoice == 1) {
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
                walkInUI.registerBooking();
                control.reloadData();
            }

        } else if (actionChoice == 2) {
            System.out.println("\nRedirecting to Walk-In Registration Module...");
            WalkInRegistrationUI walkInUI = new WalkInRegistrationUI();
            walkInUI.registerBooking();
            control.reloadData();

        } else if (actionChoice == 3) {
            processFutureReservationUI();
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
        System.out.println("\n" + DIVIDER);
        System.out.println(" ADT & SEARCH ALGORITHM TECHNICAL SUMMARY");
        System.out.println(DIVIDER);
        System.out.println(" 1. LINEAR ADT       : adt.ListInterface<Room> -> adt.CustomLinkedList<Room>");
        System.out.println("    - Purpose        : Dynamic candidate room filtering & pipeline sorting");
        System.out.println(" 2. NON-LINEAR ADT   : adt.BSTInterface<Booking> -> adt.MyBinarySearchTree<Booking>");
        System.out.println("    - Variable Type  : Interface Type (BSTInterface) [Best Practice restriction]");
        System.out.println("    - Node Structure : Internal Node (data, left, right)");
        System.out.println("    - Iterator       : java.util.Iterator<T> via getIterator()");
        System.out.println("    - Search Key     : 8-Digit Confirmation Number");
        System.out.println("    - Time Complexity: O(log n) Average Lookup / O(log n) Average Insertion");
        System.out.println(" Total Records Count : " + control.getTotalIndexedCount() + " indexed booking(s)");

        System.out.println(DIVIDER);
        System.out.println(" SYSTEM SUMMARY & ANALYTICS REPORT (Collection ADT Evaluation)");
        System.out.println(DIVIDER);
        System.out.printf("  Total Bookings       : %d booking(s)%n", control.getTotalBookingsCount());
        System.out.printf("  Total System Revenue : RM %.2f%n", control.getGrandTotalRevenue());
        System.out.printf("  Average Booking Value: RM %.2f%n", control.getAverageRevenuePerBooking());
        System.out.printf("  Average Stay Duration: %.1f night(s)%n", control.getAverageStayNights());
        System.out.println(SUB_DIVIDER);
        System.out.println(" BST BOUNDS ANALYSIS (getMin() / getMax() Operations):");

        Booking minBooking = control.getMinBooking();
        if (minBooking != null) {
            Guest g = minBooking.getGuest();
            String guestName = (g != null) ? g.getGuestName() : "N/A";
            System.out.printf("  - Min Confirmation No: %s (%s - RM %.2f)%n",
                    minBooking.getConfirmationNo(), guestName, minBooking.getBill().getGrandTotal());
        } else {
            System.out.println("  - Min Confirmation No: N/A");
        }

        Booking maxBooking = control.getMaxBooking();
        if (maxBooking != null) {
            Guest g = maxBooking.getGuest();
            String guestName = (g != null) ? g.getGuestName() : "N/A";
            System.out.printf("  - Max Confirmation No: %s (%s - RM %.2f)%n",
                    maxBooking.getConfirmationNo(), guestName, maxBooking.getBill().getGrandTotal());
        } else {
            System.out.println("  - Max Confirmation No: N/A");
        }

        System.out.println(SUB_DIVIDER);
        System.out.println(" CATEGORY BREAKDOWN BY ROOM TYPE:");
        System.out.printf("  - Standard Rooms     : %2d booking(s) | Revenue: RM %8.2f%n",
                control.getBookingCountByRoomType("Standard"), control.getRevenueByRoomType("Standard"));
        System.out.printf("  - Deluxe Rooms       : %2d booking(s) | Revenue: RM %8.2f%n",
                control.getBookingCountByRoomType("Deluxe"), control.getRevenueByRoomType("Deluxe"));
        System.out.printf("  - Suite Rooms        : %2d booking(s) | Revenue: RM %8.2f%n",
                control.getBookingCountByRoomType("Suite"), control.getRevenueByRoomType("Suite"));
        System.out.println(SUB_DIVIDER);
        System.out.println(" BOOKING STATUS SUMMARY:");
        System.out.printf("  - Confirmed Bookings : %d%n", control.getBookingCountByStatus(Booking.STATUS_CONFIRMED));
        System.out.printf("  - Reserved (Future)  : %d%n", control.getBookingCountByStatus(Booking.STATUS_RESERVED));
        System.out.printf("  - Pending (Queue)    : %d%n", control.getBookingCountByStatus(Booking.STATUS_PENDING));
        System.out.printf("  - No-Show Bookings   : %d%n", control.getBookingCountByStatus(Booking.STATUS_NO_SHOW));
        System.out.printf("  - Paid (Cash)        : %d%n", control.getPaidBookingCount());
        System.out.println(DIVIDER);
    }
}