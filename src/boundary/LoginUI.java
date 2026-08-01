package boundary;

import control.WalkInRegistrationControl;
import entity.Staff;

import java.util.Scanner;

public class LoginUI {

    private WalkInRegistrationControl control;
    private Scanner sc;

    public LoginUI() {
        control = new WalkInRegistrationControl();
        sc = new Scanner(System.in);
    }

    public void start() {
        System.out.println("=== Hotel Management System ===");
        while (true) {
            System.out.println("\n--- Login ---");
            System.out.print("Staff ID (or 'q' to quit): ");
            String staffId = sc.nextLine().trim();
            if (staffId.equalsIgnoreCase("q")) {
                System.out.println("Exiting system...");
                break;
            }

            System.out.print("Password: ");
            String password = sc.nextLine().trim();

            Staff staff = control.authenticate(staffId, password);

            if (staff != null) {
                System.out.println("Login successful. Welcome, " + staff.getUsername() + " (" + staff.getRole() + ")");
                handleRoleMenu(staff);
            } else {
                System.out.println("Invalid staff ID or password. Please try again.");
            }
        }
    }

    private void handleRoleMenu(Staff staff) {
        String role = staff.getRole();
        if (role.equals("Front Desk Staff") || role.equals("Manager")) {
            WalkInRegistrationUI ui = new WalkInRegistrationUI(control, staff);
            ui.runMenu();
        } else if (role.equals("Housekeeping Staff") || role.equals("Housekeeping Supervisor")) {
            // TODO: Jia Yie
            System.out.println("\n=== Housekeeping Menu ===");
            System.out.println("No tasks available at the moment.");
            System.out.println("Press Enter to log out...");
            sc.nextLine();
        } else {
            System.out.println("Role not recognized.");
        }
    }

    public static void main(String[] args) {
        LoginUI loginUI = new LoginUI();
        loginUI.start();
    }
}
