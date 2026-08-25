package entity;

/**
 * Bill Entity Class.
 * Encapsulates all financial state (rate, nights, charges, deposits, payment
 * status)
 * and provides clean domain calculations without requiring external parameters.
 */
public class Bill {

    public static final String PAYMENT_UNPAID = "UNPAID";
    public static final String PAYMENT_PAID_CASH = "PAID";
    public static final String METHOD_CASH = "CASH";

    private String invoiceNo;
    private double pricePerNight;
    private int nights;
    private int numOfRooms = 1;
    private String paymentStatus;
    private String paymentMethod;
    private double otherCharges;
    private double cashTendered;
    private double changeDue;
    private int requestedLateOption;

    public Bill() {
        this("INV-00000000", 0.0, 1, 1);
    }

    public Bill(String invoiceNo) {
        this(invoiceNo, 0.0, 1, 1);
    }

    public Bill(String invoiceNo, double pricePerNight, int nights) {
        this(invoiceNo, pricePerNight, nights, 1);
    }

    public Bill(String invoiceNo, double pricePerNight, int nights, int numOfRooms) {
        this.invoiceNo = (invoiceNo != null) ? invoiceNo : "INV-00000000";
        this.pricePerNight = pricePerNight;
        this.nights = Math.max(nights, 1);
        this.numOfRooms = Math.max(numOfRooms, 1);
        this.paymentMethod = METHOD_CASH;
        this.otherCharges = 80.00;
        this.cashTendered = 0.0;
        this.changeDue = 0.0;
        this.requestedLateOption = 0;
    }

    public String getInvoiceNo() {
        if (invoiceNo == null || invoiceNo.isEmpty()) {
            return "INV-00000000";
        }
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public double getPricePerNight() {
        return pricePerNight;
    }

    public void setPricePerNight(double pricePerNight) {
        this.pricePerNight = pricePerNight;
    }

    public int getNights() {
        return Math.max(nights, 1);
    }

    public void setNights(int nights) {
        this.nights = Math.max(nights, 1);
    }

    public int getNumOfRooms() {
        return Math.max(numOfRooms, 1);
    }

    public void setNumOfRooms(int numOfRooms) {
        this.numOfRooms = Math.max(numOfRooms, 1);
    }

    public String getPaymentStatus() {
        if (paymentStatus == null || paymentStatus.isEmpty()) {
            return PAYMENT_UNPAID;
        }
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentMethod() {
        if (paymentMethod == null || paymentMethod.isEmpty()) {
            return METHOD_CASH;
        }
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

    public int getRequestedLateOption() {
        return requestedLateOption;
    }

    public void setRequestedLateOption(int requestedLateOption) {
        this.requestedLateOption = requestedLateOption;
    }

    // Domain Calculations (Clean & Parameterless / Moment-of-Calculation)
    public double getRoomCharge() {
        return pricePerNight * getNights() * getNumOfRooms();
    }

    public double getServiceCharge() {
        return getRoomCharge() * 0.10;
    }

    public double getGrandTotal() {
        return getRoomCharge() + getServiceCharge() + getOtherCharges();
    }

    public double getBookingDeposit() {
        return getRoomCharge() * 0.30;
    }

    public double getSecurityDeposit() {
        return 200.00;
    }

    public boolean isDepositPaid() {
        return PAYMENT_PAID_CASH.equalsIgnoreCase(paymentStatus)
                || "DEPOSIT PAID".equalsIgnoreCase(paymentStatus)
                || (cashTendered > 0);
    }

    public double getRequiredCheckInDeposit() {
        if (isDepositPaid()) {
            return getSecurityDeposit();
        }
        return getBookingDeposit() + getSecurityDeposit();
    }

    public double getTotalCheckInDeposit() {
        return getBookingDeposit() + getSecurityDeposit();
    }

    public double calculateLateCheckOutCharge(int lateOption) {
        return switch (lateOption) {
            case 1 -> pricePerNight * 0.25;
            case 2 -> pricePerNight * 0.50;
            case 3 -> pricePerNight * 1.00;
            default -> 0.0;
        };
    }

    public double getSubtotalBill(double lateCharge) {
        return getGrandTotal() + lateCharge;
    }

    public double getNetDue(double lateCharge) {
        return getSubtotalBill(lateCharge) - getTotalCheckInDeposit();
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
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Bill bill = (Bill) obj;
        return invoiceNo != null && invoiceNo.equalsIgnoreCase(bill.invoiceNo);
    }

    @Override
    public String toString() {
        return String.format("Bill[%s] Status=%s Method=%s Cash=RM%.2f Change=RM%.2f",
                invoiceNo, paymentStatus, paymentMethod, cashTendered, changeDue);
    }
}
