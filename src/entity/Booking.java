package entity;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class Booking implements Comparable<Booking> {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_CHECKED_IN = "CHECKED_IN";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_COMPLETED = "COMPLETED";

    public static final String PAYMENT_UNPAID = "UNPAID";
    public static final String PAYMENT_PAID_CASH = "PAID (CASH)";
    public static final String METHOD_CASH = "CASH";

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

    // Billing & Payment Attributes directly inside Booking
    private String paymentStatus;
    private String invoiceNo;
    private String paymentMethod;
    private double otherCharges;
    private double cashTendered;
    private double changeDue;

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

        // Default billing attributes
        this.paymentStatus = PAYMENT_UNPAID;
        this.paymentMethod = METHOD_CASH;
        this.invoiceNo = "INV-" + (confirmationNo != null ? confirmationNo : "00000000");
        this.otherCharges = 80.00;
        this.cashTendered = 0.0;
        this.changeDue = 0.0;
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

    // Billing & Payment Methods
    public String getPaymentStatus() {
        if (paymentStatus == null || paymentStatus.isEmpty()) return PAYMENT_UNPAID;
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getInvoiceNo() {
        if (invoiceNo == null || invoiceNo.isEmpty()) return "INV-" + (confirmationNo != null ? confirmationNo : "00000000");
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public String getPaymentMethod() {
        if (paymentMethod == null || paymentMethod.isEmpty()) return METHOD_CASH;
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public double getOtherCharges() {
        return otherCharges;
    }

    public void setOtherCharges(double otherCharges) {
        this.otherCharges = otherCharges;
    }

    public double getCashTendered() {
        return cashTendered;
    }

    public void setCashTendered(double cashTendered) {
        this.cashTendered = cashTendered;
    }

    public double getChangeDue() {
        return changeDue;
    }

    public void setChangeDue(double changeDue) {
        this.changeDue = changeDue;
    }

    public double getRoomCharge() {
        return getPricePerNight() * getNights();
    }

    public double getServiceCharge() {
        return getRoomCharge() * 0.10;
    }

    public double getGrandTotal() {
        return getRoomCharge() + getServiceCharge() + getOtherCharges();
    }

    public double getTotalPrice() {
        return getGrandTotal();
    }

    public double getBookingDeposit() {
        return getRoomCharge() * 0.30;
    }

    public double getSecurityDeposit() {
        return 200.00;
    }

    public double getTotalCheckInDeposit() {
        return getBookingDeposit() + getSecurityDeposit();
    }

    public boolean processCashPayment(double cashTendered) {
        if (cashTendered < getGrandTotal()) {
            return false;
        }
        this.cashTendered = cashTendered;
        this.changeDue = cashTendered - getGrandTotal();
        this.paymentStatus = PAYMENT_PAID_CASH;
        this.paymentMethod = METHOD_CASH;
        return true;
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
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
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
                getNights(), getTotalPrice(), status, getPaymentStatus());
    }
}
