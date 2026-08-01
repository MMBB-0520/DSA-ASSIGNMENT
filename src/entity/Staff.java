package entity;

public class Staff {
    private String staffId;
    private String name;
    private String role;

    public Staff() {
    }

    public Staff(String staffId, String name, String role) {
        this.staffId = staffId;
        this.name = name;
        this.role = role;
    }

    public String getStaffId() { return staffId; }
    public void setStaffId(String staffId) { this.staffId = staffId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    @Override
    public String toString() {
        return "Staff{id='" + staffId + "', name='" + name + "', role='" + role + "'}";
    }
}
