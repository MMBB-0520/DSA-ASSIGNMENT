package boundary;

import control.StaffControl;
import entity.Staff;
import java.util.Scanner;

public class LoginUI {
    private Scanner scanner;

    public LoginUI() {
        this.scanner = new Scanner(System.in);
    }

    public Staff promptLogin() {
        System.out.println("\n**************************************************");
        System.out.println("*                STAFF LOGIN SYSTEM              *");
        System.out.println("**************************************************");
        System.out.print("Please enter Staff ID: ");
        String staffId = scanner.nextLine().trim();

        System.out.print("Please enter Password: ");
        String password = scanner.nextLine().trim();

        Staff staff = StaffControl.authenticate(staffId, password, null);
        if (staff != null) {
            System.out.println("\n[√] Login Successful! Welcome, " + staff.getName() + " (" + staff.getRole() + ")");
            return staff;
        } else {
            System.out.println("[!] Login failed. Please check your credentials.");
            return null;
        }
    }

    public boolean displayLoginScreen() {
        int choice = -1;
        do {
            System.out.println("\n**************************************************");
            System.out.println("*                STAFF LOGIN SYSTEM              *");
            System.out.println("**************************************************");
            System.out.println("* 1. Staff Login                                 *");
            System.out.println("* 0. Exit System                                 *");
            System.out.println("**************************************************");
            System.out.print("Please enter your input: ");

            try {
                String input = scanner.nextLine().trim();
                choice = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                choice = -1;
            }

            switch (choice) {
                case 1 -> {
                    Staff staff = promptLogin();
                    if (staff != null) {
                        return true;
                    }
                }
                case 0 -> {
                    System.out.println("\nThank you for using Hotel Management System. Goodbye!");
                    return false;
                }
                default -> System.out.println("\n[!] Invalid choice! Please enter 1 or 0.");
            }
        } while (choice != 0);

        return false;
    }
}
