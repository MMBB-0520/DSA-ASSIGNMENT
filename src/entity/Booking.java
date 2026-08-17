package entity;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class Booking implements Comparable<Booking> {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_RESERVED = "RESERVED";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_CHECKED_IN = "CHECKED_IN";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_COMPLETED = "COMPLETED";

    public static final String PAYMENT_UNPAID = Bill.PAYMENT_UNPAID;
    public static final String PAYMENT_PAID_CASH = Bill.PAYMENT_PAID_CASH;
    public static final String METHOD_CASH = Bill.METHOD_CASH;

    private String confirmationNo;
    private Guest guest;
    private String roomType;
    private String roomNo;
    private double pricePerNight;
    private int numGuests;
    private LocalDateTime checkInDateTime;
    private LocalDateTime checkOutDateTime;
    private LocalDateTime registeredAt;
    private String status;

    // Billing composition
    private Bill bill;

    public Booking(String confirmationNo, Guest guest, String roomType, double pricePerNight, int numGuests,
            LocalDateTime checkInDateTime, LocalDateTime checkOutDateTime, LocalDateTime registeredAt) {
        this.confirmationNo = confirmationNo;
        this.guest = guest;
        this.roomType = roomType;
        this.pricePerNight = pricePerNight;
        this.numGuests = numGuests;
        this.checkInDateTime = checkInDateTime;
        this.checkOutDateTime = checkOutDateTime;
        this.registeredAt = registeredAt;
        this.status = STATUS_PENDING;
        this.bill = new Bill("INV-" + (confirmationNo != null ? confirmationNo : "00000000"), pricePerNight, getNights());
    }

    public Bill getBill() {
        if (bill == null) {
            bill = new Bill("INV-" + (confirmationNo != null ? confirmationNo : "00000000"), pricePerNight, getNights());
        } else {
            bill.setPricePerNight(pricePerNight);
            bill.setNights(getNights());
        }
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public String getConfirmationNo() {
        return confirmationNo;
    }

    public void setConfirmationNo(String confirmationNo) {
        this.confirmationNo = confirmationNo;
    }

    public Guest getGuest() {
        return guest;
    }

    public void setGuest(Guest guest) {
        this.guest = guest;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    public double getPricePerNight() {
        return pricePerNight;
    }

    public void setPricePerNight(double pricePerNight) {
        this.pricePerNight = pricePerNight;
    }

    public int getNumGuests() {
        return numGuests;
    }

    public void setNumGuests(int numGuests) {
        this.numGuests = numGuests;
    }

    public LocalDateTime getCheckin() {
        return checkInDateTime;
    }

    public LocalDateTime getCheckInDateTime() {
        return checkInDateTime;
    }

    public void setCheckInDateTime(LocalDateTime checkInDateTime) {
        this.checkInDateTime = checkInDateTime;
    }

    public LocalDateTime getCheckout() {
        return checkOutDateTime;
    }

    public LocalDateTime getCheckOutDateTime() {
        return checkOutDateTime;
    }

    public void setCheckOutDateTime(LocalDateTime checkOutDateTime) {
        this.checkOutDateTime = checkOutDateTime;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getNights() {
        if (checkInDateTime == null || checkOutDateTime == null)
            return 1;
        long days = ChronoUnit.DAYS.between(checkInDateTime.toLocalDate(), checkOutDateTime.toLocalDate());
        return (int) Math.max(days, 1);
    }

    /**
     * Determines late check-out option automatically
     */
    public int determineLateCheckOutOption() {
        if (getBill().getRequestedLateOption() > 0) {
            return getBill().getRequestedLateOption();
        }
        if (this.checkOutDateTime == null) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        if (!now.isAfter(this.checkOutDateTime)) {
            return 0; // On time / Grace Period
        }

        long minutesOver = ChronoUnit.MINUTES.between(this.checkOutDateTime, now);

        if (minutesOver <= 30) {
            return 0; // <= 30 mins: Grace Period (Free)
        } else if (minutesOver <= 120) {
            return 1; // 30 mins - 2 hours: +25%
        } else if (minutesOver <= 240) {
            return 2; // 2 - 4 hours: +50%
        } else {
            return 3; // > 4 hours: +100% (Extra 1 night fee charged as Late Charge)
        }
    }

    /**
     * Calculates Late Check-Out Charge based on lateOption
     */
    public double calculateLateCheckOutCharge(int lateOption) {
        return switch (lateOption) {
            case 1 -> getPricePerNight() * 0.25;
            case 2 -> getPricePerNight() * 0.50;
            case 3 -> getPricePerNight() * 1.00;
            default -> 0.0;
        };
    }

    /**
     * Computes the effective late check-out charge for this booking.
     */
    public double getLateCheckOutCharge() {
        return calculateLateCheckOutCharge(determineLateCheckOutOption());
    }

    @Override
    public int compareTo(Booking other) {
        if (other == null || other.confirmationNo == null) {
            return 1;
        }
        if (this.confirmationNo == null) {
            return -1;
        }
        return this.confirmationNo.compareTo(other.confirmationNo);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Booking booking = (Booking) obj;
        return confirmationNo != null && confirmationNo.equals(booking.confirmationNo);
    }

    @Override
    public String toString() {
        String guestName = (guest != null) ? guest.getGuestName() : "None";
        String roomInfo = (roomNo != null && !roomNo.isEmpty()) ? roomNo : (roomType != null ? roomType : "Unassigned");
        return String.format(
                "Booking[%s] Guest=%s Room=%s Guests=%d CheckIn=%s CheckOut=%s Nights=%d Total=RM%.2f Status=%s Payment=%s",
                confirmationNo, guestName, roomInfo, numGuests, checkInDateTime, checkOutDateTime,
                getNights(), getBill().getGrandTotal(), status, getBill().getPaymentStatus());
    }
}
