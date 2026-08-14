package boundary;

import control.FrontDeskSearchControl;
import entity.Booking;
import entity.Guest;
import entity.Room;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

/**
 * Boundary UI class for Front-Desk Search & Service module.
 * Provides interactive terminal interfaces for:
 * 1. Instant Guest & Booking Search by Confirmation Number
 * 2. Search & Filter Available Rooms (Iterate -> Filter -> Sort pipeline)
 * 3. Search Bill & Process Cash Payment
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
            System.out.println(" FRONT-DESK SERVICE & INQUIRY (BST ADT & O(log n) Search)");
            System.out.println(DIVIDER);
            System.out.println("1. Instant Search Guest Record & Check-In (BST O(log n) Search)");
            System.out.println("2. Search & Filter Available Rooms (Iterate -> Filter -> Sort)");
            System.out.println("3. Search Bill, Calculate Late Fees & Process Check-Out (Cash / Deposit Refund)");
            System.out.println("4. View ADT & Algorithm Performance Summary");
            System.out.println("0. Return to Main Menu");
            System.out.println(DIVIDER);
            System.out.print("Enter Choice: ");

            choice = readChoice();

            switch (choice) {
                case 1 -> searchByConfirmationNo();
                case 2 -> searchAvailableRoomsUI();
                case 3 -> searchBillAndCheckOutUI();
                case 4 -> showAdtSummary();
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
     * Instantly retrieves complete guest information using BST Search by 8-digit
     * confirmation number.
     */
    private void searchByConfirmationNo() {
        System.out.println("\n" + SUB_DIVIDER);
        System.out.println(" INSTANT GUEST LOOKUP (BST SEARCH)");
        System.out.println(SUB_DIVIDER);
        System.out.print("Enter 8-Digit Confirmation Number (e.g. 84920183): ");
        String confirmationNo = scanner.nextLine().trim();

        if (!confirmationNo.matches("\\d{8}")) {
            System.out.println("\n[!] Error: Confirmation Number must be exactly 8 digits.");
            System.out.println("    (Sample valid numbers for testing: 84920183, 10293847, 59302148)");
            return;
        }

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

        // Seamless Check-In Prompt if status is CONFIRMED or PENDING
        if (Booking.STATUS_CHECKED_IN.equalsIgnoreCase(booking.getStatus())) {
            System.out.println("\n[i] Notice: This guest is already CHECKED-IN.");
            return;
        }
        if (Booking.STATUS_CANCELLED.equalsIgnoreCase(booking.getStatus())
                || Booking.STATUS_COMPLETED.equalsIgnoreCase(booking.getStatus())) {
            System.out.println("\n[i] Notice: Booking status is " + booking.getStatus() + ". No check-in required.");
            return;
        }
        if (Booking.STATUS_CONFIRMED.equalsIgnoreCase(booking.getStatus())) {
            System.out.println("\nDo you want to process Check-In and collect deposit for this guest now? (Y/N): ");
            String answer = scanner.nextLine().trim();
            if (!answer.equalsIgnoreCase("Y")) {
                System.out.println("Check-In skipped. Returning to menu.");
                return;
            }
            processCheckInFlow(booking);
        } else {
            System.out.println("\n[i] Notice: Booking status is " + booking.getStatus() + ". No action required.");
            System.out.println("\nPress any key to continue...");
            scanner.nextLine();
        }
    }

    /**
     * Formats and prints complete guest identification, room details, dates,
     * invoice, and payment status.
     */
    private void displayCompleteGuestDetails(Booking booking) {
        Guest guest = booking.getGuest();
        String roomInfo = (booking.getRoomNo() != null) ? booking.getRoomNo() : "Not Assigned Yet";

        System.out.println(DIVIDER);
        System.out.printf("  CONFIRMATION NO : %-18s INVOICE NO : %s%n", booking.getConfirmationNo(),
                booking.getInvoiceNo());
        System.out.printf("  BOOKING STATUS  : %-18s PAYMENT    : %s%n", booking.getStatus(),
                booking.getPaymentStatus());
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
        System.out.printf("  - Room Charge   : RM %8.2f  (RM %.2f / night)%n", booking.getRoomCharge(),
                booking.getPricePerNight());
        System.out.printf("  - Service Charge: RM %8.2f  (10%%)%n", booking.getServiceCharge());
        System.out.printf("  - Other Charges : RM %8.2f  (Tourism & Taxes)%n", booking.getOtherCharges());
        System.out.printf("  - Total Amount  : RM %8.2f%n", booking.getGrandTotal());
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

        System.out.println("\n" + DIVIDER);
        System.out.println(" TODAY'S ROOM AVAILABILITY OVERVIEW");
        System.out.println(DIVIDER);
        System.out.printf("  - Standard : %d / %d available  (RM 150.00 / night)%n", stdCounts[0], stdCounts[1]);
        System.out.printf("  - Deluxe   : %d / %d available  (RM 250.00 / night)%n", dlxCounts[0], dlxCounts[1]);
        System.out.printf("  - Suite    : %d / %d available  (RM 400.00 / night)%n", steCounts[0], steCounts[1]);
        System.out.println(DIVIDER);

        System.out.println("\nSelect Room Type Filter:");
        System.out.println(" 1. Standard");
        System.out.println(" 2. Deluxe");
        System.out.println(" 3. Suite");
        System.out.println(" 0. All Room Types (Press Enter)");
        System.out.print("Enter Choice [0-3]: ");
        String typeChoice = scanner.nextLine().trim();

        String roomType = switch (typeChoice) {
            case "1" -> "Standard";
            case "2" -> "Deluxe";
            case "3" -> "Suite";
            default -> "";
        };

        System.out.print("Enter Maximum Price per night Limit (e.g. 500 or press Enter for no limit): ");
        String priceInput = scanner.nextLine().trim();
        double maxPrice = -1;
        if (!priceInput.isEmpty()) {
            try {
                maxPrice = Double.parseDouble(priceInput);
            } catch (NumberFormatException ignored) {
            }
        }

        System.out.println("Select Sort Order: 1. By Room Number | 2. By Price per Night");
        System.out.print("Enter Choice [Default 1]: ");
        String sortChoice = scanner.nextLine().trim();
        String sortBy = "2".equals(sortChoice) ? "Price" : "RoomNo";

        System.out.println("\n[Processing Pipeline: 1. Iterate Rooms -> 2. Filter Available & Criteria -> 3. Sort by "
                + sortBy + "]");

        Room[] results = control.searchAvailableRooms(roomType, maxPrice, sortBy);

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

        // Interactive Booking Action Prompt
        System.out.println("\nWould you like to book a room now?");
        System.out.println(" 1. Walk-In Registration (Check-In NOW / Today)");
        System.out.println(" 2. Future Reservation (Advance Booking for Future Dates)");
        System.out.println(" 0. No / Return to Menu");
        System.out.print("Enter Choice: ");
        String actionChoice = scanner.nextLine().trim();

        if ("1".equals(actionChoice)) {
            System.out.println("\nRedirecting to Walk-In Registration Module...");
            WalkInRegistrationUI walkInUI = new WalkInRegistrationUI();
            walkInUI.registerBooking();
            control.reloadData();
        } else if ("2".equals(actionChoice)) {
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

        java.time.LocalDateTime checkIn = null;
        while (checkIn == null) {
            System.out.print("Enter Future Check-In Date & Time (yyyy-MM-dd HH:mm, e.g. 2026-08-20 15:00): ");
            String str = scanner.nextLine().trim();
            try {
                checkIn = java.time.LocalDateTime.parse(str, DATETIME_FORMAT);
            } catch (Exception e) {
                System.out.println("Invalid date format. Use yyyy-MM-dd HH:mm.");
            }
        }

        int nights = 0;
        while (nights <= 0) {
            System.out.print("Enter Number of Nights Staying (1-30): ");
            try {
                nights = Integer.parseInt(scanner.nextLine().trim());
            } catch (Exception ignored) {
            }
        }
        java.time.LocalDateTime checkOut = checkIn.plusDays(nights);

        int stdAvail = control.getAvailableRoomCountForPeriod("Standard", checkIn, checkOut);
        int dlxAvail = control.getAvailableRoomCountForPeriod("Deluxe", checkIn, checkOut);
        int steAvail = control.getAvailableRoomCountForPeriod("Suite", checkIn, checkOut);

        System.out.println("\n" + SUB_DIVIDER);
        System.out.printf(" AVAILABILITY FOR DATES: %s to %s%n", checkIn.format(DATETIME_FORMAT),
                checkOut.format(DATETIME_FORMAT));
        System.out.println(SUB_DIVIDER);
        System.out.printf(" 1. Standard : %d room(s) available (RM 150.00 / night)%n", stdAvail);
        System.out.printf(" 2. Deluxe   : %d room(s) available (RM 250.00 / night)%n", dlxAvail);
        System.out.printf(" 3. Suite    : %d room(s) available (RM 400.00 / night)%n", steAvail);
        System.out.println(SUB_DIVIDER);

        System.out.print("Select Room Type [1-3]: ");
        String typeChoice = scanner.nextLine().trim();

        String roomType = switch (typeChoice) {
            case "2" -> "Deluxe";
            case "3" -> "Suite";
            default -> "Standard";
        };

        int availableForSelected = control.getAvailableRoomCountForPeriod(roomType, checkIn, checkOut);
        if (availableForSelected <= 0) {
            System.out.printf("%n[X] Error: %s is FULLY BOOKED for the requested dates (%s to %s).%n",
                    roomType, checkIn.format(DATETIME_FORMAT), checkOut.format(DATETIME_FORMAT));
            System.out.println("    Reservation cancelled to prevent overbooking.");
            return;
        }

        double pricePerNight = switch (roomType) {
            case "Deluxe" -> 250.0;
            case "Suite" -> 400.0;
            default -> 150.0;
        };

        int numGuests = 1;
        System.out.print("Enter Number of Guests (1-6) [Default 1]: ");
        try {
            numGuests = Integer.parseInt(scanner.nextLine().trim());
        } catch (Exception ignored) {
        }

        // Guest Phone Lookup & Reuse
        System.out.print("Enter Guest Contact Number (digits only): ");
        String contact = scanner.nextLine().trim();

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
            System.out.print("Enter Guest Name: ");
            name = scanner.nextLine().trim();
            System.out.print("Enter IC/Passport Number: ");
            icPassport = scanner.nextLine().trim();
        }

        double roomChargeTotal = pricePerNight * nights;
        double depositRequired = roomChargeTotal * 0.30;

        System.out.println("\n" + SUB_DIVIDER);
        System.out.println(" RESERVATION SUMMARY & 30% DEPOSIT DUE");
        System.out.println(SUB_DIVIDER);
        System.out.printf(" Room Type         : %s (RM %.2f / night)%n", roomType, pricePerNight);
        System.out.printf(" Duration          : %s to %s (%d Night(s))%n", checkIn.format(DATETIME_FORMAT),
                checkOut.format(DATETIME_FORMAT), nights);
        System.out.printf(" Total Room Charge : RM %10.2f%n", roomChargeTotal);
        System.out.printf(" 30%% Deposit Required: RM %10.2f%n", depositRequired);
        System.out.println(SUB_DIVIDER);

        double cashTendered = 0;
        while (cashTendered < depositRequired) {
            System.out.printf("Enter Cash Deposit Received (Min RM %.2f): ", depositRequired);
            try {
                cashTendered = Double.parseDouble(scanner.nextLine().trim());
                if (cashTendered < depositRequired) {
                    System.out.println("Insufficient cash received.");
                }
            } catch (Exception ignored) {
            }
        }

        Booking booking = control.registerFutureBooking(name, contact, icPassport, roomType, pricePerNight, numGuests,
                checkIn, checkOut);
        double change = cashTendered - depositRequired;

        System.out.println("\n" + DIVIDER);
        System.out.println(" ADVANCE RESERVATION SUCCESSFUL!");
        System.out.println(DIVIDER);
        System.out.printf("  Confirmation No   : %s%n", booking.getConfirmationNo());
        System.out.printf("  Booking Status    : %s%n", booking.getStatus());
        System.out.printf("  Deposit Paid      : RM %10.2f%n", depositRequired);
        System.out.printf("  Cash Received     : RM %10.2f%n", cashTendered);
        System.out.printf("  Change Due        : RM %10.2f%n", change);
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
        System.out.print("Enter 8-Digit Confirmation Number: ");
        String confirmationNo = scanner.nextLine().trim();

        if (!confirmationNo.matches("\\d{8}")) {
            System.out.println("\n[!] Error: Confirmation Number must be exactly 8 digits.");
            return;
        }

        Booking booking = control.searchByConfirmationNo(confirmationNo);
        if (booking == null) {
            System.out.println("\n[X] Booking record not found for Confirmation Number: " + confirmationNo);
            return;
        }

        // Status validation
        if (Booking.STATUS_COMPLETED.equalsIgnoreCase(booking.getStatus())) {
            System.out.println("\n[!] Notice: This booking is already CHECKED-OUT (Status: COMPLETED).");
            return;
        }
        if (Booking.STATUS_CANCELLED.equalsIgnoreCase(booking.getStatus())) {
            System.out.println("\n[!] Notice: Cannot check-out a CANCELLED booking.");
            return;
        }
        if (Booking.STATUS_PENDING.equalsIgnoreCase(booking.getStatus())) {
            System.out.println("\n[!] Notice: Guest has not checked in yet (Status: PENDING).");
            return;
        }

        Guest guest = booking.getGuest();
        String roomInfo = (booking.getRoomNo() != null) ? booking.getRoomNo() : "Unassigned";

        System.out.println("\n" + SUB_DIVIDER);
        System.out.println(" LATE CHECK-OUT DURATION SELECTION");
        System.out.println(SUB_DIVIDER);
        double pricePerNight = booking.getPricePerNight();
        System.out.println(" [0] <= 30 mins        : Grace Period (Free - RM 0.00)");
        System.out.printf(" [1] 30 mins - 2 hrs   : +25%% Price/Night (+RM %.2f)%n", pricePerNight * 0.25);
        System.out.printf(" [2] 2 - 4 hrs         : +50%% Price/Night (+RM %.2f)%n", pricePerNight * 0.50);
        System.out.printf(" [3] > 4 hrs           : +100%% Price/Night / Extra 1 Night (+RM %.2f)%n",
                pricePerNight * 1.00);
        System.out.println(SUB_DIVIDER);

        int lateOption = 0;
        while (true) {
            System.out.print("Select Late Check-Out Option (0 - 3): ");
            String optStr = scanner.nextLine().trim();
            try {
                int opt = Integer.parseInt(optStr);
                if (opt >= 0 && opt <= 3) {
                    lateOption = opt;
                    break;
                }
                System.out.println("Invalid option! Please enter 0, 1, 2, or 3.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid numeric input. Please enter 0, 1, 2, or 3.");
            }
        }

        double roomCharge = booking.getRoomCharge();
        double serviceCharge = booking.getServiceCharge();
        double lateCharge = control.calculateLateCheckOutCharge(pricePerNight, lateOption);
        double otherCharges = booking.getOtherCharges();

        double subtotalBill = roomCharge + serviceCharge + lateCharge + otherCharges;
        double depositPaid = booking.getTotalCheckInDeposit();
        double netDue = subtotalBill - depositPaid;

        String lateOptLabel = switch (lateOption) {
            case 1 -> "30 mins - 2 hrs (+25%)";
            case 2 -> "2 - 4 hrs (+50%)";
            case 3 -> "> 4 hrs (+100% / Extra 1 Night)";
            default -> "Grace Period (Free)";
        };

        System.out.println("\n" + DIVIDER);
        System.out.println(" HOTEL CHECK-OUT STATEMENT & DEPOSIT SETTLEMENT (" + booking.getInvoiceNo() + ")");
        System.out.println(DIVIDER);
        System.out.printf(" Confirmation No: %s%n", booking.getConfirmationNo());
        System.out.printf(" Guest Name     : %s%n", (guest != null ? guest.getGuestName() : "N/A"));
        System.out.printf(" Contact Number : %s%n", (guest != null ? guest.getContactNumber() : "N/A"));
        System.out.printf(" Room Details   : %s (Room %s)%n", booking.getRoomType(), roomInfo);
        System.out.printf(" Check-In Date  : %s%n", booking.getCheckInDateTime().format(DATETIME_FORMAT));
        System.out.printf(" Check-Out Date : %s (%d Night(s))%n", booking.getCheckOutDateTime().format(DATETIME_FORMAT),
                booking.getNights());
        System.out.println(SUB_DIVIDER);
        System.out.printf(" Room Charge    : RM %10.2f (%d Night(s) @ RM %.2f/night)%n", roomCharge,
                booking.getNights(), pricePerNight);
        System.out.printf(" Service Charge : RM %10.2f  (10%%)%n", serviceCharge);
        System.out.printf(" Late Check-out : RM %10.2f  (%s)%n", lateCharge, lateOptLabel);
        System.out.printf(" Other Charges  : RM %10.2f%n", otherCharges);
        System.out.println(SUB_DIVIDER);
        System.out.printf(" SUBTOTAL BILL  : RM %10.2f%n", subtotalBill);
        System.out.printf(" Less: 30%% Dep  : RM %10.2f  (Pre-paid Booking Deposit)%n", -booking.getBookingDeposit());
        System.out.printf(" Less: Sec Dep  : RM %10.2f  (Pre-paid Security Deposit)%n", -booking.getSecurityDeposit());
        System.out.println(SUB_DIVIDER);

        if (netDue > 0) {
            System.out.printf(" NET BALANCE DUE: RM %10.2f  (Customer to Pay)%n", netDue);
        } else if (netDue < 0) {
            System.out.printf(" NET REFUND DUE : RM %10.2f  (To Refund to Customer)%n", Math.abs(netDue));
        } else {
            System.out.printf(" NET BALANCE DUE: RM %10.2f  (Fully Settled by Deposit)%n", 0.0);
        }
        System.out.println(DIVIDER);

        System.out.print("\nProceed to complete Check-Out & final settlement? (Y/N): ");
        String answer = scanner.nextLine().trim();
        if (!answer.equalsIgnoreCase("Y")) {
            System.out.println("Check-Out processing cancelled.");
            return;
        }

        double cashTendered = 0.0;
        if (netDue > 0) {
            while (true) {
                System.out.printf("Enter Cash Amount Received from Customer (Min RM %.2f): ", netDue);
                String cashStr = scanner.nextLine().trim();
                try {
                    cashTendered = Double.parseDouble(cashStr);
                    if (cashTendered < netDue) {
                        System.out.printf(
                                "Insufficient cash! Cash received (RM %.2f) is less than net due (RM %.2f).%n",
                                cashTendered, netDue);
                        continue;
                    }
                    break;
                } catch (NumberFormatException e) {
                    System.out.println("Invalid numeric amount. Please re-enter.");
                }
            }
        }

        boolean success = control.processCheckOut(confirmationNo, lateOption, cashTendered);
        if (success) {
            System.out.println("\n" + SUB_DIVIDER);
            System.out.println(" CHECK-OUT COMPLETED SUCCESSFULLY!");
            System.out.println(SUB_DIVIDER);
            System.out.printf("  Confirmation No   : %s%n", booking.getConfirmationNo());
            System.out.printf("  New Booking Status: %s%n", Booking.STATUS_COMPLETED);
            System.out.printf("  Assigned Room %-4s : Set to Status %s%n", roomInfo, Room.STATUS_DIRTY);
            if (netDue > 0) {
                System.out.printf("  Net Payment Recv'd: RM %10.2f%n", cashTendered);
                System.out.printf("  Change Returned   : RM %10.2f%n", cashTendered - netDue);
            } else if (netDue < 0) {
                System.out.printf("  Refund Returned   : RM %10.2f%n", Math.abs(netDue));
            } else {
                System.out.println("  Final Settlement  : Fully Paid & Settled (RM 0.00 balance)");
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
        double roomCharge = booking.getRoomCharge();
        double bookingDeposit = booking.getBookingDeposit();
        double securityDeposit = booking.getSecurityDeposit();
        double totalRequiredDeposit = booking.getTotalCheckInDeposit();

        System.out.println("\n" + DIVIDER);
        System.out.println(" CHECK-IN STATEMENT & DEPOSIT BREAKDOWN");
        System.out.println(DIVIDER);
        System.out.printf(" Room Charge Total    : RM %10.2f (%d Night(s) @ RM %.2f/night)%n", roomCharge,
                booking.getNights(), booking.getPricePerNight());
        System.out.printf(" 30%% Booking Deposit   : RM %10.2f%n", bookingDeposit);
        System.out.printf(" Security Deposit     : RM %10.2f (Refundable upon Check-out)%n", securityDeposit);
        System.out.println(SUB_DIVIDER);
        System.out.printf(" TOTAL DEPOSIT DUE    : RM %10.2f%n", totalRequiredDeposit);
        System.out.println(DIVIDER);

        while (true) {
            System.out.printf("Enter Cash Amount Received for Deposit (Min RM %.2f): ", totalRequiredDeposit);
            String cashStr = scanner.nextLine().trim();
            try {
                double cashTendered = Double.parseDouble(cashStr);
                if (cashTendered < totalRequiredDeposit) {
                    System.out.printf(
                            "Insufficient cash! Received (RM %.2f) is less than required deposit (RM %.2f).%n",
                            cashTendered, totalRequiredDeposit);
                    continue;
                }

                double change = cashTendered - totalRequiredDeposit;
                boolean success = control.processCheckIn(booking.getConfirmationNo(), cashTendered);

                if (success) {
                    System.out.println("\n" + SUB_DIVIDER);
                    System.out.println(" CHECK-IN SUCCESSFUL!");
                    System.out.println(SUB_DIVIDER);
                    System.out.printf("  Confirmation No   : %s%n", booking.getConfirmationNo());
                    System.out.printf("  New Booking Status: %s%n", Booking.STATUS_CHECKED_IN);
                    System.out.printf("  Deposit Required  : RM %10.2f%n", totalRequiredDeposit);
                    System.out.printf("  Cash Tendered     : RM %10.2f%n", cashTendered);
                    System.out.printf("  Change Due        : RM %10.2f%n", change);
                    System.out.println(SUB_DIVIDER);
                } else {
                    System.out.println("\n[!] Check-in processing error.");
                }
                break;
            } catch (NumberFormatException e) {
                System.out.println("Invalid numeric amount. Please re-enter.");
            }
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
        System.out.println(" Variable Declaration: Interface Type (adt.BSTInterface<Booking>) [Best Practice]");
        System.out.println(" ADT Implemented     : Custom Non-Linear BST (adt.MyBinarySearchTree)");
        System.out.println(" Node Structure      : Node (data, left, right)");
        System.out.println(" Iterator Provided   : java.util.Iterator<T> via getIterator()");
        System.out.println(" Search Algorithm    : Binary Search Tree Search (Key = 8-Digit Confirmation Number)");
        System.out.println(" Time Complexity     : O(log n) Average Lookup / O(log n) Average Insertion");
        System.out.println(" Total Records Count : " + control.getTotalIndexedCount() + " indexed booking(s)");

        // Retrieve iterator and bounds for Analytics Report
        java.util.Iterator<Booking> iterator = control.getBookingIterator();
        Booking minBooking = control.getMinBooking();
        Booking maxBooking = control.getMaxBooking();

        int totalBookings = 0;
        double grandTotalRevenue = 0.0;
        int totalNights = 0;

        int standardCount = 0, deluxeCount = 0, suiteCount = 0;
        double standardRev = 0.0, deluxeRev = 0.0, suiteRev = 0.0;

        int confirmedCount = 0, pendingCount = 0, paidCount = 0;

        while (iterator != null && iterator.hasNext()) {
            Booking b = iterator.next();
            if (b == null)
                continue;

            totalBookings++;
            double total = b.getGrandTotal();
            grandTotalRevenue += total;
            totalNights += b.getNights();

            if ("Standard".equalsIgnoreCase(b.getRoomType())) {
                standardCount++;
                standardRev += total;
            } else if ("Deluxe".equalsIgnoreCase(b.getRoomType())) {
                deluxeCount++;
                deluxeRev += total;
            } else if ("Suite".equalsIgnoreCase(b.getRoomType())) {
                suiteCount++;
                suiteRev += total;
            }

            if (Booking.STATUS_CONFIRMED.equalsIgnoreCase(b.getStatus()))
                confirmedCount++;
            if (Booking.STATUS_PENDING.equalsIgnoreCase(b.getStatus()))
                pendingCount++;
            if (Booking.PAYMENT_PAID_CASH.equalsIgnoreCase(b.getPaymentStatus()))
                paidCount++;
        }

        double avgRevenue = (totalBookings > 0) ? (grandTotalRevenue / totalBookings) : 0.0;
        double avgNights = (totalBookings > 0) ? ((double) totalNights / totalBookings) : 0.0;

        System.out.println(DIVIDER);
        System.out.println(" SYSTEM SUMMARY & ANALYTICS REPORT (Collection ADT Evaluation)");
        System.out.println(DIVIDER);
        System.out.printf("  Total Bookings       : %d booking(s)%n", totalBookings);
        System.out.printf("  Total System Revenue : RM %.2f%n", grandTotalRevenue);
        System.out.printf("  Average Booking Value: RM %.2f%n", avgRevenue);
        System.out.printf("  Average Stay Duration: %.1f night(s)%n", avgNights);
        System.out.println(SUB_DIVIDER);
        System.out.println(" BST BOUNDS ANALYSIS (getMin() / getMax() Operations):");
        System.out.printf("  - Min Confirmation No: %s (%s - RM %.2f)%n",
                (minBooking != null ? minBooking.getConfirmationNo() : "N/A"),
                (minBooking != null && minBooking.getGuest() != null ? minBooking.getGuest().getGuestName() : "N/A"),
                (minBooking != null ? minBooking.getGrandTotal() : 0.0));
        System.out.printf("  - Max Confirmation No: %s (%s - RM %.2f)%n",
                (maxBooking != null ? maxBooking.getConfirmationNo() : "N/A"),
                (maxBooking != null && maxBooking.getGuest() != null ? maxBooking.getGuest().getGuestName() : "N/A"),
                (maxBooking != null ? maxBooking.getGrandTotal() : 0.0));
        System.out.println(SUB_DIVIDER);
        System.out.println(" CATEGORY BREAKDOWN BY ROOM TYPE:");
        System.out.printf("  - Standard Rooms     : %2d booking(s) | Revenue: RM %8.2f%n", standardCount, standardRev);
        System.out.printf("  - Deluxe Rooms       : %2d booking(s) | Revenue: RM %8.2f%n", deluxeCount, deluxeRev);
        System.out.printf("  - Suite Rooms        : %2d booking(s) | Revenue: RM %8.2f%n", suiteCount, suiteRev);
        System.out.println(SUB_DIVIDER);
        System.out.println(" BOOKING STATUS SUMMARY:");
        System.out.printf("  - Confirmed Bookings : %d%n", confirmedCount);
        System.out.printf("  - Pending Bookings   : %d%n", pendingCount);
        System.out.printf("  - Paid (Cash)        : %d%n", paidCount);
        System.out.println(DIVIDER);
    }
}
