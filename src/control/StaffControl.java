package control;

import entity.Staff;
import java.util.ArrayList;
import java.util.List;

public class StaffControl {
    private static List<Staff> staffList = new ArrayList<>();

    static {
        staffList.add(new Staff("S001", "Alice", "Housekeeper", "pass123"));
        staffList.add(new Staff("S002", "Bob", "Housekeeper", "pass123"));
        staffList.add(new Staff("S003", "Charlie", "Housekeeper", "pass123"));
        staffList.add(new Staff("S004", "David", "Housekeeper", "pass123"));
        staffList.add(new Staff("S005", "Emma", "Housekeeper", "pass123"));
        staffList.add(new Staff("S006", "Frank", "Housekeeper", "pass123"));
        staffList.add(new Staff("S007", "Grace", "Housekeeper", "pass123"));
        staffList.add(new Staff("S008", "Hannah", "Housekeeper", "pass123"));
        staffList.add(new Staff("S009", "Ian", "Housekeeper", "pass123"));
        staffList.add(new Staff("S010", "Jack", "Housekeeper", "pass123"));
        staffList.add(new Staff("SP001", "Sarah", "Supervisor", "admin123"));
        staffList.add(new Staff("SP002", "Tom", "Supervisor", "admin123"));
        staffList.add(new Staff("F001", "Diana", "Front Desk", "front123"));
        staffList.add(new Staff("F002", "Eve", "Front Desk", "front123"));
    }

    public static Staff authenticate(String staffId, String password, String requiredRole) {
        for (Staff staff : staffList) {
            if (staff.getStaffId().equalsIgnoreCase(staffId) && staff.getPassword().equals(password)) {
                if (requiredRole == null || staff.getRole().equalsIgnoreCase(requiredRole)) {
                    return staff;
                } else {
                    System.out.println("Error: Insufficient privileges. Required role: " + requiredRole);
                    return null;
                }
            }
        }
        System.out.println("Error: Invalid credentials.");
        return null;
    }
}
