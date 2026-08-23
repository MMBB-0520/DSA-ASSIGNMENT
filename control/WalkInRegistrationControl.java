// Author: <your name>
package control;

import entity.Booking;
import entity.Guest;
import entity.LinkedList;
import java.time.LocalDateTime;

public class WalkInRegistrationControl {

    public static final String[] ROOM_TYPES = {"Standard", "Deluxe", "Suite"};
    private static final double[] ROOM_PRICES = {150.00, 250.00, 400.00}; // RM per night

    // ASSUMPTION: 30 total rooms, 10 per room type.
    private static final int[] ROOM_CAPACITY = {10, 10, 10};

    // Now backed by the team's custom LinkedList<T> instead of the array-based MyQueue.
    private LinkedList<Booking> bookingQueue;   // waiting to be assigned a room (status AVAILABLE)
    private LinkedList<Booking> processedLog;   // already assigned rooms (status BOOKED or DIRTY)
    private int bookingCounter;
    private int guestCounter;

    public WalkInRegistrationControl() {
        bookingQueue = new LinkedList<>();
        processedLog = new LinkedList<>();
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

    public int getCapacityForRoomType(String roomType) {
        for (int i = 0; i < ROOM_TYPES.length; i++) {
            if (ROOM_TYPES[i].equalsIgnoreCase(roomType)) {
                return ROOM_CAPACITY[i];
            }
        }
        return 0;
    }

    // Checks whether at least one room of this type is free for the requested
    // check-in/check-out period. Counts every non-cancelled booking of the same
    // room type (from both the waiting queue AND the processed log) whose stay
    // overlaps the requested period - advance reservations count too, which is
    // exactly why advance bookings effectively get priority: rooms they've
    // already reserved for a date are no longer available to walk-ins that day.
    public boolean isRoomAvailable(String roomType, LocalDateTime checkIn, LocalDateTime checkOut) {
        int capacity = getCapacityForRoomType(roomType);
        int occupied = countOverlapsIn(bookingQueue, roomType, checkIn, checkOut)
                     + countOverlapsIn(processedLog, roomType, checkIn, checkOut);
        return occupied < capacity;
    }

    private int countOverlapsIn(LinkedList<Booking> list, String roomType,
                                 LocalDateTime checkIn, LocalDateTime checkOut) {
        int count = 0;
        for (int i = 0; i < list.size(); i++) {
            Booking b = list.get(i);
            if (!b.getRoomType().equalsIgnoreCase(roomType)) continue;
            if (b.getStatus().equals(Booking.STATUS_CANCELLED)) continue;
            // Standard interval overlap check: two ranges [in1,out1) and [in2,out2) overlap
            // if in1 < out2 AND in2 < out1
            if (b.getCheckInDateTime().isBefore(checkOut) && checkIn.isBefore(b.getCheckOutDateTime())) {
                count++;
            }
        }
        return count;
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
        bookingQueue.add(booking); // enqueue = add to the end of the linked list
        return booking;
    }

    public boolean bookingExists(String bookingId) {
        for (int i = 0; i < bookingQueue.size(); i++) {
            if (bookingQueue.get(i).getBookingId().equalsIgnoreCase(bookingId)) {
                return true;
            }
        }
        return false;
    }

    // ON THE SPOT walk-in: guest is physically here right now, so there's no waiting
    // in the queue - check-in time is stamped as "now" and the room is BOOKED
    // immediately, going straight into the processed log.
    public Booking registerWalkInOnTheSpot(String name, String contactNumber, String roomType,
                                            int numGuests, LocalDateTime checkOutDateTime) {
        LocalDateTime checkInDateTime = LocalDateTime.now();
        Guest guest = new Guest(generateGuestId(), name, contactNumber);
        String bookingId = generateBookingId();
        double price = getPriceForRoomType(roomType);
        Booking booking = new Booking(bookingId, guest, roomType, price, numGuests,
                checkInDateTime, checkOutDateTime, LocalDateTime.now());
        booking.setStatus(Booking.STATUS_BOOKED); // skips AVAILABLE / the waiting queue entirely
        processedLog.add(booking);
        return booking;
    }

    // Process the next booking in arrival order: assigns a room (AVAILABLE -> BOOKED)
    // and moves it out of the waiting queue into the processed log.
    // "Manual" room assignment is also available separately via assignRoomManually(bookingId).
    public Booking processNextBooking() {
        if (bookingQueue.isEmpty()) {
            return null;
        }
        Booking next = bookingQueue.remove(0); // dequeue = remove from the front (index 0)
        next.setStatus(Booking.STATUS_BOOKED);
        processedLog.add(next);
        return next;
    }

    // OPTIONAL: manually assign a room to a specific booking instead of always taking
    // the front of the queue. Front desk picks the ID directly (e.g. a VIP walk-in
    // being bumped ahead, or a specific room request).
    public Booking assignRoomManually(String bookingId) {
        for (int i = 0; i < bookingQueue.size(); i++) {
            if (bookingQueue.get(i).getBookingId().equalsIgnoreCase(bookingId)) {
                Booking chosen = bookingQueue.remove(i);
                chosen.setStatus(Booking.STATUS_BOOKED);
                processedLog.add(chosen);
                return chosen;
            }
        }
        return null;
    }

    // Cancel a booking that is still waiting (AVAILABLE -> CANCELLED)
    public Booking cancelBooking(String bookingId) {
        for (int i = 0; i < bookingQueue.size(); i++) {
            if (bookingQueue.get(i).getBookingId().equalsIgnoreCase(bookingId)) {
                Booking removed = bookingQueue.remove(i);
                removed.setStatus(Booking.STATUS_CANCELLED);
                return removed;
            }
        }
        return null;
    }

    // Guest checks out of an already-booked room (BOOKED -> DIRTY, needs housekeeping)
    // Taken by front desk, per the task list.
    public Booking checkOutBooking(String bookingId) {
        for (int i = 0; i < processedLog.size(); i++) {
            Booking b = processedLog.get(i);
            if (b.getBookingId().equalsIgnoreCase(bookingId) && b.getStatus().equals(Booking.STATUS_BOOKED)) {
                b.setStatus(Booking.STATUS_DIRTY);
                return b;
            }
        }
        return null;
    }

    public boolean processedBookingExists(String bookingId) {
        for (int i = 0; i < processedLog.size(); i++) {
            if (processedLog.get(i).getBookingId().equalsIgnoreCase(bookingId)
                    && processedLog.get(i).getStatus().equals(Booking.STATUS_BOOKED)) {
                return true;
            }
        }
        return false;
    }

    // View current queue in arrival order (AVAILABLE bookings only)
    public Booking[] viewQueue() {
        Booking[] result = new Booking[bookingQueue.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = bookingQueue.get(i);
        }
        return result;
    }

    // View bookings that have already been assigned a room (BOOKED or DIRTY)
    public Booking[] viewProcessedLog() {
        Booking[] result = new Booking[processedLog.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = processedLog.get(i);
        }
        return result;
    }

    public int getQueueSize() {
        return bookingQueue.size();
    }

    public int getProcessedLogSize() {
        return processedLog.size();
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