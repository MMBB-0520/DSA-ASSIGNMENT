package control;

import adt.BSTInterface;
import adt.MyBinarySearchTree;
import adt.ListInterface;
import adt.CustomLinkedList;
import dao.BookingDAO;
import dao.GuestDAO;
import dao.RoomDAO;
import dao.impl.BookingDAOImpl;
import dao.impl.GuestDAOImpl;
import dao.impl.RoomDAOImpl;
import entity.Bill;
import entity.Booking;
import entity.Guest;
import entity.Room;
import java.util.Iterator;

/**
 * Control class for Front-Desk Search module.
 * Best Practice 1: Declares ADT variable using interface type (BSTInterface).
 * Best Practice 2: Declares Data Access Objects using Interface types
 * (BookingDAO, RoomDAO, GuestDAO).
 *
 * @author Ng Yuen Qi
 */
public class FrontDeskServiceControl {

    // Using interface types for variables (OOP Best Practice)
    private final BSTInterface<Booking> bookingTree;
    private final BookingDAO bookingDAO;
    private final RoomDAO roomDAO;
    private final GuestDAO guestDAO;

    public FrontDeskServiceControl() {
        this.bookingTree = new MyBinarySearchTree<>();
        this.bookingDAO = new BookingDAOImpl();
        this.roomDAO = new RoomDAOImpl();
        this.guestDAO = new GuestDAOImpl();
        loadData();
    }

    /**
     * Loads bookings from DAO into the Binary Search Tree.
     * Auto-assigns available rooms for CONFIRMED bookings whose check-in date is
     * today or past.
     */
    private void loadData() {
        bookingTree.clear();
        Booking[] loaded = bookingDAO.getAllBookings();
        Room[] allRooms = roomDAO.getAllRooms();

        boolean roomUpdated = false;
        boolean bookingUpdated = false;

        if (loaded != null && loaded.length > 0) {
            for (Booking b : loaded) {
                if (b != null && b.getConfirmationNo() != null) {
                    // 1. Auto-cancel NO-SHOW bookings: If RESERVED or CONFIRMED and past
                    // checkInDateTime + 15 mins without check-in
                    if ((Booking.STATUS_RESERVED.equalsIgnoreCase(b.getStatus())
                            || Booking.STATUS_CONFIRMED.equalsIgnoreCase(b.getStatus()))
                            && b.getCheckInDateTime() != null
                            && java.time.LocalDateTime.now().isAfter(b.getCheckInDateTime().plusMinutes(15))) {

                        b.setStatus(Booking.STATUS_CANCELLED);
                        bookingUpdated = true;

                        // Release assigned room back to AVAILABLE if any
                        if (b.getRoomNo() != null && !b.getRoomNo().isEmpty()
                                && !"Unassigned".equalsIgnoreCase(b.getRoomNo())) {
                            if (allRooms != null) {
                                for (Room r : allRooms) {
                                    if (r != null && r.getRoomNo().equalsIgnoreCase(b.getRoomNo())) {
                                        r.setStatus(Room.STATUS_AVAILABLE);
                                        roomUpdated = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    // 2. Auto-assign room if check-in date is TODAY or past (within grace period),
                    // status is RESERVED or PENDING and roomNo is unassigned
                    else if ((Booking.STATUS_RESERVED.equalsIgnoreCase(b.getStatus())
                            || Booking.STATUS_PENDING.equalsIgnoreCase(b.getStatus()))
                            && (b.getRoomNo() == null || b.getRoomNo().isEmpty()
                                    || "Unassigned".equalsIgnoreCase(b.getRoomNo()))
                            && b.getCheckInDateTime() != null
                            && !b.getCheckInDateTime().toLocalDate().isAfter(java.time.LocalDate.now())) {

                        if (allRooms != null) {
                            for (Room r : allRooms) {
                                if (r != null && r.getRoomType().equalsIgnoreCase(b.getRoomType())
                                        && Room.STATUS_AVAILABLE.equalsIgnoreCase(r.getStatus())) {
                                    b.setRoomNo(r.getRoomNo());
                                    b.setStatus(Booking.STATUS_CONFIRMED); // Status becomes CONFIRMED once room is
                                                                           // assigned!
                                    r.setStatus(Room.STATUS_BOOKED);
                                    roomUpdated = true;
                                    bookingUpdated = true;
                                    break;
                                }
                            }
                        }
                    }
                    // Auto-update room status to LATE CHECK-OUT tier if CHECKED_IN and past
                    // checkOutDateTime
                    if (Booking.STATUS_CHECKED_IN.equalsIgnoreCase(b.getStatus())
                            && b.getRoomNo() != null && !b.getRoomNo().isEmpty()
                            && b.getCheckOutDateTime() != null
                            && java.time.LocalDateTime.now().isAfter(b.getCheckOutDateTime())) {

                        long minutesOver = java.time.temporal.ChronoUnit.MINUTES.between(b.getCheckOutDateTime(),
                                java.time.LocalDateTime.now());
                        String lateRoomStatus;
                        if (minutesOver <= 30) {
                            lateRoomStatus = Room.STATUS_LATE_30MINS;
                        } else if (minutesOver <= 120) {
                            lateRoomStatus = Room.STATUS_LATE_2HRS;
                        } else {
                            lateRoomStatus = Room.STATUS_LATE_4HRS;
                        }

                        if (allRooms != null) {
                            for (Room r : allRooms) {
                                if (r != null && r.getRoomNo().equalsIgnoreCase(b.getRoomNo())) {
                                    if (!lateRoomStatus.equalsIgnoreCase(r.getStatus())) {
                                        r.setStatus(lateRoomStatus);
                                        roomUpdated = true;
                                    }
                                    break;
                                }
                            }
                        }
                    }

                    bookingTree.insert(b);
                }
            }
        }

        if (roomUpdated && allRooms != null) {
            roomDAO.saveAllRooms(allRooms);
        }
        if (bookingUpdated && loaded != null) {
            bookingDAO.saveAllBookings(loaded);
        }
    }

    public void reloadData() {
        loadData();
    }

    /**
     * Retrieves guest booking information using Binary Search Tree lookup.
     * Time Complexity: O(log n) average.
     */
    public Booking searchByConfirmationNo(String confirmationNo) {
        if (confirmationNo == null || confirmationNo.trim().isEmpty()) {
            return null;
        }
        Booking dummySearchKey = new Booking(confirmationNo.trim(), null, null, 0, 0, null, null, null);
        return bookingTree.search(dummySearchKey);
    }

    public boolean containsConfirmationNo(String confirmationNo) {
        if (confirmationNo == null)
            return false;
        Booking dummySearchKey = new Booking(confirmationNo.trim(), null, null, 0, 0, null, null, null);
        return bookingTree.contains(dummySearchKey);
    }

    /**
     * Adds or updates a booking in the BST and persists via DAOs.
     */
    public boolean addBooking(Booking booking) {
        if (booking == null || booking.getConfirmationNo() == null) {
            return false;
        }

        boolean added = bookingTree.insert(booking);

        if (!added) {
            return false;
        }

        bookingDAO.saveBooking(booking);

        if (booking.getGuest() != null) {
            guestDAO.saveGuest(booking.getGuest());
        }
        return true;
    }

    public int getTotalIndexedCount() {
        return bookingTree.size();
    }

    public Booking getMinBooking() {
        return bookingTree.findMin();
    }

    public Booking getMaxBooking() {
        return bookingTree.findMax();
    }

    public Iterator<Booking> getBookingIterator() {
        return bookingTree.getIterator();
    }

    /**
     * Registers a future reservation booking and persists changes to BST and JSON.
     */
    public Booking registerFutureBooking(String name, String contactNumber, String icPassport,
            String roomType, double pricePerNight, int numGuests,
            java.time.LocalDateTime checkIn, java.time.LocalDateTime checkOut, double depositPaid) {

        Guest existingGuest = findGuestByContact(contactNumber);
        Guest guest = (existingGuest != null) ? existingGuest
                : new Guest(generateGuestId(), name, contactNumber, icPassport);

        String confirmationNo = String.format("%08d", (int) (Math.random() * 90000000) + 10000000);
        Booking booking = new Booking(confirmationNo, guest, roomType, pricePerNight, numGuests, checkIn, checkOut,
                java.time.LocalDateTime.now());
        booking.setStatus(Booking.STATUS_RESERVED);
        Bill bill = booking.getBill();
        bill.setCashTendered(depositPaid);
        bill.setPaymentStatus("DEPOSIT PAID");

        bookingTree.insert(booking);
        bookingDAO.saveBooking(booking);
        if (guestDAO != null && guest != null) {
            guestDAO.saveGuest(guest);
        }

        return booking;
    }

    public Booking registerFutureBooking(String name, String contactNumber, String icPassport,
            String roomType, double pricePerNight, int numGuests,
            java.time.LocalDateTime checkIn, java.time.LocalDateTime checkOut) {
        return registerFutureBooking(name, contactNumber, icPassport, roomType, pricePerNight, numGuests, checkIn,
                checkOut, pricePerNight * 0.30);
    }

    /**
     * Updates payment status of a booking bill to PAID (CASH) and persists via
     * BookingDAO.
     */
    public boolean processCashPayment(String confirmationNo,
            double cashTendered) {
        Booking booking = searchByConfirmationNo(confirmationNo);
        if (booking == null) {
            return false;
        }
        Bill bill = booking.getBill();
        boolean success = bill.processCashPayment(cashTendered);
        if (!success) {
            return false;
        }
        bookingDAO.saveBooking(booking);
        return true;
    }

    /**
     * Processes check-in for a booking:
     * Collects 30% Booking Deposit + RM200 Security Deposit,
     * updates status to CHECKED_IN, and persists changes.
     */
    public boolean processCheckIn(String confirmationNo, double cashTendered) {
        Booking booking = searchByConfirmationNo(confirmationNo);
        if (booking == null) {
            return false;
        }
        Bill bill = booking.getBill();
        String status = booking.getStatus();
        if (Booking.STATUS_CHECKED_IN.equalsIgnoreCase(status)
                || Booking.STATUS_CANCELLED.equalsIgnoreCase(status)
                || Booking.STATUS_COMPLETED.equalsIgnoreCase(status)) {
            return false;
        }

        double requiredDeposit = bill.getRequiredCheckInDeposit();
        if (cashTendered < requiredDeposit) {
            return false;
        }

        booking.setStatus(Booking.STATUS_CHECKED_IN);
        bill.setCashTendered(bill.getCashTendered() + cashTendered);
        bill.setChangeDue(cashTendered - requiredDeposit);
        bill.setPaymentStatus(Bill.PAYMENT_PAID_CASH);

        // Auto-assign room if unassigned during Check-In
        Room[] allRooms = roomDAO.getAllRooms();
        if (allRooms != null) {
            if (booking.getRoomNo() == null || booking.getRoomNo().isEmpty()
                    || "Unassigned".equalsIgnoreCase(booking.getRoomNo())) {
                for (Room room : allRooms) {
                    if (room != null && room.getRoomType().equalsIgnoreCase(booking.getRoomType())
                            && Room.STATUS_AVAILABLE.equalsIgnoreCase(room.getStatus())) {
                        booking.setRoomNo(room.getRoomNo());
                        room.setStatus(Room.STATUS_BOOKED);
                        break;
                    }
                }
            } else {
                for (Room room : allRooms) {
                    if (room != null && room.getRoomNo().equalsIgnoreCase(booking.getRoomNo())) {
                        room.setStatus(Room.STATUS_BOOKED);
                        break;
                    }
                }
            }
            roomDAO.saveAllRooms(allRooms);
        }

        bookingDAO.saveBooking(booking);
        return true;
    }

    /**
     * Determines late check-out option automatically.
     * Delegates calculation logic to Booking entity method.
     */
    public int determineLateCheckOutOption(Booking booking) {
        if (booking == null) {
            return 0;
        }
        return booking.determineLateCheckOutOption();
    }

    /**
     * Pre-requests Late Check-Out for a checked-in guest with optional advance
     * payment collection.
     */
    public boolean requestLateCheckOut(String confirmationNo, int lateOption, double cashTendered) {
        Booking booking = searchByConfirmationNo(confirmationNo);
        if (booking == null || !Booking.STATUS_CHECKED_IN.equalsIgnoreCase(booking.getStatus())) {
            return false;
        }
        Bill bill = booking.getBill();
        if (lateOption < 0 || lateOption > 3) {
            return false;
        }
        bill.setRequestedLateOption(lateOption);
        if (cashTendered > 0) {
            bill.setCashTendered(bill.getCashTendered() + cashTendered);
        }
        if (booking.getRoomNo() != null && !booking.getRoomNo().isEmpty()) {
            Room[] allRooms = roomDAO.getAllRooms();
            if (allRooms != null) {
                for (Room r : allRooms) {
                    if (r != null && r.getRoomNo().equalsIgnoreCase(booking.getRoomNo())) {
                        r.setStatus("Dirty (Late Check-Out)");
                        break;
                    }
                }
                roomDAO.saveAllRooms(allRooms);
            }
        }
        bookingDAO.saveBooking(booking);
        return true;
    }

    public boolean requestLateCheckOut(String confirmationNo, int lateOption) {
        return requestLateCheckOut(confirmationNo, lateOption, 0.0);
    }

    /**
     * Processes Check-Out for a booking:
     * Auto-determines Late Check-Out Charge (pre-requested or actual time past
     * checkOutDateTime),
     * deducts pre-collected deposits (30% Booking Deposit + RM200 Security
     * Deposit),
     * computes net balance / refund, updates status to COMPLETED, and sets assigned
     * room status to DIRTY.
     */
    public boolean processCheckOut(String confirmationNo, double cashTendered) {
        Booking booking = searchByConfirmationNo(confirmationNo);
        if (booking == null) {
            return false;
        }
        Bill bill = booking.getBill();
        String status = booking.getStatus();
        if (!Booking.STATUS_CHECKED_IN.equalsIgnoreCase(status) && !Booking.STATUS_CONFIRMED.equalsIgnoreCase(status)) {
            return false;
        }

        int lateOption = determineLateCheckOutOption(booking);
        double lateCharge = bill.calculateLateCheckOutCharge(lateOption);
        double netDue = bill.getNetDue(lateCharge);

        if (netDue > 0 && cashTendered < netDue) {
            return false;
        }

        booking.setStatus(Booking.STATUS_COMPLETED);
        bill.setPaymentStatus(Bill.PAYMENT_PAID_CASH);
        bill.setCashTendered(cashTendered);
        bill.setChangeDue(netDue > 0 ? cashTendered - netDue : 0.0);

        // Update assigned room status to DIRTY
        if (booking.getRoomNo() != null && !booking.getRoomNo().isEmpty()) {
            Room[] allRooms = roomDAO.getAllRooms();
            for (Room room : allRooms) {
                if (room != null && room.getRoomNo().equalsIgnoreCase(booking.getRoomNo())) {
                    room.setStatus(Room.STATUS_DIRTY);
                    break;
                }
            }
            roomDAO.saveAllRooms(allRooms);
        }

        bookingDAO.saveBooking(booking);
        return true;
    }

    /**
     * Search Available Rooms Pipeline:
     * 1. Iterate through rooms loaded via RoomDAO.
     * 2. Filter available rooms into CustomLinkedList ADT by Room Type and Max
     * Price limit.
     * 3. Sort filtered rooms by Room Number or Price per night.
     */
    public Room[] searchAvailableRooms(String roomTypeFilter, String sortBy) {
        Room[] allRooms = roomDAO.getAllRooms();
        if (allRooms == null || allRooms.length == 0) {
            return new Room[0];
        }

        // Step 1 & 2: Iterate & Filter using Linear ADT (CustomLinkedList)
        ListInterface<Room> filteredList = new CustomLinkedList<>();

        for (Room room : allRooms) {
            if (room != null && Room.STATUS_AVAILABLE.equalsIgnoreCase(room.getStatus())) {
                boolean matchType = (roomTypeFilter == null || roomTypeFilter.trim().isEmpty()
                        || roomTypeFilter.equalsIgnoreCase("All")
                        || room.getRoomType().equalsIgnoreCase(roomTypeFilter.trim()));

                if (matchType) {
                    filteredList.add(room);
                }
            }
        }

        // Convert CustomLinkedList to array for sorting
        Object[] rawArray = filteredList.toArray();
        Room[] filtered = new Room[rawArray.length];
        for (int i = 0; i < rawArray.length; i++) {
            filtered[i] = (Room) rawArray[i];
        }

        // Step 3: Sort by Room Number or Price per Night (Insertion Sort algorithm)
        for (int i = 1; i < filtered.length; i++) {
            Room key = filtered[i];
            int j = i - 1;

            while (j >= 0 && compareRooms(filtered[j], key, sortBy) > 0) {
                filtered[j + 1] = filtered[j];
                j--;
            }
            filtered[j + 1] = key;
        }

        return filtered;
    }

    private int compareRooms(Room r1, Room r2, String sortBy) {
        if ("Price".equalsIgnoreCase(sortBy)) {
            int priceComp = Double.compare(r1.getPricePerNight(), r2.getPricePerNight());
            if (priceComp != 0)
                return priceComp;
        }
        return r1.getRoomNo().compareTo(r2.getRoomNo());
    }

    /**
     * Calculates available and total rooms count for a specific room type today.
     * Deducts pending unassigned bookings to reflect true availability.
     * 
     * @return int array where index 0 is available count, index 1 is total count.
     */
    public int[] getRoomTypeCounts(String roomType) {
        Room[] allRooms = roomDAO.getAllRooms();
        int total = 0;
        int available = 0;

        if (allRooms != null) {
            for (Room r : allRooms) {
                if (r != null && r.getRoomType().equalsIgnoreCase(roomType)) {
                    total++;
                    if (Room.STATUS_AVAILABLE.equalsIgnoreCase(r.getStatus())) {
                        available++;
                    }
                }
            }
        }

        // Deduct pending / reserved unassigned bookings for today
        Booking[] allBookings = bookingDAO.getAllBookings();
        if (allBookings != null) {
            for (Booking b : allBookings) {
                if (b != null && roomType.equalsIgnoreCase(b.getRoomType())
                        && (Booking.STATUS_PENDING.equalsIgnoreCase(b.getStatus())
                                || Booking.STATUS_RESERVED.equalsIgnoreCase(b.getStatus()))
                        && (b.getRoomNo() == null || b.getRoomNo().isEmpty()
                                || "Unassigned".equalsIgnoreCase(b.getRoomNo()))
                        && b.getCheckInDateTime() != null
                        && !b.getCheckInDateTime().toLocalDate().isAfter(java.time.LocalDate.now())) {
                    available--;
                }
            }
        }

        return new int[] { Math.max(available, 0), total };
    }

    /**
     * Calculates price range [minPrice, maxPrice] for a specific room type.
     * 
     * @return double array where index 0 is min price, index 1 is max price.
     */
    public double[] getRoomTypePriceRange(String roomType) {
        Room[] allRooms = (roomDAO != null) ? roomDAO.getAllRooms() : null;
        double minPrice = Double.MAX_VALUE;
        double maxPrice = 0.0;

        if (allRooms != null) {
            for (Room r : allRooms) {
                if (r != null && r.getRoomType() != null && r.getRoomType().equalsIgnoreCase(roomType)) {
                    double price = r.getPricePerNight();
                    if (price < minPrice)
                        minPrice = price;
                    if (price > maxPrice)
                        maxPrice = price;
                }
            }
        }

        if (minPrice == Double.MAX_VALUE) {
            minPrice = 0.0;
        }
        return new double[] { minPrice, maxPrice };
    }

    /**
     * Calculates overall hotel room price range [minPrice, maxPrice] across all
     * rooms.
     */
    public double[] getOverallPriceRange() {
        Room[] allRooms = (roomDAO != null) ? roomDAO.getAllRooms() : null;
        double minPrice = Double.MAX_VALUE;
        double maxPrice = 0.0;

        if (allRooms != null) {
            for (Room r : allRooms) {
                if (r != null) {
                    double price = r.getPricePerNight();
                    if (price < minPrice)
                        minPrice = price;
                    if (price > maxPrice)
                        maxPrice = price;
                }
            }
        }

        if (minPrice == Double.MAX_VALUE) {
            minPrice = 0.0;
        }
        return new double[] { minPrice, maxPrice };
    }

    /**
     * Calculates true effective available rooms count for a given room type and
     * date range,
     * deducting DIRTY rooms and all active bookings (PENDING, CONFIRMED,
     * CHECKED_IN) that overlap with the target date range.
     * Prevents OVERBOOKING and protects against assigning DIRTY rooms.
     */
    public int getAvailableRoomCountForPeriod(String roomType, java.time.LocalDateTime checkIn,
            java.time.LocalDateTime checkOut) {
        if (roomType == null || checkIn == null || checkOut == null) {
            return 0;
        }

        Room[] allRooms = roomDAO.getAllRooms();
        int totalPhysicalRooms = 0;
        if (allRooms != null) {
            for (Room r : allRooms) {
                if (r != null && r.getRoomType().equalsIgnoreCase(roomType)) {
                    totalPhysicalRooms++;
                }
            }
        }

        Booking[] allBookings = bookingDAO.getAllBookings();
        int activeOverlappingBookings = 0;

        if (allBookings != null) {
            for (Booking b : allBookings) {
                if (b == null || b.getRoomType() == null || !b.getRoomType().equalsIgnoreCase(roomType)) {
                    continue;
                }

                String status = b.getStatus();
                if (Booking.STATUS_CANCELLED.equalsIgnoreCase(status)
                        || Booking.STATUS_COMPLETED.equalsIgnoreCase(status)) {
                    continue;
                }

                java.time.LocalDateTime bCheckIn = b.getCheckInDateTime();
                java.time.LocalDateTime bCheckOut = b.getCheckOutDateTime();

                if (bCheckIn != null && bCheckOut != null) {
                    // Standard interval overlap check: [bCheckIn, bCheckOut) overlaps with
                    // [checkIn, checkOut)
                    if (bCheckIn.isBefore(checkOut) && bCheckOut.isAfter(checkIn)) {
                        activeOverlappingBookings++;
                    }
                }
            }
        }

        int netAvailable = totalPhysicalRooms - activeOverlappingBookings;
        return Math.max(netAvailable, 0);
    }

    /**
     * Collects all rooms requiring attention (DIRTY or LATE Check-Out status)
     * using CustomLinkedList Linear ADT.
     */
    public ListInterface<Room> getOverdueAndDirtyRooms() {
        ListInterface<Room> alertList = new CustomLinkedList<>();
        Room[] allRooms = roomDAO.getAllRooms();
        if (allRooms != null) {
            for (Room r : allRooms) {
                if (r != null) {
                    String status = r.getStatus();
                    if (Room.STATUS_DIRTY.equalsIgnoreCase(status)
                            || Room.STATUS_LATE_30MINS.equalsIgnoreCase(status)
                            || Room.STATUS_LATE_2HRS.equalsIgnoreCase(status)
                            || Room.STATUS_LATE_4HRS.equalsIgnoreCase(status)) {
                        alertList.add(r);
                    }
                }
            }
        }
        return alertList;
    }

    /**
     * Looks up existing guest record by contact number.
     */
    public Guest findGuestByContact(String contactNumber) {
        if (contactNumber == null || contactNumber.trim().isEmpty()) {
            return null;
        }
        Guest[] guests = guestDAO.getAllGuests();
        if (guests != null) {
            for (Guest g : guests) {
                if (g != null && contactNumber.trim().equalsIgnoreCase(g.getContactNumber())) {
                    return g;
                }
            }
        }
        return null;
    }

    /**
     * Retrieves all historical and active bookings for a guest by contact number
     * using CustomLinkedList Linear ADT.
     */
    public ListInterface<Booking> getGuestBookingHistory(String contactNumber) {
        ListInterface<Booking> history = new CustomLinkedList<>();
        if (contactNumber == null || contactNumber.trim().isEmpty()) {
            return history;
        }

        Booking[] allBookings = bookingDAO.getAllBookings();
        if (allBookings != null) {
            for (Booking b : allBookings) {
                if (b != null && b.getGuest() != null
                        && contactNumber.trim().equalsIgnoreCase(b.getGuest().getContactNumber())) {
                    history.add(b);
                }
            }
        }
        return history;
    }

    private String generateGuestId() {
        Guest[] guests = guestDAO.getAllGuests();
        int count = (guests != null) ? guests.length + 1 : 1;
        return String.format("G%04d", count);
    }

    /**
     * Analytics & Reporting Query Methods (Calculated on-demand via ADT Iterator)
     */
    public int getTotalBookingsCount() {
        return bookingTree.size();
    }

    public double getGrandTotalRevenue() {
        double total = 0.0;
        Iterator<Booking> iterator = getBookingIterator();
        while (iterator != null && iterator.hasNext()) {
            Booking b = iterator.next();
            if (b != null && b.getBill() != null) {
                total += b.getBill().getGrandTotal();
            }
        }
        return total;
    }

    public double getAverageRevenuePerBooking() {
        int count = getTotalBookingsCount();
        return (count > 0) ? (getGrandTotalRevenue() / count) : 0.0;
    }

    public double getAverageStayNights() {
        int count = getTotalBookingsCount();
        if (count == 0)
            return 0.0;
        int totalNights = 0;
        Iterator<Booking> iterator = getBookingIterator();
        while (iterator != null && iterator.hasNext()) {
            Booking b = iterator.next();
            if (b != null) {
                totalNights += b.getNights();
            }
        }
        return (double) totalNights / count;
    }

    public int getBookingCountByStatus(String status) {
        int count = 0;
        Iterator<Booking> iterator = getBookingIterator();
        while (iterator != null && iterator.hasNext()) {
            Booking b = iterator.next();
            if (b != null && status.equalsIgnoreCase(b.getStatus())) {
                count++;
            }
        }
        return count;
    }

    public int getPaidBookingCount() {
        int count = 0;
        Iterator<Booking> iterator = getBookingIterator();
        while (iterator != null && iterator.hasNext()) {
            Booking b = iterator.next();
            if (b != null && b.getBill() != null && Bill.PAYMENT_PAID_CASH.equalsIgnoreCase(b.getBill().getPaymentStatus())) {
                count++;
            }
        }
        return count;
    }

    public int getBookingCountByRoomType(String roomType) {
        int count = 0;
        Iterator<Booking> iterator = getBookingIterator();
        while (iterator != null && iterator.hasNext()) {
            Booking b = iterator.next();
            if (b != null && roomType.equalsIgnoreCase(b.getRoomType())) {
                count++;
            }
        }
        return count;
    }

    public double getRevenueByRoomType(String roomType) {
        double rev = 0.0;
        Iterator<Booking> iterator = getBookingIterator();
        while (iterator != null && iterator.hasNext()) {
            Booking b = iterator.next();
            if (b != null && roomType.equalsIgnoreCase(b.getRoomType()) && b.getBill() != null) {
                rev += b.getBill().getGrandTotal();
            }
        }
        return rev;
    }
}
