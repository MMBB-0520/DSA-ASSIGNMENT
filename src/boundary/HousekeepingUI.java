package boundary;

import control.HousekeepingControl;
import entity.Room;

import java.util.Scanner;

public class HousekeepingUI {
    private HousekeepingControl controller;
    private Scanner scanner;

    public HousekeepingUI() {
        this.controller = new HousekeepingControl();
        this.scanner = new Scanner(System.in);
    }

    public void runMenu() {
        int choice = -1;
        do {
            displayMenu();
            try {
                String input = scanner.nextLine().trim();
                choice = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                choice = -1;
            }

            switch (choice) {
                case 1 -> viewRoomsStatus();
                case 2 -> startCleaning();
                case 3 -> completeCleaning();
                case 4 -> inspectRoom();
                case 5 -> controller.viewRawTaskLog();
                case 6 -> generateStaffEfficiencyReportUI();
                case 7 -> generateRoomHygieneReportUI();
                case 8 -> controller.undoLastAction();
                case 0 -> System.out.println("\nReturning to Main Menu...");
                default -> System.out.println("\n[!] Invalid choice! Please enter a number between 0 and 8.");
            }
        } while (choice != 0);
    }

    private void displayMenu() {
        System.out.println("\n*******************************************************************");
        System.out.println("*                       HOUSEKEEPING MODULE                       *");
        System.out.println("*******************************************************************");
        System.out.println("* 1. View Dirty Rooms                                             *");
        System.out.println("* 2. Start Cleaning Rooms                                         *");
        System.out.println("* 3. Complete Cleaning Rooms                                      *");
        System.out.println("* 4. Inspect Room & Mark Ready for Check-In                       *");
        System.out.println("* 5. View Housekeeping Task Log                                   *");
        System.out.println("* 6. Report 1: Staff Cleaning Performance & Penalty Audit Report  *");
        System.out.println("* 7. Report 2: Room Hygiene & Supervisor Quality Audit Report     *");
        System.out.println("* 8. Rollback Last Action                                         *");
        System.out.println("* 0. Back to Main Menu                                            *");
        System.out.println("*******************************************************************");
        System.out.print("Please enter your choice (0-8): ");
    }

    /**
     * Displays rooms with status 'Dirty' (checked-out rooms ready to be cleaned by
     * housekeepers).
     */
    private void viewRoomsStatus() {
        System.out.println("\n=======================================================================================");
        System.out.println("                 DIRTY ROOMS AWAITING HOUSEKEEPING CLEANING                            ");
        System.out.println("=======================================================================================");
        System.out.printf("%-10s %-20s %-15s %-26s %-15s\n", "Room No.", "Room Type", "Price (RM)", "Current Status",
                "Std Duration");
        System.out.println("---------------------------------------------------------------------------------------");
        Room[] dirtyRooms = controller.getDirtyRooms();
        if (dirtyRooms.length == 0) {
            System.out.println(" [i] Notice: No dirty rooms found. All rooms are clean or currently occupied!");
        } else {
            for (Room r : dirtyRooms) {
                int stdMins = controller.getStandardCleaningDurationMinutes(r.getRoomType());
                String st = r.getStatus();
                if (st.toLowerCase().contains("late check-out")) {
                    st = "DIRTY (LATE CHECK-OUT)";
                }
                System.out.printf("%-10s %-20s %-15.2f %-26s %-15s\n",
                        r.getRoomNo(), r.getRoomType(), r.getPricePerNight(), st, stdMins + " mins");
            }
        }
        System.out.println("=======================================================================================\n");
    }

    private void startCleaning() {
        System.out.println("\n--- Step 1: Start Room Cleaning ---");
        System.out.print("Enter Room Number to clean: ");
        String roomNo = scanner.nextLine().trim();

        Room room = controller.findRoom(roomNo);
        if (room == null) {
            System.out.println("[!] Error: Room not found.");
            return;
        }

        if (!room.getStatus().equalsIgnoreCase("Dirty")) {
            System.out.println("[!] Notice: Room " + roomNo + " is currently '" + room.getStatus()
                    + "'. Only 'Dirty' rooms can be started.");
            return;
        }

        System.out.print("Enter Housekeeper Staff ID [Press Enter for default 'S001']: ");
        String id = scanner.nextLine().trim();
        if (id.isEmpty()) {
            id = "S001";
        }

        controller.assignCleaning(roomNo, id);
    }

    private void completeCleaning() {
        System.out.println("\n--- Step 2: Complete Room Cleaning ---");
        System.out.print("Enter Room Number being cleaned: ");
        String roomNo = scanner.nextLine().trim();

        Room room = controller.findRoom(roomNo);
        if (room == null) {
            System.out.println("[!] Error: Room not found.");
            return;
        }

        if (!room.getStatus().equalsIgnoreCase("Cleaning In Progress")) {
            System.out.println("[!] Notice: Room " + roomNo + " is currently '" + room.getStatus()
                    + "'. Only 'Cleaning In Progress' rooms can be completed.");
            return;
        }

        System.out.print("Enter Housekeeper Staff ID [Press Enter for default 'S001']: ");
        String id = scanner.nextLine().trim();
        if (id.isEmpty()) {
            id = "S001";
        }

        controller.completeCleaning(roomNo, id);
    }

    /**
     * Supervisor inspection view for rooms done cleaning.
     */
    private void inspectRoom() {
        System.out.println("\n--- Step 3: Supervisor Inspection ---");

        Room[] cleanedRooms = controller.getCleanedRooms();

        if (cleanedRooms.length == 0) {
            System.out.println("\n[i] Notice: No rooms are currently waiting for inspection.");
        } else {
            System.out.println("\n=====================================================================");
            System.out.println("            ROOMS DONE CLEANING & READY FOR INSPECTION               ");
            System.out.println("=====================================================================");
            System.out.printf("%-10s %-20s %-15s %-25s\n", "Room No.", "Room Type", "Price (RM)", "Current Status");
            System.out.println("---------------------------------------------------------------------");
            for (Room r : cleanedRooms) {
                System.out.printf("%-10s %-20s %-15.2f %-25s\n",
                        r.getRoomNo(), r.getRoomType(), r.getPricePerNight(), r.getStatus());
            }
            System.out.println("=====================================================================");
        }

        System.out.print("\nEnter Room Number to inspect: ");
        String roomNo = scanner.nextLine().trim();

        Room room = controller.findRoom(roomNo);
        if (room == null) {
            System.out.println("[!] Error: Room not found.");
            return;
        }

        if (!room.getStatus().toLowerCase().startsWith("cleaned")) {
            System.out.println("[!] Notice: Room " + roomNo + " is currently '" + room.getStatus()
                    + "'. Only cleaned rooms can be inspected.");
            return;
        }

        System.out.print("Enter Supervisor Staff ID [Press Enter for default 'SP001']: ");
        String id = scanner.nextLine().trim();
        if (id.isEmpty()) {
            id = "SP001";
        }

        System.out.print("Did the room pass inspection? (Y = Approve / N = Reject to Redo): ");
        String isCleanStr = scanner.nextLine().trim();
        boolean isClean = isCleanStr.equalsIgnoreCase("Y");
        controller.inspectRoom(roomNo, id, isClean);
    }

    /**
     * Interactive UI for REPORT 1: Staff Efficiency & Penalty Audit.
     * Prompts for multi-criteria filters and custom sorting algorithm choice.
     */
    private void generateStaffEfficiencyReportUI() {
        System.out.println("\n--- [REPORT 1] Staff Cleaning Performance & Penalty Audit Configuration ---");

        // Criterion 1: Staff ID
        System.out.print("1. Enter Staff ID to filter (e.g. S001, S002, S003, S004) [Press Enter for ALL]: ");
        String staffId = scanner.nextLine().trim();
        if (staffId.isEmpty()) staffId = "ALL";

        // Criterion 2: Room Type
        System.out.println("2. Select Room Type filter:");
        System.out.println("   [1] ALL Room Types");
        System.out.println("   [2] Standard Rooms");
        System.out.println("   [3] Deluxe Rooms");
        System.out.println("   [4] Suite Rooms");
        System.out.print("   Enter choice (1-4) [Default 1]: ");
        String rtChoice = scanner.nextLine().trim();
        String roomType = switch (rtChoice) {
            case "2" -> "Standard";
            case "3" -> "Deluxe";
            case "4" -> "Suite";
            default -> "ALL";
        };

        // Criterion 3: Punctuality Filter
        System.out.println("3. Select Punctuality filter:");
        System.out.println("   [1] ALL Tasks");
        System.out.println("   [2] Completed On-Time Only");
        System.out.println("   [3] Overdue / Delayed Tasks Only (With Penalties)");
        System.out.println("   [4] In-Progress Tasks Only");
        System.out.print("   Enter choice (1-4) [Default 1]: ");
        String punctChoice = scanner.nextLine().trim();
        String punctuality = switch (punctChoice) {
            case "2" -> "ON_TIME";
            case "3" -> "OVERDUE";
            case "4" -> "IN_PROGRESS";
            default -> "ALL";
        };

        // Criterion 4: Penalty Fines
        System.out.print("4. Filter only records with penalty fine incurred? (Y/N) [Default N]: ");
        String penaltyOnlyStr = scanner.nextLine().trim();
        boolean onlyPenalties = penaltyOnlyStr.equalsIgnoreCase("Y");

        // Sorting Technique Selection
        System.out.println("\nSelect Sorting Technique / Metric Order:");
        System.out.println("   [1] Sort by Penalty Fine (Highest to Lowest) [QuickSort Algorithm]");
        System.out.println("   [2] Sort by Turnaround Duration (Longest to Shortest) [QuickSort Algorithm]");
        System.out.println("   [3] Sort by Turnaround Duration (Shortest to Longest) [QuickSort Algorithm]");
        System.out.println("   [4] Sort by Room Number (Ascending) [QuickSort Algorithm]");
        System.out.println("   [5] Sort by Staff ID (Ascending) [InsertionSort Algorithm]");
        System.out.println("   [6] Sort by Task Timestamp (Most Recent First) [QuickSort Algorithm]");
        System.out.print("   Enter sorting choice (1-6) [Default 1]: ");
        int sortChoice = 1;
        try {
            String sInput = scanner.nextLine().trim();
            if (!sInput.isEmpty()) {
                sortChoice = Integer.parseInt(sInput);
            }
        } catch (NumberFormatException e) {
            sortChoice = 1;
        }

        controller.displayStaffEfficiencyReport(staffId, roomType, punctuality, onlyPenalties, sortChoice);
    }

    /**
     * Interactive UI for REPORT 2: Room Hygiene & Supervisor Quality Audit.
     * Prompts for multi-criteria filters and custom sorting algorithm choice.
     */
    private void generateRoomHygieneReportUI() {
        System.out.println("\n--- [REPORT 2] Room Hygiene & Supervisor Quality Audit Configuration ---");

        // Criterion 1: Supervisor ID
        System.out.print("1. Enter Supervisor ID to filter (e.g. SP001, SP002) [Press Enter for ALL]: ");
        String supervisorId = scanner.nextLine().trim();
        if (supervisorId.isEmpty()) supervisorId = "ALL";

        // Criterion 2: Room Type
        System.out.println("2. Select Room Type filter:");
        System.out.println("   [1] ALL Room Types");
        System.out.println("   [2] Standard Rooms");
        System.out.println("   [3] Deluxe Rooms");
        System.out.println("   [4] Suite Rooms");
        System.out.print("   Enter choice (1-4) [Default 1]: ");
        String rtChoice = scanner.nextLine().trim();
        String roomType = switch (rtChoice) {
            case "2" -> "Standard";
            case "3" -> "Deluxe";
            case "4" -> "Suite";
            default -> "ALL";
        };

        // Criterion 3: Inspection Result Filter
        System.out.println("3. Select Inspection Result filter:");
        System.out.println("   [1] ALL Inspections (Pass & Fail)");
        System.out.println("   [2] Approved (PASS) Inspections Only");
        System.out.println("   [3] Quality Defects (FAIL) Inspections Only (Re-clean Required)");
        System.out.print("   Enter choice (1-3) [Default 1]: ");
        String resChoice = scanner.nextLine().trim();
        String resultFilter = switch (resChoice) {
            case "2" -> "PASSED";
            case "3" -> "FAILED";
            default -> "ALL";
        };

        // Sorting Technique Selection
        System.out.println("\nSelect Sorting Technique / Metric Order:");
        System.out.println("   [1] Sort by Inspection Outcome (Quality Defects/Fail First) [InsertionSort Algorithm]");
        System.out.println("   [2] Sort by Inspection Outcome (Approved/Pass First) [InsertionSort Algorithm]");
        System.out.println("   [3] Sort by Room Number (Ascending) [QuickSort Algorithm]");
        System.out.println("   [4] Sort by Supervisor ID (Ascending) [InsertionSort Algorithm]");
        System.out.println("   [5] Sort by Inspection Timestamp (Most Recent First) [QuickSort Algorithm]");
        System.out.print("   Enter sorting choice (1-5) [Default 1]: ");
        int sortChoice = 1;
        try {
            String sInput = scanner.nextLine().trim();
            if (!sInput.isEmpty()) {
                sortChoice = Integer.parseInt(sInput);
            }
        } catch (NumberFormatException e) {
            sortChoice = 1;
        }

        controller.displayRoomHygieneReport(supervisorId, roomType, resultFilter, sortChoice);
    }
}


