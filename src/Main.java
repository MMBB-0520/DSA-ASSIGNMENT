import boundary.FrontDeskSearchUI;
import boundary.WalkInRegistrationUI;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int choice = -1;

        do {
            System.out.println("\n=================================");
            System.out.println(" HOTEL MANAGEMENT SYSTEM");
            System.out.println("=================================");
            System.out.println();
            System.out.println("1. Walk-In Registration");
            System.out.println("2. Housekeeping Task Log");
            System.out.println("3. Front Desk Search");
            System.out.println("4. Exit");
            System.out.println();
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
                case 2 -> System.out.println("\n[Housekeeping Task Log module is under development]");
                case 3 -> {
                    FrontDeskSearchUI searchUI = new FrontDeskSearchUI();
                    searchUI.runMenu();
                }
                case 4 -> System.out.println("\nThank you for using Hotel Management System. Goodbye!");
                default -> System.out.println("\nInvalid choice! Please enter a number between 1 and 4.");
            }
        } while (choice != 4);
        
        sc.close();
    }
}
