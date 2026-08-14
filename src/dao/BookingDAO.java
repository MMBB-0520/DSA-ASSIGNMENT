package dao;

import entity.Booking;

/**
 * Data Access Object (DAO) Interface for Booking entity persistence.
 * Best Practice: Restricts caller to abstract DAO contract methods.
 */
public interface BookingDAO {
    Booking[] getAllBookings();
    void saveAllBookings(Booking[] bookings);
    Booking getBookingByConfirmationNo(String confirmationNo);
    void saveBooking(Booking booking);
}
