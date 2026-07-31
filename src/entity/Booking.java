// Author: <your name>
package entity;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class Booking {

    // Status lifecycle: AVAILABLE (waiting) -> BOOKED (room assigned)
    //                    -> DIRTY (guest checked out, needs cleaning)
    //                 or -> CANCELLED (dropped while waiting)
    public static final String STATUS_AVAILABLE = "AVAILABLE";
    public static final String STATUS_BOOKED = "BOOKED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_DIRTY = "DIRTY";

    private String bookingId;
    private String guestName;
    private String guestIcPassport;
    private String guestContact;
    private Staff staff;
    private String roomId;
    private transient Room room;
    private int numGuests;
    private LocalDateTime checkInDateTime;
    private LocalDateTime checkOutDateTime;
    private LocalDateTime registeredAt;
    private String bookingStatus;

    public Booking(String bookingId, String guestName, String guestIcPassport, String guestContact, Staff staff, Room room,
                   int numGuests, LocalDateTime checkInDateTime, LocalDateTime checkOutDateTime,
                   LocalDateTime registeredAt) {
        this.bookingId = bookingId;
        this.guestName = guestName;
        this.guestIcPassport = guestIcPassport;
        this.guestContact = guestContact;
        this.staff = staff;
        this.room = room;
        this.roomId = room.getRoomId();
        this.numGuests = numGuests;
        this.checkInDateTime = checkInDateTime;
        this.checkOutDateTime = checkOutDateTime;
        this.registeredAt = registeredAt;
        this.bookingStatus = STATUS_AVAILABLE;
    }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }

    public String getGuestIcPassport() { return guestIcPassport; }
    public void setGuestIcPassport(String guestIcPassport) { this.guestIcPassport = guestIcPassport; }

    public String getGuestContact() { return guestContact; }
    public void setGuestContact(String guestContact) { this.guestContact = guestContact; }

    public Staff getStaff() { return staff; }
    public void setStaff(Staff staff) { this.staff = staff; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public Room getRoom() { return room; }
    public void setRoom(Room room) { 
        this.room = room; 
        if (room != null) {
            this.roomId = room.getRoomId();
        }
    }

    public int getNumGuests() { return numGuests; }
    public void setNumGuests(int numGuests) { this.numGuests = numGuests; }

    public LocalDateTime getCheckInDateTime() { return checkInDateTime; }
    public void setCheckInDateTime(LocalDateTime checkInDateTime) { this.checkInDateTime = checkInDateTime; }

    public LocalDateTime getCheckOutDateTime() { return checkOutDateTime; }
    public void setCheckOutDateTime(LocalDateTime checkOutDateTime) { this.checkOutDateTime = checkOutDateTime; }

    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }

    public String getBookingStatus() { return bookingStatus; }
    public void setBookingStatus(String bookingStatus) { this.bookingStatus = bookingStatus; }

    // Derived: number of nights between check-in and check-out (minimum 1)
    public int getNights() {
        long days = ChronoUnit.DAYS.between(checkInDateTime.toLocalDate(), checkOutDateTime.toLocalDate());
        return (int) Math.max(days, 1);
    }

    public double getTotalPrice() {
        if (room == null)
            return 0;
        return room.getPrice() * getNights();
    }

    @Override
    public String toString() {
        String staffStr = staff != null ? staff.getUsername() : "N/A";
        String roomStr = room != null ? room.getType() + " (" + room.getNumber() + ")" : "N/A";
        return String.format(
                "Booking[%s] Guest=%s(IC:%s, Tel:%s) Staff=%s Room=%s Guests=%d CheckIn=%s CheckOut=%s Nights=%d Total=RM%.2f Status=%s",
                bookingId, guestName, guestIcPassport, guestContact, staffStr, roomStr, numGuests, checkInDateTime, checkOutDateTime,
                getNights(), getTotalPrice(), bookingStatus);
    }
}
