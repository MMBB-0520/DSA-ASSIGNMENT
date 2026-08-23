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
    private Guest guest;
    private String roomType;
    private double pricePerNight;
    private int numGuests;
    private LocalDateTime checkInDateTime;
    private LocalDateTime checkOutDateTime;
    private LocalDateTime registeredAt;
    private String status;

    public Booking(String bookingId, Guest guest, String roomType, double pricePerNight,
                   int numGuests, LocalDateTime checkInDateTime, LocalDateTime checkOutDateTime,
                   LocalDateTime registeredAt) {
        this.bookingId = bookingId;
        this.guest = guest;
        this.roomType = roomType;
        this.pricePerNight = pricePerNight;
        this.numGuests = numGuests;
        this.checkInDateTime = checkInDateTime;
        this.checkOutDateTime = checkOutDateTime;
        this.registeredAt = registeredAt;
        this.status = STATUS_AVAILABLE;
    }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public Guest getGuest() { return guest; }
    public void setGuest(Guest guest) { this.guest = guest; }

    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }

    public double getPricePerNight() { return pricePerNight; }
    public void setPricePerNight(double pricePerNight) { this.pricePerNight = pricePerNight; }

    public int getNumGuests() { return numGuests; }
    public void setNumGuests(int numGuests) { this.numGuests = numGuests; }

    public LocalDateTime getCheckInDateTime() { return checkInDateTime; }
    public void setCheckInDateTime(LocalDateTime checkInDateTime) { this.checkInDateTime = checkInDateTime; }

    public LocalDateTime getCheckOutDateTime() { return checkOutDateTime; }
    public void setCheckOutDateTime(LocalDateTime checkOutDateTime) { this.checkOutDateTime = checkOutDateTime; }

    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // Derived: number of nights between check-in and check-out (minimum 1)
    public int getNights() {
        long days = ChronoUnit.DAYS.between(checkInDateTime.toLocalDate(), checkOutDateTime.toLocalDate());
        return (int) Math.max(days, 1);
    }

    public double getTotalPrice() {
        return pricePerNight * getNights();
    }

    @Override
    public String toString() {
        return String.format(
                "Booking[%s] Guest=%s RoomType=%s Guests=%d CheckIn=%s CheckOut=%s Nights=%d Total=RM%.2f Status=%s",
                bookingId, guest.getName(), roomType, numGuests, checkInDateTime, checkOutDateTime,
                getNights(), getTotalPrice(), status);
    }
}
