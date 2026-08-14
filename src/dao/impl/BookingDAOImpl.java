package dao.impl;

import dao.BookingDAO;
import data.JsonManager;
import entity.Booking;

/**
 * Concrete JSON implementation of BookingDAO.
 */
public class BookingDAOImpl implements BookingDAO {

    @Override
    public Booking[] getAllBookings() {
        return JsonManager.loadBookings();
    }

    @Override
    public void saveAllBookings(Booking[] bookings) {
        JsonManager.saveBookings(bookings);
    }

    @Override
    public Booking getBookingByConfirmationNo(String confirmationNo) {
        if (confirmationNo == null)
            return null;
        Booking[] bookings = getAllBookings();
        for (Booking b : bookings) {
            if (b != null && confirmationNo.equalsIgnoreCase(b.getConfirmationNo())) {
                return b;
            }
        }
        return null;
    }

    @Override
    public void saveBooking(Booking booking) {
        if (booking == null || booking.getConfirmationNo() == null)
            return;
        Booking[] existing = getAllBookings();
        boolean found = false;
        for (int i = 0; i < existing.length; i++) {
            if (existing[i] != null && booking.getConfirmationNo().equalsIgnoreCase(existing[i].getConfirmationNo())) {
                existing[i] = booking;
                found = true;
                break;
            }
        }

        if (found) {
            saveAllBookings(existing);
        } else {
            Booking[] updated = new Booking[existing.length + 1];
            System.arraycopy(existing, 0, updated, 0, existing.length);
            updated[existing.length] = booking;
            saveAllBookings(updated);
        }
    }
}
