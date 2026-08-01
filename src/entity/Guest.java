package entity;

public class Guest {
    private String guestId;
    private String guestName;
    private String contactNumber;
    private String icPassport;

    public Guest() {
    }

    public Guest(String guestId, String guestName, String contactNumber, String icPassport) {
        this.guestId = guestId;
        this.guestName = guestName;
        this.contactNumber = contactNumber;
        this.icPassport = icPassport;
    }

    public Guest(String guestId, String guestName, String contactNumber) {
        this(guestId, guestName, contactNumber, "N/A");
    }

    public String getGuestId() { return guestId; }
    public void setGuestId(String guestId) { this.guestId = guestId; }

    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }
    
    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getIcPassport() { return icPassport; }
    public void setIcPassport(String icPassport) { this.icPassport = icPassport; }

    @Override
    public String toString() {
        return "Guest{id='" + guestId + "', name='" + guestName + "', contactNumber='" + contactNumber + "', icPassport='" + icPassport + "'}";
    }
}
