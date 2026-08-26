import boundary.FrontDeskServiceUI;
import boundary.HousekeepingUI;
import boundary.WalkInRegistrationUI;
import util.InputUtil;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int choice = -1;

        do {
            System.out.println("    _____  _    ____  _   _ __  __ _____    ");
            System.out.println("   |_   _|/ \\  |  _ \\| | | |  \\/  |_   _|   ");
            System.out.println("     | | / _ \\ | |_) | | | | |\\/| | | |     ");
            System.out.println("     | |/ ___ \\|  _ <| |_| | |  | | | |     ");
            System.out.println("  ___|_/_/___\\_\\_| \\_\\\\___/|_| _|_|_|_|__   ");
            System.out.println(" |  _ \\| ____/ ___| / _ \\|  _ \\_   _/ ___|  ");
            System.out.println(" | |_) |  _| \\___ \\| | | | |_) || | \\___ \\  ");
            System.out.println(" |  _ <| |___ ___) | |_| |  _ < | |  ___) | ");
            System.out.println(" |_| \\_\\_____|____/ \\___/|_| \\_\\|_| |____/ \n");
            System.out.println("╔═════════════════════════════════════════╗");
            System.out.println("║            MANAGEMENT SYSTEM            ║");
            System.out.println("╚═════════════════════════════════════════╝");
            System.out.println("1. Walk-In Registration");
            System.out.println("2. Housekeeping Task Log");
            System.out.println("3. Front Desk Service");
            System.out.println("0. Exit\n");
            choice = InputUtil.readIntInRange(sc, "Enter Choice [0-3]: ", 0, 3);

            switch (choice) {
                case 1 -> {
                    WalkInRegistrationUI walkInUI = new WalkInRegistrationUI();
                    walkInUI.displayMenu();
                }
                case 2 -> {
                    HousekeepingUI housekeepingUI = new HousekeepingUI();
                    housekeepingUI.runMenu();
                }
                case 3 -> {
                    FrontDeskServiceUI searchUI = new FrontDeskServiceUI();
                    searchUI.runMenu();
                }
                case 0 -> System.out.println("\nThank you for using Hotel Management System. Goodbye!");
                default -> System.out.println("\nInvalid choice! Please enter a valid menu option.");
            }
        } while (choice != 0);

        sc.close();
    }
}
