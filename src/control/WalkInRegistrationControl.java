// Author: <your name>
package control;

import entity.Booking;
import entity.Guest;
import entity.MyQueue;
import java.time.LocalDateTime;

public class WalkInRegistrationControl {

    public static final String[] ROOM_TYPES = {"Standard", "Deluxe", "Suite"};
    private static final double[] ROOM_PRICES = {150.00, 250.00, 400.00}; // RM per night

    private MyQueue<Booking> bookingQueue;   // waiting to be assigned a room (status AVAILABLE)
    private MyQueue<Booking> processedLog;   // already assigned rooms (status BOOKED or DIRTY)
    private int bookingCounter;
    private int guestCounter;

    public WalkInRegistrationControl() {
        bookingQueue = new MyQueue<>();
        processedLog = new MyQueue<>();
        bookingCounter = 1;
        guestCounter = 1;
    }

    public double getPriceForRoomType(String roomType) {
        for (int i = 0; i < ROOM_TYPES.length; i++) {
            if (ROOM_TYPES[i].equalsIgnoreCase(roomType)) {
                return ROOM_PRICES[i];
            }
        }
        return -1;
    }

    // Register a new walk-in / standard booking - joins the back of the queue.
    // Status starts as AVAILABLE (waiting for a room to be assigned).
    public Booking registerBooking(String name, String contactNumber, String roomType, int numGuests,
                                    LocalDateTime checkInDateTime, LocalDateTime checkOutDateTime) {
        Guest guest = new Guest(generateGuestId(), name, contactNumber);
        String bookingId = generateBookingId();
        double price = getPriceForRoomType(roomType);
        Booking booking = new Booking(bookingId, guest, roomType, price, numGuests,
                checkInDateTime, checkOutDateTime, LocalDateTime.now());
        bookingQueue.enqueue(booking);
        return booking;
    }

    public boolean bookingExists(String bookingId) {
        for (int i = 0; i < bookingQueue.getSize(); i++) {
            if (bookingQueue.get(i).getBookingId().equalsIgnoreCase(bookingId)) {
                return true;
            }
        }
        return false;
    }

    // Process the next booking in arrival order: assigns a room (AVAILABLE -> BOOKED)
    // and moves it out of the waiting queue into the processed log.
    public Booking processNextBooking() {
        Booking next = bookingQueue.dequeue();
        if (next != null) {
            next.setStatus(Booking.STATUS_BOOKED);
            processedLog.enqueue(next);
        }
        return next;
    }

    // Cancel a booking that is still waiting (AVAILABLE -> CANCELLED)
    public Booking cancelBooking(String bookingId) {
        Booking removed = bookingQueue.removeById(bookingId);
        if (removed != null) {
            removed.setStatus(Booking.STATUS_CANCELLED);
        }
        return removed;
    }

    // Guest checks out of an already-booked room (BOOKED -> DIRTY, needs housekeeping)
    public Booking checkOutBooking(String bookingId) {
        for (int i = 0; i < processedLog.getSize(); i++) {
            Booking b = processedLog.get(i);
            if (b.getBookingId().equalsIgnoreCase(bookingId) && b.getStatus().equals(Booking.STATUS_BOOKED)) {
                b.setStatus(Booking.STATUS_DIRTY);
                return b;
            }
        }
        return null;
    }

    public boolean processedBookingExists(String bookingId) {
        for (int i = 0; i < processedLog.getSize(); i++) {
            if (processedLog.get(i).getBookingId().equalsIgnoreCase(bookingId)
                    && processedLog.get(i).getStatus().equals(Booking.STATUS_BOOKED)) {
                return true;
            }
        }
        return false;
    }

    // View current queue in arrival order (AVAILABLE bookings only)
    public Booking[] viewQueue() {
        Booking[] result = new Booking[bookingQueue.getSize()];
        for (int i = 0; i < result.length; i++) {
            result[i] = bookingQueue.get(i);
        }
        return result;
    }

    // View bookings that have already been assigned a room (BOOKED or DIRTY)
    public Booking[] viewProcessedLog() {
        Booking[] result = new Booking[processedLog.getSize()];
        for (int i = 0; i < result.length; i++) {
            result[i] = processedLog.get(i);
        }
        return result;
    }

    public int getQueueSize() {
        return bookingQueue.getSize();
    }

    public int getProcessedLogSize() {
        return processedLog.getSize();
    }

    // Total potential revenue of everyone currently waiting in the queue
    public double getTotalQueueRevenue() {
        double total = 0;
        for (Booking b : viewQueue()) {
            total += b.getTotalPrice();
        }
        return total;
    }

    // --- Report 1: waiting bookings sorted by room type, then check-in time ---
    // Manual insertion sort (no Collections.sort / Arrays.sort)
    public Booking[] getBookingsSortedByRoomType() {
        Booking[] bookings = viewQueue();
        for (int i = 1; i < bookings.length; i++) {
            Booking key = bookings[i];
            int j = i - 1;
            while (j >= 0 && compareByRoomTypeThenCheckIn(bookings[j], key) > 0) {
                bookings[j + 1] = bookings[j];
                j--;
            }
            bookings[j + 1] = key;
        }
        return bookings;
    }

    private int compareByRoomTypeThenCheckIn(Booking a, Booking b) {
        int roomCompare = a.getRoomType().compareTo(b.getRoomType());
        if (roomCompare != 0) {
            return roomCompare;
        }
        return a.getCheckInDateTime().compareTo(b.getCheckInDateTime());
    }

    // --- Report 2: filter waiting bookings by room type, using linear search ---
    public Booking[] filterByRoomType(String roomType) {
        Booking[] all = viewQueue();
        Booking[] temp = new Booking[all.length];
        int count = 0;
        for (Booking b : all) {
            if (b.getRoomType().equalsIgnoreCase(roomType)) {
                temp[count++] = b;
            }
        }
        Booking[] result = new Booking[count];
        System.arraycopy(temp, 0, result, 0, count);
        return result;
    }

    private String generateBookingId() {
        return String.format("BK%05d", bookingCounter++);
    }

    private String generateGuestId() {
        return String.format("G%04d", guestCounter++);
    }
}
