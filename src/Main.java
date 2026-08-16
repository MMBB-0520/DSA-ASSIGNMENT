import boundary.FrontDeskSearchUI;
import boundary.HousekeepingUI;
import boundary.WalkInRegistrationUI;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int choice = -1;

        do {
            System.out.println("\n**************************************************");
            System.out.println("*             HOTEL MANAGEMENT SYSTEM            *");
            System.out.println("**************************************************");
            System.out.println("* 1. Walk-In Registration                        *");
            System.out.println("* 2. Housekeeping Task Log                       *");
            System.out.println("* 3. Front Desk Search                           *");
            System.out.println("* 0. Exit System                                 *");
            System.out.println("**************************************************");
            System.out.print("Please enter your input: ");

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
                    FrontDeskSearchUI searchUI = new FrontDeskSearchUI();
                    searchUI.runMenu();
                }
                case 0 -> System.out.println("\nThank you for using Hotel Management System. Goodbye!");
                default -> System.out.println("\n[!] Invalid choice! Please enter a number between 0 and 3.");
            }
        } while (choice != 0);

        sc.close();
    }
}
