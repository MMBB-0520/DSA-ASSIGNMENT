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
                case 5 -> controller.displayOperationalReport();
                case 6 -> controller.undoLastAction();
                case 0 -> System.out.println("\nReturning to Main Menu...");
                default -> System.out.println("\n[!] Invalid choice! Please enter a number between 0 and 6.");
            }
        } while (choice != 0);
    }

    private void displayMenu() {
        System.out.println("\n**************************************************");
        System.out.println("*              HOUSEKEEPING MODULE               *");
        System.out.println("**************************************************");
        System.out.println("* 1. View Dirty Rooms (Awaiting Cleaning)          *");
        System.out.println("* 2. Start Cleaning (Dirty -> In Progress)       *");
        System.out.println("* 3. Complete Cleaning (In Progress -> Cleaned)  *");
        System.out.println("* 4. Inspect Room & Mark Ready for Check-In      *");
        System.out.println("* 5. View Housekeeping Task History / Report     *");
        System.out.println("* 6. Rollback Last Action (Undo)                 *");
        System.out.println("* 0. Back to Main Menu                           *");
        System.out.println("**************************************************");
        System.out.print("Please enter your input: ");
    }

    /**
     * Displays rooms with status 'Dirty' (checked-out rooms ready to be cleaned by housekeepers).
     */
    private void viewRoomsStatus() {
        System.out.println("\n===============================================================================");
        System.out.println("                 DIRTY ROOMS AWAITING HOUSEKEEPING CLEANING                    ");
        System.out.println("===============================================================================");
        System.out.printf("%-10s %-20s %-15s %-18s %-15s\n", "Room No.", "Room Type", "Price (RM)", "Current Status", "Std Duration");
        System.out.println("-------------------------------------------------------------------------------");
        Room[] dirtyRooms = controller.getDirtyRooms();
        if (dirtyRooms.length == 0) {
            System.out.println(" [i] Notice: No dirty rooms found. All rooms are clean or currently occupied!");
        } else {
            for (Room r : dirtyRooms) {
                int stdMins = controller.getStandardCleaningDurationMinutes(r.getRoomType());
                System.out.printf("%-10s %-20s %-15.2f %-18s %-15s\n", 
                        r.getRoomNo(), r.getRoomType(), r.getPricePerNight(), r.getStatus(), stdMins + " mins");
            }
        }
        System.out.println("===============================================================================\n");
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
            System.out.println("[!] Notice: Room " + roomNo + " is currently '" + room.getStatus() + "'. Only 'Dirty' rooms can be started.");
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
            System.out.println("[!] Notice: Room " + roomNo + " is currently '" + room.getStatus() + "'. Only 'Cleaning In Progress' rooms can be completed.");
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
            System.out.println("[!] Notice: Room " + roomNo + " is currently '" + room.getStatus() + "'. Only cleaned rooms can be inspected.");
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
}
