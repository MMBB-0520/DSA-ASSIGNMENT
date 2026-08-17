import boundary.FrontDeskServiceUI;
import boundary.HousekeepingUI;
import boundary.WalkInRegistrationUI;
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
            System.out.println("3. Front Desk Search");
            System.out.println("0. Exit\n");
            System.out.print("Enter Choice: ");

            try {
                String input = sc.nextLine().trim();
                choice = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                choice = -1;
            }

            switch (choice) {
                case 1 -> {
                    WalkInRegistrationUI walkInUI = new WalkInRegistrationUI();
                    walkInUI.runMenu();
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
                default -> System.out.println("\nInvalid choice! Please enter a number between 1 and 4.");
            }
        } while (choice != 0);

        sc.close();
    }
}
