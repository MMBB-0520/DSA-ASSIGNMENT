// Author: <your name>
package entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Booking {
    private String bookingId;
    private Guest guest;
    private String roomType;
    private double pricePerNight;
    private int numGuests;
    private LocalDate checkInDate;
    private int nights;
    private LocalDateTime registeredAt;
    private String status; // "WAITING", "ASSIGNED", "CANCELLED"

    public Booking(String bookingId, Guest guest, String roomType, double pricePerNight,
                   int numGuests, LocalDate checkInDate, int nights, LocalDateTime registeredAt) {
        this.bookingId = bookingId;
        this.guest = guest;
        this.roomType = roomType;
        this.pricePerNight = pricePerNight;
        this.numGuests = numGuests;
        this.checkInDate = checkInDate;
        this.nights = nights;
        this.registeredAt = registeredAt;
        this.status = "WAITING";
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

    public LocalDate getCheckInDate() { return checkInDate; }
    public void setCheckInDate(LocalDate checkInDate) { this.checkInDate = checkInDate; }

    public int getNights() { return nights; }
    public void setNights(int nights) { this.nights = nights; }

    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // Total price for the whole stay
    public double getTotalPrice() {
        return pricePerNight * nights;
    }

    @Override
    public String toString() {
        return String.format("Booking[%s] Guest=%s RoomType=%s Guests=%d CheckIn=%s Nights=%d Total=RM%.2f Status=%s",
                bookingId, guest.getName(), roomType, numGuests, checkInDate, nights, getTotalPrice(), status);
    }
}
