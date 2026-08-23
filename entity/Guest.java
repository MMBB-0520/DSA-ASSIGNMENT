// Author: <your name>
package entity;

public class Guest {
    private String guestId;
    private String name;
    private String contactNumber;

    public Guest(String guestId, String name, String contactNumber) {
        this.guestId = guestId;
        this.name = name;
        this.contactNumber = contactNumber;
    }

    public String getGuestId() { return guestId; }
    public void setGuestId(String guestId) { this.guestId = guestId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    @Override
    public String toString() {
        return "Guest{id=" + guestId + ", name=" + name + ", contact=" + contactNumber + "}";
    }
}
