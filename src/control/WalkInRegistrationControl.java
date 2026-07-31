// Author: <your name>
package control;

import entity.Booking;
import entity.Room;
import entity.Staff;
import entity.MyQueue;
import java.time.LocalDateTime;

import data.RoomData;
import data.StaffData;

public class WalkInRegistrationControl {

    private MyQueue<Booking> bookingQueue;   // waiting to be assigned a room (status AVAILABLE)
    private MyQueue<Booking> processedLog;   // already assigned rooms (status BOOKED or DIRTY)
    private int bookingCounter;

    private DataRepository dataRepository;
    private Room[] rooms;
    private Staff[] staffList;

    public WalkInRegistrationControl() {
        bookingQueue = new MyQueue<>();
        processedLog = new MyQueue<>();
        dataRepository = new DataRepository();
        loadData();
    }

    private void loadData() {
        // Load Staff
        Staff[] loadedStaff = dataRepository.loadStaff();
        if (loadedStaff == null || loadedStaff.length == 0) {
            staffList = StaffData.STAFF;
            dataRepository.saveStaff(staffList);
        } else {
            staffList = loadedStaff;
        }

        // Load Rooms
        Room[] loadedRooms = dataRepository.loadRooms();
        if (loadedRooms == null || loadedRooms.length == 0) {
            rooms = RoomData.ROOMS;
            dataRepository.saveRooms(rooms);
        } else {
            rooms = loadedRooms;
        }

        // Load Bookings
        Booking[] allBookings = dataRepository.loadBookings();
        int maxId = 0;
        for (Booking b : allBookings) {
            // Re-link with local objects to avoid detached instances
            if (b.getStaff() != null) {
                b.setStaff(getStaffById(b.getStaff().getStaffId()));
            }
            if (b.getRoomId() != null) {
                b.setRoom(getRoomById(b.getRoomId()));
            }

            if (Booking.STATUS_AVAILABLE.equals(b.getBookingStatus())) {
                bookingQueue.enqueue(b);
            } else {
                processedLog.enqueue(b);
            }

            try {
                int idNum = Integer.parseInt(b.getBookingId().replace("BK", ""));
                if (idNum > maxId)
                    maxId = idNum;
            } catch (Exception e) {
            }
        }
        bookingCounter = maxId + 1;
    }

    public void saveData() {
        dataRepository.saveBookings(viewQueue(), viewProcessedLog());
        dataRepository.saveRooms(rooms);
    }

    public Staff getStaffById(String id) {
        for (Staff s : staffList) {
            if (s.getStaffId().equals(id))
                return s;
        }
        return null;
    }

    public Staff authenticate(String staffId, String password) {
        for (Staff s : staffList) {
            if (s.getStaffId().equals(staffId) && s.getPassword().equals(password)) {
                return s;
            }
        }
        return null;
    }

    public Room getRoomById(String id) {
        for (Room r : rooms) {
            if (r.getRoomId().equals(id))
                return r;
        }
        return null;
    }

    public Room[] getRooms() {
        return rooms;
    }

    // Register a new walk-in / standard booking - joins the back of the queue.
    // Status starts as AVAILABLE (waiting for a room to be assigned).
    public Booking registerBooking(String guestName, String icPassport, String contact, Staff staff, Room room,
            int numGuests, LocalDateTime checkInDateTime, LocalDateTime checkOutDateTime) {
        String bookingId = generateBookingId();
        Booking booking = new Booking(bookingId, guestName, icPassport, contact, staff, room, numGuests,
                checkInDateTime, checkOutDateTime, LocalDateTime.now());
        
        bookingQueue.enqueue(booking);
        saveData();
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
            next.setBookingStatus(Booking.STATUS_BOOKED);
            if (next.getRoom() != null) {
                next.getRoom().setStatus(Room.STATUS_OCCUPIED);
                next.getRoom().setAvailableFrom(next.getCheckOutDateTime());
            }
            processedLog.enqueue(next);
            saveData();
        }
        return next;
    }

    // Cancel a booking that is still waiting (AVAILABLE -> CANCELLED)
    public Booking cancelBooking(String bookingId) {
        Booking removed = bookingQueue.removeById(bookingId);
        if (removed != null) {
            removed.setBookingStatus(Booking.STATUS_CANCELLED);
            processedLog.enqueue(removed); // optional: keep log of cancelled bookings
            saveData();
        }
        return removed;
    }

    // Guest checks out of an already-booked room (BOOKED -> DIRTY, needs housekeeping)
    public Booking checkOutBooking(String bookingId) {
        for (int i = 0; i < processedLog.getSize(); i++) {
            Booking b = processedLog.get(i);
            if (b.getBookingId().equalsIgnoreCase(bookingId) && b.getBookingStatus().equals(Booking.STATUS_BOOKED)) {
                b.setBookingStatus(Booking.STATUS_DIRTY);
                if (b.getRoom() != null) {
                    b.getRoom().setStatus(Room.STATUS_DIRTY);
                }
                saveData();
                return b;
            }
        }
        return null;
    }

    public boolean processedBookingExists(String bookingId) {
        for (int i = 0; i < processedLog.getSize(); i++) {
            if (processedLog.get(i).getBookingId().equalsIgnoreCase(bookingId)
                    && processedLog.get(i).getBookingStatus().equals(Booking.STATUS_BOOKED)) {
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
        int roomCompare = a.getRoom().getType().compareTo(b.getRoom().getType());
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
            if (b.getRoom().getType().equalsIgnoreCase(roomType)) {
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
}
