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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;

/**
 * Control class for the Front-Desk Service module.
 * Acts as the intermediary between the User Interface (UI), Data Access Objects
 * (DAO),
 * and underlying Abstract Data Types (ADT).
 * 
 * Note: Implements Object-Oriented best practices by depending on interface
 * abstractions
 * (e.g., BSTInterface, ListInterface) for dynamic filtering, sorting, and
 * business logic execution.
 *
 * @author Ng Yuen Qi
 */
public class FrontDeskServiceControl {

    // Hotel Policy Constants
    public static final int STANDARD_CHECK_IN_HOUR = 15; // 3:00 PM standard check-in time
    public static final int NO_SHOW_CUTOFF_HOUR = 23; // 23:59 PM No-Show evaluation cutoff hour
    public static final int NO_SHOW_CUTOFF_MINUTE = 59;
    public static final double BOOKING_DEPOSIT_PERCENTAGE = 0.30; // 30% booking deposit policy
    public static final long REFUNDABLE_CANCELLATION_HOURS = 24; // 24-hour cancellation rule for full deposit refund

    // Using interface types for variables
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
     * Evaluates No-Show cutoffs, auto-assigns rooms, updates late check-out
     * statuses,
     * and rebalances the BST.
     */
    public void loadData() {
        bookingTree.clear();
        Booking[] loaded = bookingDAO.getAllBookings();
        Room[] allRooms = roomDAO.getAllRooms();

        boolean roomUpdated = false;
        boolean bookingUpdated = false;

        if (loaded != null && loaded.length > 0) {
            for (Booking b : loaded) {
                if (b != null && b.getConfirmationNo() != null) {
                    // Skip CANCELLED bookings to maintain an active-only BST search index
                    if (Booking.STATUS_CANCELLED.equalsIgnoreCase(b.getStatus())) {
                        continue;
                    }

                    // 1. Process 23:59 No-Show cutoff
                    if (processNoShow(b, allRooms)) {
                        bookingUpdated = true;
                        roomUpdated = true;
                    }

                    // 2. Process Auto-Assign Room for today/past check-ins
                    if (autoAssignRoom(b, allRooms)) {
                        bookingUpdated = true;
                        roomUpdated = true;
                    }

                    // 3. Process Late Check-Out status updates
                    if (updateLateCheckoutStatus(b, allRooms)) {
                        roomUpdated = true;
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

        // 4. Rebalance BST to achieve minimum balanced height
        bookingTree.rebalance();
    }

    /**
     * Evaluates and marks NO-SHOW bookings past 23:59 cutoff.
     */
    private boolean processNoShow(Booking b, Room[] allRooms) {
        LocalDateTime noShowCutoff = (b.getCheckInDateTime() != null)
                ? b.getCheckInDateTime().toLocalDate().atTime(NO_SHOW_CUTOFF_HOUR, NO_SHOW_CUTOFF_MINUTE)
                : null;

        if ((Booking.STATUS_RESERVED.equalsIgnoreCase(b.getStatus())
                || Booking.STATUS_CONFIRMED.equalsIgnoreCase(b.getStatus())
                || Booking.STATUS_PENDING.equalsIgnoreCase(b.getStatus()))
                && noShowCutoff != null
                && LocalDateTime.now().isAfter(noShowCutoff)) {

            b.setStatus(Booking.STATUS_NO_SHOW);

            // Release assigned room back to AVAILABLE if any
            updateAssignedRoomsStatus(b, allRooms, Room.STATUS_AVAILABLE);
            return true;
        }
        return false;
    }

    /**
     * Auto-assigns available rooms for bookings whose check-in is today or past.
     */
    private boolean autoAssignRoom(Booking b, Room[] allRooms) {
        if (b == null || allRooms == null) {
            return false;
        }

        if (b.getRoomNo() == null || b.getRoomNo().isEmpty()
                || "Unassigned".equalsIgnoreCase(b.getRoomNo())) {

            int needed = b.getNumOfRooms() > 0 ? b.getNumOfRooms() : 1;
            ListInterface<String> assignedRooms = new CustomLinkedList<>();
            for (Room r : allRooms) {
                if (r != null && r.getRoomType().equalsIgnoreCase(b.getRoomType())
                        && Room.STATUS_AVAILABLE.equalsIgnoreCase(r.getStatus())) {
                    assignedRooms.add(r.getRoomNo());
                    r.setStatus(Room.STATUS_BOOKED);
                    if (assignedRooms.size() == needed) {
                        break;
                    }
                }
            }
            if (!assignedRooms.isEmpty()) {
                b.setRoomNo(String.join(", ", assignedRooms));
                if (Booking.STATUS_RESERVED.equalsIgnoreCase(b.getStatus())
                        || Booking.STATUS_PENDING.equalsIgnoreCase(b.getStatus())) {
                    b.setStatus(Booking.STATUS_CONFIRMED);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Auto-updates room status to LATE CHECK-OUT tier if past check-out time.
     */
    private boolean updateLateCheckoutStatus(Booking b, Room[] allRooms) {
        if (Booking.STATUS_CHECKED_IN.equalsIgnoreCase(b.getStatus())
                && b.getRoomNo() != null && !b.getRoomNo().isEmpty()
                && b.getCheckOutDateTime() != null
                && LocalDateTime.now().isAfter(b.getCheckOutDateTime())) {

            long minutesOver = ChronoUnit.MINUTES.between(b.getCheckOutDateTime(), LocalDateTime.now());
            String lateRoomStatus;
            if (minutesOver <= 30) {
                lateRoomStatus = Room.STATUS_LATE_30MIN;
            } else if (minutesOver <= 60) {
                lateRoomStatus = Room.STATUS_LATE_1HRS;
            } else {
                lateRoomStatus = Room.STATUS_LATE_2HRS;
            }

            return updateAssignedRoomsStatus(b, allRooms, lateRoomStatus);
        }
        return false;
    }

    /**
     * Helper method to update physical room status for all assigned room numbers in
     * a booking.
     * Encapsulates string splitting, room array scanning, and status updates (DRY
     * principle).
     * 
     * @param b            The booking containing the assigned room numbers.
     * @param allRooms     The array of all physical rooms in the system.
     * @param targetStatus The new status to be applied (e.g., Room.STATUS_DIRTY).
     * @return true if at least one room's status was updated, false otherwise.
     */
    private boolean updateAssignedRoomsStatus(Booking b, Room[] allRooms, String targetStatus) {
        if (b == null || b.getRoomNo() == null || b.getRoomNo().isEmpty()
                || "Unassigned".equalsIgnoreCase(b.getRoomNo()) || allRooms == null || targetStatus == null) {
            return false;
        }

        boolean updated = false;
        String[] roomNos = b.getRoomNo().split(",\\s*");
        for (String rNo : roomNos) {
            for (Room r : allRooms) {
                if (r != null && r.getRoomNo().equalsIgnoreCase(rNo.trim())) {
                    if (!targetStatus.equalsIgnoreCase(r.getStatus())) {
                        r.setStatus(targetStatus);
                        updated = true;
                    }
                    break;
                }
            }
        }
        return updated;
    }

    public void reloadData() {
        loadData();
    }

    /**
     * Searches for a booking by confirmation number using the Binary Search Tree.
     * Time Complexity: O(log n) average.
     *
     * @param confirmationNo the booking confirmation number
     * @return the matching booking, or null if not found
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
     * Adds a new booking to the BST and persists it through the DAO.
     *
     * @param booking the booking to add
     * @return true if the booking was successfully added; false otherwise
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

    public int getTreeHeight() {
        return bookingTree.getHeight();
    }

    public Iterator<Booking> getBookingIterator() {
        return bookingTree.getIterator();
    }

    /**
     * Registers a future reservation booking and persists changes to BST and JSON.
     */
    public Booking registerFutureBooking(String name, String contactNumber, String icPassport,
            String roomType, double pricePerNight, int numGuests, int numOfRooms,
            LocalDateTime checkIn, LocalDateTime checkOut, double depositPaid) {

        Guest existingGuest = findGuestByContact(contactNumber);
        Guest guest = (existingGuest != null) ? existingGuest
                : new Guest(generateGuestId(), name, contactNumber, icPassport);

        String confirmationNo;
        do {
            // Generate a unique 8-digit confirmation number to serve as the BST search key
            confirmationNo = String.format("%08d", (int) (Math.random() * 90000000) + 10000000);
        } while (containsConfirmationNo(confirmationNo));

        Booking booking = new Booking(confirmationNo, guest, roomType, pricePerNight, numGuests, numOfRooms, checkIn,
                checkOut,
                LocalDateTime.now());
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
            LocalDateTime checkIn, LocalDateTime checkOut) {
        double nights = Math.max(1,
                ChronoUnit.DAYS.between(checkIn.toLocalDate(), checkOut.toLocalDate()));
        double roomChargeTotal = pricePerNight * nights;
        return registerFutureBooking(name, contactNumber, icPassport, roomType, pricePerNight, numGuests, 1, checkIn,
                checkOut, roomChargeTotal * BOOKING_DEPOSIT_PERCENTAGE);
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
     * Processes check-in for a booking.
     * Validates standard check-in time, collects the required deposit (30% Booking
     * + RM200 Security),
     * auto-assigns a room if necessary, and updates status to CHECKED_IN.
     *
     * @param confirmationNo The 8-digit unique booking confirmation number.
     * @param cashTendered   The amount of cash handed by the guest at the counter.
     * @return true if check-in is successful; false if booking is invalid, too
     *         early, or cash is insufficient.
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
                || Booking.STATUS_NO_SHOW.equalsIgnoreCase(status)
                || Booking.STATUS_COMPLETED.equalsIgnoreCase(status)) {
            return false;
        }

        // Validate early check-in before 3:00 PM on check-in date
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime checkInDate = booking.getCheckInDateTime();
        if (checkInDate != null && checkInDate.toLocalDate().equals(now.toLocalDate())
                && now.getHour() < STANDARD_CHECK_IN_HOUR) {
            System.out.println(
                    "\n[!] Error: Standard Check-In time starts at 3:00 PM (15:00). Check-in before 3:00 PM is not allowed.");
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
                autoAssignRoom(booking, allRooms);
            } else {
                updateAssignedRoomsStatus(booking, allRooms, Room.STATUS_BOOKED);
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
     * Instantly updates physical room status to the corresponding LATE CHECKOUT tag.
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

        // Instantly update physical room status to corresponding LATE CHECKOUT tag
        String lateRoomStatus = switch (lateOption) {
            case 1 -> Room.STATUS_LATE_30MIN;
            case 2 -> Room.STATUS_LATE_1HRS;
            case 3 -> Room.STATUS_LATE_2HRS;
            default -> Room.STATUS_BOOKED;
        };

        Room[] allRooms = roomDAO.getAllRooms();
        if (updateAssignedRoomsStatus(booking, allRooms, lateRoomStatus)) {
            roomDAO.saveAllRooms(allRooms);
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
        Room[] allRooms = roomDAO.getAllRooms();
        if (updateAssignedRoomsStatus(booking, allRooms, Room.STATUS_DIRTY)) {
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

        // Step 1 & 2: Pre-clean filter parameters outside loop for performance
        final String cleanType = (roomTypeFilter == null) ? "" : roomTypeFilter.trim();
        final boolean isAllType = cleanType.isEmpty() || "ALL".equalsIgnoreCase(cleanType);

        ListInterface<Room> filteredList = new CustomLinkedList<>();

        for (Room room : allRooms) {
            if (room != null && Room.STATUS_AVAILABLE.equalsIgnoreCase(room.getStatus())) {
                boolean matchType = isAllType || cleanType.equalsIgnoreCase(room.getRoomType());

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

        // Step 3: Sort candidate rooms
        if ("Price".equalsIgnoreCase(sortBy)) {
            // Sort by Price per Night using Insertion Sort algorithm
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
        } else {
            // Sort by Room Number using BST ADT and inorder() traversal (Ascending Order)
            BSTInterface<Room> tempRoomTree = new MyBinarySearchTree<>();
            for (Room r : filtered) {
                if (r != null) {
                    tempRoomTree.insert(r);
                }
            }
            Object[] inorderArray = tempRoomTree.inorder();
            Room[] sortedByRoomNo = new Room[inorderArray.length];
            for (int i = 0; i < inorderArray.length; i++) {
                sortedByRoomNo[i] = (Room) inorderArray[i];
            }
            return sortedByRoomNo;
        }
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
                        && !b.getCheckInDateTime().toLocalDate().isAfter(LocalDate.now())) {
                    available -= (b.getNumOfRooms() > 0 ? b.getNumOfRooms() : 1);
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
    public int getAvailableRoomCountForPeriod(String roomType, LocalDateTime checkIn,
            LocalDateTime checkOut) {
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
                        || Booking.STATUS_NO_SHOW.equalsIgnoreCase(status)
                        || Booking.STATUS_COMPLETED.equalsIgnoreCase(status)) {
                    continue;
                }

                LocalDateTime bCheckIn = b.getCheckInDateTime();
                LocalDateTime bCheckOut = b.getCheckOutDateTime();

                if (bCheckIn != null && bCheckOut != null) {
                    // Standard interval overlap check
                    if (bCheckIn.isBefore(checkOut) && bCheckOut.isAfter(checkIn)) {
                        activeOverlappingBookings += (b.getNumOfRooms() > 0 ? b.getNumOfRooms() : 1);
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
                            || Room.STATUS_LATE_30MIN.equalsIgnoreCase(status)
                            || Room.STATUS_LATE_1HRS.equalsIgnoreCase(status)
                            || Room.STATUS_LATE_2HRS.equalsIgnoreCase(status)) {
                        alertList.add(r);
                    }
                }
            }
        }
        return alertList;
    }

    public int getStandardCleaningDurationMinutes(String roomType) {
        if (roomType == null)
            return 30;
        String type = roomType.trim().toLowerCase();
        if (type.contains("deluxe")) {
            return 40;
        } else if (type.contains("suite")) {
            return 45;
        } else {
            return 30;
        }
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
            if (b != null && b.getBill() != null
                    && Bill.PAYMENT_PAID_CASH.equalsIgnoreCase(b.getBill().getPaymentStatus())) {
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

    /**
     * Cancels a booking with automatic 24-hour refund/forfeit evaluation:
     * - Cancelled >= 24 hours before Check-In: Deposit is fully REFUNDABLE.
     * - Cancelled < 24 hours before Check-In: Deposit is FORFEITED (RM 0.00
     * refund).
     * Releases assigned room(s) back to AVAILABLE.
     */
    public String cancelBooking(String confirmationNo) {
        Booking booking = searchByConfirmationNo(confirmationNo);
        if (booking == null) {
            return "[X] Error: Booking not found for Confirmation No: " + confirmationNo;
        }

        String status = booking.getStatus();
        if (Booking.STATUS_CANCELLED.equalsIgnoreCase(status)) {
            return "[!] Notice: Booking is ALREADY CANCELLED.";
        }
        if (Booking.STATUS_CHECKED_IN.equalsIgnoreCase(status) || Booking.STATUS_COMPLETED.equalsIgnoreCase(status)) {
            return "[!] Error: Cannot cancel a booking that is currently " + status
                    + ". Please process Check-Out instead.";
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime checkInTime = booking.getCheckInDateTime();

        long hoursUntilCheckIn = (checkInTime != null)
                ? ChronoUnit.HOURS.between(now, checkInTime)
                : 0;

        Bill bill = booking.getBill();
        double depositPaid = (bill != null) ? bill.getCashTendered() : 0.0;
        boolean isRefundable = (hoursUntilCheckIn >= REFUNDABLE_CANCELLATION_HOURS);

        if (isRefundable) {
            if (bill != null) {
                bill.setPaymentStatus("REFUNDED");
                bill.setChangeDue(depositPaid);
            }
        } else {
            if (bill != null) {
                bill.setPaymentStatus("FORFEITED");
                bill.setChangeDue(0.0);
            }
        }

        booking.setStatus(Booking.STATUS_CANCELLED);

        // Release assigned room(s) back to AVAILABLE
        Room[] allRooms = roomDAO.getAllRooms();
        if (updateAssignedRoomsStatus(booking, allRooms, Room.STATUS_AVAILABLE)) {
            roomDAO.saveAllRooms(allRooms);
        }

        // 1. Save updated status to DAO JSON file (preserves audit history)
        bookingDAO.saveBooking(booking);

        // 2. Remove node from in-memory BST search index using BST delete() algorithm
        Booking dummyKey = new Booking(confirmationNo, null, null, 0, 0, null, null, null);
        bookingTree.delete(dummyKey);

        if (isRefundable) {
            return String.format(
                    "SUCCESS_REFUND:%d hrs prior to Check-In (>= 24 hrs rule). Full deposit of RM %.2f REFUNDED to guest.",
                    hoursUntilCheckIn, depositPaid);
        } else {
            return String.format(
                    "SUCCESS_FORFEIT:%d hrs prior to Check-In (< 24 hrs rule). Deposit of RM %.2f FORFEITED (RM 0.00 Refund).",
                    hoursUntilCheckIn, depositPaid);
        }
    }

    /**
     * Custom comparator function for sorting bookings based on specified criteria.
     */
    private int compareBookings(Booking b1, Booking b2, String sortBy) {
        if (b1 == null || b2 == null)
            return 0;
        if (sortBy == null)
            sortBy = "Revenue_Desc";

        switch (sortBy) {
            case "Revenue_Desc" -> {
                double rev1 = (b1.getBill() != null) ? b1.getBill().getGrandTotal() : 0.0;
                double rev2 = (b2.getBill() != null) ? b2.getBill().getGrandTotal() : 0.0;
                return Double.compare(rev2, rev1); // Descending order
            }
            case "Revenue_Asc" -> {
                double rev1 = (b1.getBill() != null) ? b1.getBill().getGrandTotal() : 0.0;
                double rev2 = (b2.getBill() != null) ? b2.getBill().getGrandTotal() : 0.0;
                return Double.compare(rev1, rev2); // Ascending order
            }
            case "Nights_Desc" -> {
                return Integer.compare(b2.getNights(), b1.getNights()); // Descending order
            }
            case "GuestName_Asc" -> {
                String n1 = (b1.getGuest() != null && b1.getGuest().getGuestName() != null)
                        ? b1.getGuest().getGuestName()
                        : "";
                String n2 = (b2.getGuest() != null && b2.getGuest().getGuestName() != null)
                        ? b2.getGuest().getGuestName()
                        : "";
                return n1.compareToIgnoreCase(n2);
            }
            case "Date_Asc" -> {
                if (b1.getCheckInDateTime() == null)
                    return 1;
                if (b2.getCheckInDateTime() == null)
                    return -1;
                return b1.getCheckInDateTime().compareTo(b2.getCheckInDateTime());
            }
            case "ConfirmationNo" -> {
                String c1 = (b1.getConfirmationNo() != null) ? b1.getConfirmationNo() : "";
                String c2 = (b2.getConfirmationNo() != null) ? b2.getConfirmationNo() : "";
                return c1.compareTo(c2);
            }
            default -> {
                return 0;
            }
        }
    }

    /**
     * Report 1 Algorithm Generator: Room Availability & Booking Demand Analysis
     * Pipeline: Filter -> Search Candidate Rooms -> Calculate Period Demand ->
     * Insertion Sort
     *
     * @param roomTypeFilter "ALL" or specific room type (e.g. Deluxe, Standard,
     *                       Suite)
     * @param checkInPeriod  Start of target date range
     * @param checkOutPeriod End of target date range
     * @param statusFilter   "AVAILABLE" or "ALL"
     * @param sortBy         "Price_Asc", "Price_Desc", "RoomNo_Asc"
     * @return ListInterface<Room> containing candidate rooms sorted by price/room
     *         number
     */
    public ListInterface<Room> generateRoomAvailabilityDemandReport(
            String roomTypeFilter,
            LocalDateTime checkInPeriod,
            LocalDateTime checkOutPeriod,
            String statusFilter,
            String sortBy) {

        Room[] allRooms = roomDAO.getAllRooms();
        Booking[] allBookings = bookingDAO.getAllBookings();
        ListInterface<Room> filteredList = new CustomLinkedList<>();

        // Pre-clean and normalize filter parameters outside the loop
        final String cleanType = (roomTypeFilter == null) ? "" : roomTypeFilter.trim();
        final boolean isAllType = cleanType.isEmpty() || "ALL".equalsIgnoreCase(cleanType);

        final String cleanStatus = (statusFilter == null) ? "" : statusFilter.trim();
        final boolean isAllStatus = cleanStatus.isEmpty() || "ALL".equalsIgnoreCase(cleanStatus);
        final boolean isAvailableFilter = "AVAILABLE".equalsIgnoreCase(cleanStatus)
                || Room.STATUS_AVAILABLE.equalsIgnoreCase(cleanStatus);

        // Algorithmic Optimization: Pre-collect occupied room numbers in target period
        // O(B)
        // Eliminates O(R * B) nested loop scanning bottleneck
        ListInterface<String> occupiedRoomNos = new CustomLinkedList<>();
        if (isAvailableFilter && checkInPeriod != null && checkOutPeriod != null && allBookings != null) {
            for (Booking b : allBookings) {
                if (b == null)
                    continue;

                String status = b.getStatus();
                if (Booking.STATUS_CANCELLED.equalsIgnoreCase(status)
                        || Booking.STATUS_NO_SHOW.equalsIgnoreCase(status)
                        || Booking.STATUS_COMPLETED.equalsIgnoreCase(status)) {
                    continue;
                }

                LocalDateTime bCheckIn = b.getCheckInDateTime();
                LocalDateTime bCheckOut = b.getCheckOutDateTime();
                if (bCheckIn != null && bCheckOut != null) {
                    if (bCheckIn.isBefore(checkOutPeriod) && bCheckOut.isAfter(checkInPeriod)) {
                        String rNo = b.getRoomNo();
                        if (rNo != null && !rNo.isEmpty() && !"Unassigned".equalsIgnoreCase(rNo)) {
                            String[] roomNos = rNo.split(",\\s*");
                            for (String roomNo : roomNos) {
                                occupiedRoomNos.add(roomNo.trim().toUpperCase());
                            }
                        }
                    }
                }
            }
        }

        if (allRooms != null && allRooms.length > 0) {
            for (Room r : allRooms) {
                if (r == null)
                    continue;

                // 1. Filter by Room Type
                boolean matchType = isAllType || cleanType.equalsIgnoreCase(r.getRoomType());

                // 2. Filter by Room Status (O(k) ADT lookup instead of O(B) full scan per room)
                boolean matchStatus;
                if (isAvailableFilter) {
                    boolean isStaticAvailable = Room.STATUS_AVAILABLE.equalsIgnoreCase(r.getStatus());
                    boolean isOccupied = (r.getRoomNo() != null)
                            && occupiedRoomNos.contains(r.getRoomNo().trim().toUpperCase());
                    matchStatus = isStaticAvailable && !isOccupied;
                } else {
                    matchStatus = isAllStatus || cleanStatus.equalsIgnoreCase(r.getStatus());
                }

                if (matchType && matchStatus) {
                    filteredList.add(r);
                }
            }
        }

        // Sort in-place directly on CustomLinkedList Linear ADT
        filteredList.sort((r1, r2) -> compareRoomsForReport(r1, r2, sortBy));
        return filteredList;
    }

    private int compareRoomsForReport(Room r1, Room r2, String sortBy) {
        if (r1 == null || r2 == null)
            return 0;
        if ("Price_Asc".equalsIgnoreCase(sortBy)) {
            int comp = Double.compare(r1.getPricePerNight(), r2.getPricePerNight());
            if (comp != 0)
                return comp;
            return r1.getRoomNo().compareTo(r2.getRoomNo());
        } else if ("Price_Desc".equalsIgnoreCase(sortBy)) {
            int comp = Double.compare(r2.getPricePerNight(), r1.getPricePerNight());
            if (comp != 0)
                return comp;
            return r1.getRoomNo().compareTo(r2.getRoomNo());
        } else { // RoomNo_Asc
            return r1.getRoomNo().compareTo(r2.getRoomNo());
        }
    }

    /**
     * Report 1 Calculation Helper: Computes occupancy rates and counts per room
     * type for date period.
     */
    public double[] calculatePeriodOccupancyMetrics(LocalDateTime checkInPeriod, LocalDateTime checkOutPeriod) {
        int stdAvail = getAvailableRoomCountForPeriod("Standard", checkInPeriod, checkOutPeriod);
        int delAvail = getAvailableRoomCountForPeriod("Deluxe", checkInPeriod, checkOutPeriod);
        int steAvail = getAvailableRoomCountForPeriod("Suite", checkInPeriod, checkOutPeriod);

        int[] stdCounts = getRoomTypeCounts("Standard");
        int[] delCounts = getRoomTypeCounts("Deluxe");
        int[] steCounts = getRoomTypeCounts("Suite");

        double stdTotal = stdCounts[1], stdBooked = Math.max(0, stdTotal - stdAvail);
        double delTotal = delCounts[1], delBooked = Math.max(0, delTotal - delAvail);
        double steTotal = steCounts[1], steBooked = Math.max(0, steTotal - steAvail);

        double stdOccPct = (stdTotal > 0) ? (stdBooked / stdTotal) * 100.0 : 0.0;
        double delOccPct = (delTotal > 0) ? (delBooked / delTotal) * 100.0 : 0.0;
        double steOccPct = (steTotal > 0) ? (steBooked / steTotal) * 100.0 : 0.0;

        double totalAllRooms = stdTotal + delTotal + steTotal;
        double totalAllBooked = stdBooked + delBooked + steBooked;
        double overallOccPct = (totalAllRooms > 0) ? (totalAllBooked / totalAllRooms) * 100.0 : 0.0;

        return new double[] {
                stdBooked, stdTotal, stdOccPct,
                delBooked, delTotal, delOccPct,
                steBooked, steTotal, steOccPct,
                totalAllBooked, totalAllRooms, overallOccPct
        };
    }

    /**
     * Report 1 Calculation Helper: Computes target physical rooms and active booked
     * rooms based on roomTypeFilter.
     * Index 0: targetPhysicalRooms, 1: targetActiveBooked, 2: targetNetAvailable,
     * 3: demandOccupancyRate
     */
    public double[] calculateTargetDemandMetrics(String roomTypeFilter, double[] occMetrics, int candidateCount) {
        double stdBooked = occMetrics[0], stdTotal = occMetrics[1];
        double delBooked = occMetrics[3], delTotal = occMetrics[4];
        double steBooked = occMetrics[6], steTotal = occMetrics[7];
        double totalAllBooked = occMetrics[9], totalAllRooms = occMetrics[10];

        double targetPhysicalRooms = candidateCount;
        double targetActiveBooked = 0;
        if ("ALL".equalsIgnoreCase(roomTypeFilter)) {
            targetPhysicalRooms = totalAllRooms;
            targetActiveBooked = totalAllBooked;
        } else if ("Standard".equalsIgnoreCase(roomTypeFilter)) {
            targetPhysicalRooms = stdTotal;
            targetActiveBooked = stdBooked;
        } else if ("Deluxe".equalsIgnoreCase(roomTypeFilter)) {
            targetPhysicalRooms = delTotal;
            targetActiveBooked = delBooked;
        } else if ("Suite".equalsIgnoreCase(roomTypeFilter)) {
            targetPhysicalRooms = steTotal;
            targetActiveBooked = steBooked;
        }

        double targetNetAvailable = Math.max(0, targetPhysicalRooms - targetActiveBooked);
        double demandOccupancyRate = (targetPhysicalRooms > 0)
                ? (targetActiveBooked / targetPhysicalRooms) * 100.0
                : 0.0;

        return new double[] { targetPhysicalRooms, targetActiveBooked, targetNetAvailable, demandOccupancyRate };
    }

    /**
     * Report 1 Strategic Recommendation Generator.
     * Returns String array [Header, Body] based on demand occupancy rate.
     */
    public String[] generateDemandRecommendation(double demandOccupancyRate) {
        if (demandOccupancyRate >= 70.0) {
            return new String[] {
                    String.format("  [★] HIGH DEMAND ALERT (Occupancy: %.1f%% >= 70%%):", demandOccupancyRate),
                    "      Recommendation: Implement Dynamic Surge Pricing (+15-20% rate increase \n                      for remaining rooms).\n"
            };
        } else if (demandOccupancyRate <= 30.0) {
            return new String[] {
                    String.format("  [!] LOW DEMAND ALERT (Occupancy: %.1f%% <= 30%%):", demandOccupancyRate),
                    "      Recommendation: Launch Promotional Marketing / Bundle Packages to boost \n                      room utilization.\n"
            };
        } else {
            return new String[] {
                    String.format("  [√] BALANCED DEMAND (Occupancy: %.1f%%):", demandOccupancyRate),
                    "      Recommendation: Demand rate is healthy. Maintain standard pricing \n                      strategy.\n"
            };
        }
    }

    /**
     * Report 1 Calculation Helper: Finds highest occupancy and availability room
     * types.
     * Returns String array [topOccType, topOccVal, topAvailType, topAvailPct]
     */
    public String[] calculateTopOccupancyAndAvailabilityTypes(double stdOccPct, double delOccPct, double steOccPct) {
        String topOccType = (stdOccPct >= delOccPct && stdOccPct >= steOccPct) ? "Standard"
                : ((delOccPct >= steOccPct) ? "Deluxe" : "Suite");
        double topOccVal = Math.max(stdOccPct, Math.max(delOccPct, steOccPct));
        String topAvailType = (stdOccPct <= delOccPct && stdOccPct <= steOccPct) ? "Standard"
                : ((delOccPct <= steOccPct) ? "Deluxe" : "Suite");
        double topAvailPct = 100.0 - Math.min(stdOccPct, Math.min(delOccPct, steOccPct));

        return new String[] { topOccType, String.format("%.1f", topOccVal), topAvailType,
                String.format("%.1f", topAvailPct) };
    }

    /**
     * Report 2 (Option A Generator): Daily Arrivals, Departures & In-House Action
     * Report
     * Primary Front-Desk shift handover report filtering by target shift date
     * (Default: Today)
     * and operational mode (ARRIVALS_DUE, DEPARTURES_DUE, IN_HOUSE_STAY,
     * OVERDUE_ALERT).
     *
     * @param shiftDate      target shift date (e.g. LocalDate.now())
     * @param opModeFilter   "ALL", "ARRIVALS_DUE", "DEPARTURES_DUE",
     *                       "IN_HOUSE_STAY", "OVERDUE_ALERT"
     * @param roomTypeFilter "ALL" or specific room type
     * @param sortBy         "GuestName_Asc", "Date_Asc", "CheckOut_Asc",
     *                       "ConfirmationNo", "RoomNo_Asc"
     * @return ListInterface<Booking> sorted operational bookings
     */
    public ListInterface<Booking> generateDailyOperationalActionReport(
            LocalDate shiftDate,
            String opModeFilter,
            String roomTypeFilter,
            String sortBy) {

        if (shiftDate == null) {
            shiftDate = LocalDate.now();
        }

        Booking[] allBookings = bookingDAO.getAllBookings();
        ListInterface<Booking> filteredList = new CustomLinkedList<>();

        final String cleanOpMode = (opModeFilter == null) ? "ALL" : opModeFilter.trim().toUpperCase();
        final String cleanType = (roomTypeFilter == null) ? "" : roomTypeFilter.trim();
        final boolean isAllType = cleanType.isEmpty() || "ALL".equalsIgnoreCase(cleanType);
        final LocalDateTime now = LocalDateTime.now();

        if (allBookings != null && allBookings.length > 0) {
            for (Booking b : allBookings) {
                if (b == null)
                    continue;

                // 1. Filter by Room Type
                boolean typeMatch = isAllType || cleanType.equalsIgnoreCase(b.getRoomType());
                if (!typeMatch)
                    continue;

                LocalDate bCheckInDate = (b.getCheckInDateTime() != null) ? b.getCheckInDateTime().toLocalDate() : null;
                LocalDate bCheckOutDate = (b.getCheckOutDateTime() != null) ? b.getCheckOutDateTime().toLocalDate()
                        : null;
                String status = (b.getStatus() != null) ? b.getStatus() : "";

                // Determine operational classifications
                boolean isPendingArrival = (bCheckInDate != null && bCheckInDate.equals(shiftDate))
                        && (Booking.STATUS_RESERVED.equalsIgnoreCase(status)
                                || Booking.STATUS_CONFIRMED.equalsIgnoreCase(status));

                boolean isPendingDeparture = (bCheckOutDate != null && bCheckOutDate.equals(shiftDate))
                        && (Booking.STATUS_CHECKED_IN.equalsIgnoreCase(status)
                                || Booking.STATUS_CONFIRMED.equalsIgnoreCase(status));

                boolean isInHouseStay = (bCheckInDate != null && bCheckOutDate != null)
                        && (!bCheckInDate.isAfter(shiftDate) && bCheckOutDate.isAfter(shiftDate))
                        && Booking.STATUS_CHECKED_IN.equalsIgnoreCase(status);

                boolean isOverdueAlert = (b.getCheckOutDateTime() != null)
                        && now.isAfter(b.getCheckOutDateTime())
                        && (Booking.STATUS_CHECKED_IN.equalsIgnoreCase(status)
                                || Booking.STATUS_CONFIRMED.equalsIgnoreCase(status));

                boolean opMatch = switch (cleanOpMode) {
                    case "ARRIVALS_DUE" -> isPendingArrival;
                    case "DEPARTURES_DUE" -> isPendingDeparture;
                    case "IN_HOUSE_STAY" -> isInHouseStay;
                    case "OVERDUE_ALERT" -> isOverdueAlert;
                    default -> (isPendingArrival || isPendingDeparture || isInHouseStay || isOverdueAlert);
                };

                if (opMatch) {
                    filteredList.add(b);
                }
            }
        }

        // Sort in-place directly on CustomLinkedList Linear ADT
        filteredList.sort((b1, b2) -> compareBookings(b1, b2, sortBy));
        return filteredList;
    }

    /**
     * Report 2 Calculation Helper: Computes operational shift counts across
     * bookings.
     * Index 0: totalCount
     * Index 1: pendingArrivals
     * Index 2: pendingDepartures
     * Index 3: inHouseStays
     * Index 4: overdueLateCheckouts
     */
    public double[] calculateOperationalActionMetrics(ListInterface<Booking> reportBookings, LocalDate shiftDate) {
        if (shiftDate == null)
            shiftDate = LocalDate.now();

        int totalCount = (reportBookings != null) ? reportBookings.size() : 0;
        int pendingArrivals = 0;
        int pendingDepartures = 0;
        int inHouseStays = 0;
        int overdueLateCheckouts = 0;
        LocalDateTime now = LocalDateTime.now();

        if (reportBookings != null) {
            for (Booking b : reportBookings) {
                if (b == null)
                    continue;

                LocalDate bCheckInDate = (b.getCheckInDateTime() != null) ? b.getCheckInDateTime().toLocalDate() : null;
                LocalDate bCheckOutDate = (b.getCheckOutDateTime() != null) ? b.getCheckOutDateTime().toLocalDate()
                        : null;
                String status = (b.getStatus() != null) ? b.getStatus() : "";

                if (bCheckInDate != null && bCheckInDate.equals(shiftDate)
                        && (Booking.STATUS_RESERVED.equalsIgnoreCase(status)
                                || Booking.STATUS_CONFIRMED.equalsIgnoreCase(status))) {
                    pendingArrivals++;
                }

                if (bCheckOutDate != null && bCheckOutDate.equals(shiftDate)
                        && (Booking.STATUS_CHECKED_IN.equalsIgnoreCase(status)
                                || Booking.STATUS_CONFIRMED.equalsIgnoreCase(status))) {
                    pendingDepartures++;
                }

                if (bCheckInDate != null && bCheckOutDate != null
                        && (!bCheckInDate.isAfter(shiftDate) && bCheckOutDate.isAfter(shiftDate))
                        && Booking.STATUS_CHECKED_IN.equalsIgnoreCase(status)) {
                    inHouseStays++;
                }

                if (b.getCheckOutDateTime() != null && now.isAfter(b.getCheckOutDateTime())
                        && (Booking.STATUS_CHECKED_IN.equalsIgnoreCase(status)
                                || Booking.STATUS_CONFIRMED.equalsIgnoreCase(status))) {
                    overdueLateCheckouts++;
                }
            }
        }

        double totalOutstandingBalance = 0.0;
        if (reportBookings != null) {
            for (Booking b : reportBookings) {
                if (b == null || b.getBill() == null)
                    continue;
                String payStatus = (b.getBill().getPaymentStatus() != null) ? b.getBill().getPaymentStatus() : "";
                if (!"PAID".equalsIgnoreCase(payStatus) && !"PAID (CASH)".equalsIgnoreCase(payStatus)
                        && !"CASH".equalsIgnoreCase(payStatus)) {
                    double due = Math.max(0.0, b.getBill().getGrandTotal() - b.getBill().getCashTendered());
                    totalOutstandingBalance += due;
                }
            }
        }

        return new double[] { totalCount, pendingArrivals, pendingDepartures, inHouseStays, overdueLateCheckouts,
                totalOutstandingBalance };
    }

    /**
     * Report 2 Action Recommendation Generator.
     * Returns String[] [Header, Body] based on operational action metrics.
     */
    public String[] generateOperationalActionRecommendation(double[] opMetrics) {
        int overdue = (int) opMetrics[4];
        int arrivals = (int) opMetrics[1];
        int departures = (int) opMetrics[2];
        double totalBalanceDue = (opMetrics.length > 5) ? opMetrics[5] : 0.0;

        if (overdue > 0) {
            return new String[] {
                    String.format("  [!] URGENT LATE CHECK-OUT ALERT (%d guest(s) exceeded scheduled check-out time):",
                            overdue),
                    String.format(
                            "      Action Plan: Call guest contact numbers immediately. Verify outstanding balance (RM %.2f total) before checkout.",
                            totalBalanceDue)
            };
        } else if (arrivals > 5) {
            return new String[] {
                    String.format("  [★] HIGH ARRIVAL VOLUME ALERT (%d guest(s) due to arrive today):", arrivals),
                    "      Action Plan: Pre-assign rooms and coordinate with Housekeeping to ensure key cards and rooms are ready."
            };
        } else {
            return new String[] {
                    String.format("  [√] NORMAL SHIFT FLOW (%d arrival(s), %d departure(s) today):", arrivals,
                            departures),
                    String.format("      Action Plan: Shift workload balanced. Total outstanding collection: RM %.2f.",
                            totalBalanceDue)
            };
        }
    }

    /**
     * Report 2 Calculation Helper: Computes primary operational focus and overdue
     * status text.
     * Returns String[] [topFocusText, overdueText]
     */
    public String[] calculateOperationalTopBreakdown(double[] opMetrics) {
        int arrivals = (int) opMetrics[1];
        int departures = (int) opMetrics[2];
        int inHouse = (int) opMetrics[3];
        int overdue = (int) opMetrics[4];

        String topFocus = "Standard Reception Service";
        if (overdue > 0) {
            topFocus = "Overdue Room Follow-Up & Outstanding Balance Collection";
        } else if (arrivals >= departures && arrivals >= inHouse) {
            topFocus = String.format("Check-In Arrival Processing (%d Guest(s))", arrivals);
        } else if (departures >= inHouse) {
            topFocus = String.format("Check-Out Departure Processing (%d Guest(s))", departures);
        } else {
            topFocus = String.format("In-House Guest Assistance (%d Stay(s))", inHouse);
        }

        String overdueText = (overdue > 0)
                ? String.format("%d Room(s) Exceeded Scheduled Check-Out Time", overdue)
                : "All Guest Departures On Schedule";

        return new String[] { topFocus, overdueText };
    }

    /**
     * Report 2 Operational Classification Tag Helper: Determines the action status
     * tag for a single booking row.
     */
    public String getBookingOperationalActionTag(Booking b, LocalDate shiftDate) {
        if (b == null)
            return "N/A";
        if (shiftDate == null)
            shiftDate = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        String status = (b.getStatus() != null) ? b.getStatus() : "";

        if (b.getCheckOutDateTime() != null && now.isAfter(b.getCheckOutDateTime())
                && (Booking.STATUS_CHECKED_IN.equalsIgnoreCase(status)
                        || Booking.STATUS_CONFIRMED.equalsIgnoreCase(status))) {
            return "OVERDUE ALERT!";
        }

        LocalDate bCheckInDate = (b.getCheckInDateTime() != null) ? b.getCheckInDateTime().toLocalDate() : null;
        LocalDate bCheckOutDate = (b.getCheckOutDateTime() != null) ? b.getCheckOutDateTime().toLocalDate() : null;

        if (bCheckInDate != null && bCheckInDate.equals(shiftDate)
                && (Booking.STATUS_RESERVED.equalsIgnoreCase(status)
                        || Booking.STATUS_CONFIRMED.equalsIgnoreCase(status))) {
            return "Check-In Due";
        }

        if (bCheckOutDate != null && bCheckOutDate.equals(shiftDate)
                && (Booking.STATUS_CHECKED_IN.equalsIgnoreCase(status)
                        || Booking.STATUS_CONFIRMED.equalsIgnoreCase(status))) {
            return "Check-Out Due";
        }

        if (bCheckInDate != null && bCheckOutDate != null
                && (!bCheckInDate.isAfter(shiftDate) && bCheckOutDate.isAfter(shiftDate))
                && Booking.STATUS_CHECKED_IN.equalsIgnoreCase(status)) {
            return "In-House Stay";
        }

        return status;
    }

    /**
     * Report 2 Payment Balance Tag Helper: Calculates outstanding balance or
     * payment status for a booking.
     */
    public String getBookingPaymentBalanceTag(Booking b) {
        if (b == null || b.getBill() == null)
            return "UNPAID";
        Bill bill = b.getBill();
        String payStatus = (bill.getPaymentStatus() != null) ? bill.getPaymentStatus() : "UNPAID";
        if ("PAID".equalsIgnoreCase(payStatus) || "PAID (CASH)".equalsIgnoreCase(payStatus)
                || "CASH".equalsIgnoreCase(payStatus)) {
            return "PAID";
        }
        double grandTotal = bill.getGrandTotal();
        double paid = bill.getCashTendered();
        double balanceDue = Math.max(0.0, grandTotal - paid);
        if (balanceDue <= 0.01) {
            return "PAID";
        }
        return String.format("RM %.2f Due", balanceDue);
    }

    /**
     * Report 2 Guest Contact Number Helper.
     */
    public String getGuestContactNumber(Booking b) {
        if (b != null && b.getGuest() != null && b.getGuest().getContactNumber() != null) {
            return b.getGuest().getContactNumber();
        }
        return "N/A";
    }

    /**
     * Report 2 Compact Shift Time Summary Helper.
     * Formats Check-In / Check-Out time compactly relative to shift target date.
     */
    public String getBookingShiftTimeSummary(Booking b, LocalDate shiftDate) {
        if (b == null)
            return "N/A";
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter monthDayFmt = DateTimeFormatter.ofPattern("MM-dd");

        String inStr = "N/A";
        if (b.getCheckInDateTime() != null) {
            if (shiftDate != null && b.getCheckInDateTime().toLocalDate().equals(shiftDate)) {
                inStr = b.getCheckInDateTime().format(timeFmt);
            } else {
                inStr = b.getCheckInDateTime().format(monthDayFmt);
            }
        }

        String outStr = "N/A";
        if (b.getCheckOutDateTime() != null) {
            if (shiftDate != null && b.getCheckOutDateTime().toLocalDate().equals(shiftDate)) {
                outStr = b.getCheckOutDateTime().format(timeFmt);
            } else {
                outStr = b.getCheckOutDateTime().format(monthDayFmt);
            }
        }

        return inStr + " -> " + outStr;
    }
}