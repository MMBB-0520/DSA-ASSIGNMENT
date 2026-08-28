package control;

import adt.CustomArrayList;
import adt.CustomQueue;
import adt.ListInterface;
import adt.QueueInterface;
import dao.BookingDAO;
import dao.GuestDAO;
import dao.RoomDAO;
import dao.impl.BookingDAOImpl;
import dao.impl.GuestDAOImpl;
import dao.impl.RoomDAOImpl;
import entity.Booking;
import entity.Guest;
import entity.Room;
import java.time.LocalDateTime;

public class WalkInRegistrationControl {

    public static final String[] ROOM_TYPES = {"Standard", "Deluxe", "Suite"};

    private QueueInterface<Booking> pendingQueue;
    private ListInterface<Booking> processedLog;
    private ListInterface<Room> rooms;

    private BookingDAO bookingDAO;
    private GuestDAO guestDAO;
    private RoomDAO roomDAO;

    public WalkInRegistrationControl() {
        pendingQueue = new CustomQueue<>();
        processedLog = new CustomArrayList<>();

        bookingDAO = new BookingDAOImpl();
        guestDAO = new GuestDAOImpl();
        roomDAO = new RoomDAOImpl();

        loadInitialData();
    }

    private void loadInitialData() {
        rooms = new CustomArrayList<>();
        Room[] loadedRooms = roomDAO.getAllRooms();
        if (loadedRooms != null) {
            for (Room r : loadedRooms) {
                if (r != null) rooms.add(r);
            }
        }

        Booking[] loadedBookings = bookingDAO.getAllBookings();
        if (loadedBookings != null) {
            for (Booking b : loadedBookings) {
                if (b != null) {
                    if ("PENDING".equalsIgnoreCase(b.getStatus())) {
                        pendingQueue.enqueue(b);
                    } else if ("CONFIRMED".equalsIgnoreCase(b.getStatus()) ||
                            "CHECKED_OUT".equalsIgnoreCase(b.getStatus())) {
                        processedLog.add(b);
                    }
                }
            }
        }
    }

    /**
     * Registers a walk-in customer. Automatically sets check-in to the current date/time (now)
     * and computes check-out based on the number of nights.
     */
    public Booking registerWalkInBooking(String guestName, String contact, String icPassport,
            String roomType, int numGuests, int numberOfNights) {

        // Generate ID to correctly map to Guest(guestId, guestName, contactNumber, icPassport)
        String guestId = "G" + (System.currentTimeMillis() % 100000);
        Guest guest = new Guest(guestId, guestName, contact, icPassport);
        guestDAO.saveGuest(guest);

        String confirmNo = generateConfirmationNo();
        double pricePerNight = getPriceForRoomType(roomType);

        // Walk-in check-in is strictly right now
        LocalDateTime checkIn = LocalDateTime.now();
        LocalDateTime checkOut = checkIn.plusDays(numberOfNights);

        Booking booking = new Booking(confirmNo, guest, roomType, numGuests, checkIn, checkOut);
        booking.setPricePerNight(pricePerNight);
        booking.setStatus("PENDING");

        pendingQueue.enqueue(booking);
        bookingDAO.saveBooking(booking);

        return booking;
    }

    // Retained for backward compatibility or advance booking use cases if needed
    public Booking registerBooking(String guestName, String contact, String icPassport,
            String roomType, int numGuests,
            LocalDateTime checkIn, LocalDateTime checkOut) {

        String guestId = "G" + (System.currentTimeMillis() % 100000);
        Guest guest = new Guest(guestId, guestName, contact, icPassport);
        guestDAO.saveGuest(guest);

        String confirmNo = generateConfirmationNo();
        double pricePerNight = getPriceForRoomType(roomType);

        Booking booking = new Booking(confirmNo, guest, roomType, numGuests, checkIn, checkOut);
        booking.setPricePerNight(pricePerNight);
        booking.setStatus("PENDING");

        pendingQueue.enqueue(booking);
        bookingDAO.saveBooking(booking);

        return booking;
    }

    public Booking processNextBooking(Room assignedRoom) {
        if (pendingQueue.isEmpty()) {
            return null;
        }

        Booking booking = pendingQueue.dequeue();

        if (assignedRoom != null) {
            assignedRoom.setStatus("OCCUPIED");
            roomDAO.saveRoom(assignedRoom);
        }

        booking.setStatus("CONFIRMED");
        bookingDAO.saveBooking(booking);
        processedLog.add(booking);

        return booking;
    }

    public Booking cancelBooking(String confirmationNo) {
        QueueInterface<Booking> temp = new CustomQueue<>();
        Booking cancelled = null;

        while (!pendingQueue.isEmpty()) {
            Booking b = pendingQueue.dequeue();
            if (b != null && b.getConfirmationNo().equalsIgnoreCase(confirmationNo) && cancelled == null) {
                cancelled = b;
                b.setStatus("CANCELLED");
                bookingDAO.saveBooking(b);
            } else if (b != null) {
                temp.enqueue(b);
            }
        }
        pendingQueue = temp;
        return cancelled;
    }

    public boolean bookingExists(String confirmationNo) {
        Booking[] arr = pendingQueue.toArray(new Booking[0]);
        for (Booking b : arr) {
            if (b != null && b.getConfirmationNo().equalsIgnoreCase(confirmationNo)) {
                return true;
            }
        }
        return false;
    }

    public Booking getNextBooking() {
        return pendingQueue.peek();
    }

    public int getQueueSize() {
        return pendingQueue.size();
    }

    public Booking[] viewQueue() {
        return pendingQueue.toArray(new Booking[0]);
    }

    public Booking[] viewProcessedLog() {
        Booking[] arr = new Booking[processedLog.size()];
        for (int i = 0; i < processedLog.size(); i++) {
            arr[i] = processedLog.get(i);
        }
        return arr;
    }

    public ListInterface<Booking> getBookingsSortedByRoomType() {
        ListInterface<Booking> all = getAllBookingsList();
        Booking[] arr = new Booking[all.size()];
        for (int i = 0; i < all.size(); i++) {
            arr[i] = all.get(i);
        }

        for (int i = 0; i < arr.length - 1; i++) {
            for (int j = 0; j < arr.length - i - 1; j++) {
                if (compareBookings(arr[j], arr[j + 1]) > 0) {
                    Booking temp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = temp;
                }
            }
        }

        ListInterface<Booking> sortedList = new CustomArrayList<>();
        for (Booking b : arr) {
            sortedList.add(b);
        }
        return sortedList;
    }

    public ListInterface<Booking> filterByRoomType(String roomType) {
        ListInterface<Booking> all = getAllBookingsList();
        ListInterface<Booking> filtered = new CustomArrayList<>();

        for (int i = 0; i < all.size(); i++) {
            Booking b = all.get(i);
            if (b != null && b.getRoomType().equalsIgnoreCase(roomType)) {
                filtered.add(b);
            }
        }
        return filtered;
    }

    public double getTotalQueueRevenue() {
        double total = 0;
        Booking[] arr = pendingQueue.toArray(new Booking[0]);
        for (Booking b : arr) {
            if (b != null && b.getBill() != null) {
                total += b.getBill().getGrandTotal();
            }
        }
        return total;
    }

    public double getPriceForRoomType(String roomType) {
        return switch (roomType.toLowerCase()) {
            case "standard" -> 150.00;
            case "deluxe" -> 250.00;
            case "suite" -> 400.00;
            default -> 100.00;
        };
    }

    public ListInterface<Room> getAvailableRoomsByType(String roomType) {
        ListInterface<Room> availableList = new CustomArrayList<>();
        if (rooms == null) return availableList;

        for (int i = 0; i < rooms.size(); i++) {
            Room room = rooms.get(i);
            if (room != null && room.getRoomType() != null && room.getStatus() != null) {
                if (room.getRoomType().equalsIgnoreCase(roomType) && room.getStatus().equalsIgnoreCase("AVAILABLE")) {
                    availableList.add(room);
                }
            }
        }
        return availableList;
    }

    public Room getAvailableRoomByRoomNo(String roomType, String roomNo) {
        ListInterface<Room> availableList = getAvailableRoomsByType(roomType);
        for (int i = 0; i < availableList.size(); i++) {
            Room room = availableList.get(i);
            if (room != null && room.getRoomNo() != null && room.getRoomNo().equalsIgnoreCase(roomNo.trim())) {
                return availableList.remove(i);
            }
        }
        return null;
    }

    public boolean isRoomAvailable(String roomType, LocalDateTime checkIn, LocalDateTime checkOut) {
        ListInterface<Room> availableRooms = getAvailableRoomsByType(roomType);
        return !availableRooms.isEmpty();
    }

    private ListInterface<Booking> getAllBookingsList() {
        ListInterface<Booking> all = new CustomArrayList<>();
        Booking[] pendingArr = pendingQueue.toArray(new Booking[0]);
        for (Booking b : pendingArr) {
            all.add(b);
        }
        for (int i = 0; i < processedLog.size(); i++) {
            all.add(processedLog.get(i));
        }
        return all;
    }

    private int compareBookings(Booking b1, Booking b2) {
        if (b1 == null || b2 == null) return 0;
        int typeComp = b1.getRoomType().compareToIgnoreCase(b2.getRoomType());
        if (typeComp != 0) {
            return typeComp;
        }
        return b1.getCheckInDateTime().compareTo(b2.getCheckInDateTime());
    }

    private String generateConfirmationNo() {
        return "BK" + (System.currentTimeMillis() % 100000);
    }
}