package entity;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class Booking {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_COMPLETED = "COMPLETED";

    private String confirmationNo;
    private Guest guest;
    private Room room;
    private String roomType;
    private String roomNo;
    private double pricePerNight;
    private int numGuests;
    private LocalDateTime checkInDateTime;
    private LocalDateTime checkOutDateTime;
    private LocalDateTime registeredAt;
    private String bookingStatus;

    public Booking(String confirmationNo, Guest guest, String roomType, double pricePerNight, int numGuests, LocalDateTime checkInDateTime, LocalDateTime checkOutDateTime, LocalDateTime registeredAt) {
        this.confirmationNo = confirmationNo;
        this.guest = guest;
        this.roomType = roomType;
        this.pricePerNight = pricePerNight;
        this.numGuests = numGuests;
        if (room != null) {
            this.roomType = room.getRoomType();
            this.pricePerNight = room.getPricePerNight();
        }
        this.checkInDateTime = checkInDateTime;
        this.checkOutDateTime = checkOutDateTime;
        this.registeredAt = registeredAt;
        this.status = STATUS_PENDING;
    }

    public String getConfirmationNo() { return confirmationNo; }
    public void setConfirmationNo(String confirmationNo) { this.confirmationNo = confirmationNo; }

    public String getBookingId() { return confirmationNo; }
    public void setBookingId(String bookingId) { this.confirmationNo = bookingId; }

    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }

    public Room getRoom() { return room; }
    public void setRoom(Room room) {
        this.room = room;
        if (room != null) {
            this.roomType = room.getRoomType();
            this.pricePerNight = room.getPricePerNight();
        }
    }

    public String getRoomType() {
        if (room != null) return room.getRoomType();
        return roomType;
    }
    public void setRoomType(String roomType) { this.roomType = roomType; }
    
    public String getRoomNo() { return roomNo; }
    public void setRoomNo(String roomNo) { this.roomNo = roomNo;}

    public double getPricePerNight() {
        if (room != null) return room.getPricePerNight();
        return pricePerNight;
    }
    public void setPricePerNight(double pricePerNight) { this.pricePerNight = pricePerNight; }

    public int getNumGuests() { return numGuests; }
    public void setNumGuests(int numGuests) { this.numGuests = numGuests; }

    public LocalDateTime getCheckin() { return checkInDateTime; }
    public LocalDateTime getCheckInDateTime() { return checkInDateTime; }
    public void setCheckInDateTime(LocalDateTime checkInDateTime) { this.checkInDateTime = checkInDateTime; }

    public LocalDateTime getCheckout() { return checkOutDateTime; }
    public LocalDateTime getCheckOutDateTime() { return checkOutDateTime; }
    public void setCheckOutDateTime(LocalDateTime checkOutDateTime) { this.checkOutDateTime = checkOutDateTime; }

    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }

    public String getBookingStatus() { return bookingStatus; }
    public void setBookingStatus(String bookingStatus) { this.bookingStatus = bookingStatus; }

    public int getNights() {
        if (checkInDateTime == null || checkOutDateTime == null) return 1;
        long days = ChronoUnit.DAYS.between(checkInDateTime.toLocalDate(), checkOutDateTime.toLocalDate());
        return (int) Math.max(days, 1);
    }

    public double getTotalPrice() {
        return getPricePerNight() * getNights();
    }

    @Override
    public String toString() {
        String guestName = (guest != null) ? guest.getGuestName() : "None";
        String roomInfo = (room != null) ? room.getRoomNo() : (roomType != null ? roomType : "Unassigned");
        return String.format(
                "Booking[%s] Guest=%s Room=%s Guests=%d CheckIn=%s CheckOut=%s Nights=%d Total=RM%.2f Status=%s",
                confirmationNo, guestName, roomInfo, numGuests, checkInDateTime, checkOutDateTime,
                getNights(), getTotalPrice(), status);
    }
}
