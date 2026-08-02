package control;

import adt.MyHashMap;
import data.JsonManager;
import entity.Booking;
import entity.Guest;
import java.time.LocalDateTime;

/**
 * Control class for Front-Desk Search module.
 * Uses custom Non-Linear ADT (MyHashMap) for O(1) Hash Searching of guest
 * records by 8-digit confirmation number, synchronized with JSON persistence.
 */
public class FrontDeskSearchControl {

    // Custom Non-Linear ADT: Key = 8-digit Confirmation Number (String), Value =
    // Booking Object
    private MyHashMap<String, Booking> bookingMap;

    public FrontDeskSearchControl() {
        this.bookingMap = new MyHashMap<>();
        loadDataFromJson();
    }

    /**
     * Loads bookings from JsonManager (src/data/bookings.json) into the Hash Table.
     * If JSON is empty, loads default sample data.
     */
    private void loadDataFromJson() {
        Booking[] loaded = JsonManager.loadBookings();
        if (loaded != null && loaded.length > 0) {
            for (Booking b : loaded) {
                if (b != null && b.getConfirmationNo() != null) {
                    bookingMap.put(b.getConfirmationNo(), b);
                }
            }
        } else {
            loadSampleData();
        }
    }

    /**
     * Pre-populate sample guest booking records with 8-digit confirmation numbers
     * if no JSON file exists yet.
     */
    private void loadSampleData() {
        Guest g1 = new Guest("G1001", "Alice Johnson", "0123456789", "950101-14-5566");
        Booking b1 = new Booking("84920183", g1, "Deluxe", 250.00, 2,
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().minusHours(3));
        b1.setRoomNo("201");
        b1.setStatus(Booking.STATUS_CONFIRMED);
        bookingMap.put(b1.getConfirmationNo(), b1);

        Guest g2 = new Guest("G1002", "Bob Smith", "0198765432", "A12345678");
        Booking b2 = new Booking("10293847", g2, "Suite", 400.00, 3,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(4),
                LocalDateTime.now().minusDays(1));
        b2.setStatus(Booking.STATUS_PENDING);
        bookingMap.put(b2.getConfirmationNo(), b2);

        Guest g3 = new Guest("G1003", "Charlie Tan", "0112233445", "900812-10-1234");
        Booking b3 = new Booking("59302148", g3, "Standard", 150.00, 1,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().minusDays(2));
        b3.setRoomNo("105");
        b3.setStatus(Booking.STATUS_CONFIRMED);
        bookingMap.put(b3.getConfirmationNo(), b3);

        saveMapToJson();
    }

    private void saveMapToJson() {
        Object[] values = bookingMap.values();

        Booking[] bookings = new Booking[values.length];
        Guest[] guests = new Guest[values.length];

        for (int i = 0; i < values.length; i++) {
            Booking booking = (Booking) values[i];
            bookings[i] = booking;
            guests[i] = booking.getGuest();
        }

        JsonManager.saveBookings(bookings);
        JsonManager.saveGuests(guests);
    }

    /**
     * Instantly retrieves complete guest booking information using Hash Search.
     * Time Complexity: O(1) average.
     */
    public Booking searchByConfirmationNo(String confirmationNo) {
        if (confirmationNo == null || confirmationNo.trim().isEmpty()) {
            return null;
        }
        // Always refresh from JSON in case WalkInRegistration updated it
        loadDataFromJson();
        return bookingMap.get(confirmationNo.trim());
    }

    /**
     * Adds or updates a booking in the Hash Table and persists to JSON.
     */
    public void addBooking(Booking booking) {
        if (booking != null && booking.getConfirmationNo() != null) {
            bookingMap.put(booking.getConfirmationNo(), booking);

            // Sync to JSON file
            Booking[] existing = JsonManager.loadBookings();
            Booking[] updated = new Booking[existing.length + 1];
            System.arraycopy(existing, 0, updated, 0, existing.length);
            updated[existing.length] = booking;
            JsonManager.saveBookings(updated);

            // Sync guest details to guests.json
            if (booking.getGuest() != null) {
                Guest[] existingGuests = JsonManager.loadGuests();
                boolean exists = false;
                for (Guest g : existingGuests) {
                    if (g != null && g.getGuestId() != null
                            && g.getGuestId().equalsIgnoreCase(booking.getGuest().getGuestId())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    Guest[] updatedGuests = new Guest[existingGuests.length + 1];
                    System.arraycopy(existingGuests, 0, updatedGuests, 0, existingGuests.length);
                    updatedGuests[existingGuests.length] = booking.getGuest();
                    JsonManager.saveGuests(updatedGuests);
                }
            }
        }
    }

    public boolean containsConfirmationNo(String confirmationNo) {
        loadDataFromJson();
        return bookingMap.containsKey(confirmationNo);
    }

    public int getTotalIndexedCount() {
        loadDataFromJson();
        return bookingMap.size();
    }
}
