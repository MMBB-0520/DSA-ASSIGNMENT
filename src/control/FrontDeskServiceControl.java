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
import java.time.temporal.ChronoUnit;
import java.util.Iterator;

/**
 * Control class for Front-Desk Service module.
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
     * today or past. Evaluates 23:59 No-Show cutoff rules.
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
                    // Skip CANCELLED bookings (keeps in-memory BST search tree active-only, honors
                    // delete())
                    if (Booking.STATUS_CANCELLED.equalsIgnoreCase(b.getStatus())) {
                        continue;
                    }

                    // 1. Auto-mark NO-SHOW bookings
                    // 
                    LocalDateTime noShowCutoff = (b.getCheckInDateTime() != null)
                            ? b.getCheckInDateTime().toLocalDate().atTime(23, 59)
                            : null;

                    if ((Booking.STATUS_RESERVED.equalsIgnoreCase(b.getStatus())
                            || Booking.STATUS_CONFIRMED.equalsIgnoreCase(b.getStatus())
                            || Booking.STATUS_PENDING.equalsIgnoreCase(b.getStatus()))
                            && noShowCutoff != null
                            && LocalDateTime.now().isAfter(noShowCutoff)) {

                        b.setStatus(Booking.STATUS_NO_SHOW);
                        bookingUpdated = true;

                        // Release assigned room back to AVAILABLE if any
                        if (b.getRoomNo() != null && !b.getRoomNo().isEmpty()
                                && !"Unassigned".equalsIgnoreCase(b.getRoomNo())) {
                            if (allRooms != null) {
                                String[] roomNos = b.getRoomNo().split(",\\s*");
                                for (String rNo : roomNos) {
                                    for (Room r : allRooms) {
                                        if (r != null && r.getRoomNo().equalsIgnoreCase(rNo.trim())) {
                                            r.setStatus(Room.STATUS_AVAILABLE);
                                            roomUpdated = true;
                                            break;
                                        }
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
                            && !b.getCheckInDateTime().toLocalDate().isAfter(LocalDate.now())) {

                        if (allRooms != null) {
                            int needed = b.getNumOfRooms() > 0 ? b.getNumOfRooms() : 1;
                            ListInterface<String> assignedRooms = new CustomLinkedList<>();
                            for (Room r : allRooms) {
                                if (r != null && r.getRoomType().equalsIgnoreCase(b.getRoomType())
                                        && Room.STATUS_AVAILABLE.equalsIgnoreCase(r.getStatus())) {
                                    assignedRooms.add(r.getRoomNo());
                                    r.setStatus(Room.STATUS_BOOKED);
                                    roomUpdated = true;
                                    if (assignedRooms.size() == needed) {
                                        break;
                                    }
                                }
                            }
                            if (!assignedRooms.isEmpty()) {
                                b.setRoomNo(String.join(", ", assignedRooms));
                                b.setStatus(Booking.STATUS_CONFIRMED);
                                bookingUpdated = true;
                            }
                        }
                    }
                    // Auto-update room status to LATE CHECK-OUT tier if CHECKED_IN and past
                    // checkOutDateTime
                    if (Booking.STATUS_CHECKED_IN.equalsIgnoreCase(b.getStatus())
                            && b.getRoomNo() != null && !b.getRoomNo().isEmpty()
                            && b.getCheckOutDateTime() != null
                            && LocalDateTime.now().isAfter(b.getCheckOutDateTime())) {

                        long minutesOver = ChronoUnit.MINUTES.between(b.getCheckOutDateTime(),
                                LocalDateTime.now());
                        String lateRoomStatus;
                        if (minutesOver <= 30) {
                            lateRoomStatus = Room.STATUS_LATE_30MIN;
                        } else if (minutesOver <= 60) {
                            lateRoomStatus = Room.STATUS_LATE_1HRS;
                        } else {
                            lateRoomStatus = Room.STATUS_LATE_2HRS;
                        }

                        if (allRooms != null) {
                            String[] roomNos = b.getRoomNo().split(",\\s*");
                            for (String rNo : roomNos) {
                                for (Room r : allRooms) {
                                    if (r != null && r.getRoomNo().equalsIgnoreCase(rNo.trim())) {
                                        if (!lateRoomStatus.equalsIgnoreCase(r.getStatus())) {
                                            r.setStatus(lateRoomStatus);
                                            roomUpdated = true;
                                        }
                                        break;
                                    }
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

        // 4. Rebalance BST to achieve minimum balanced height
        bookingTree.rebalance();
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
                checkOut, roomChargeTotal * 0.30);
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
                || Booking.STATUS_NO_SHOW.equalsIgnoreCase(status)
                || Booking.STATUS_COMPLETED.equalsIgnoreCase(status)) {
            return false;
        }

        // Validate early check-in before 3:00 PM on check-in date
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime checkInDate = booking.getCheckInDateTime();
        if (checkInDate != null && checkInDate.toLocalDate().equals(now.toLocalDate()) && now.getHour() < 15) {
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
                int needed = booking.getNumOfRooms() > 0 ? booking.getNumOfRooms() : 1;
                ListInterface<String> assignedRooms = new CustomLinkedList<>();
                for (Room room : allRooms) {
                    if (room != null && room.getRoomType().equalsIgnoreCase(booking.getRoomType())
                            && Room.STATUS_AVAILABLE.equalsIgnoreCase(room.getStatus())) {
                        assignedRooms.add(room.getRoomNo());
                        room.setStatus(Room.STATUS_BOOKED);
                        if (assignedRooms.size() == needed) {
                            break;
                        }
                    }
                }
                if (!assignedRooms.isEmpty()) {
                    booking.setRoomNo(String.join(", ", assignedRooms));
                }
            } else {
                String[] roomNos = booking.getRoomNo().split(",\\s*");
                for (String rNo : roomNos) {
                    for (Room room : allRooms) {
                        if (room != null && room.getRoomNo().equalsIgnoreCase(rNo.trim())) {
                            room.setStatus(Room.STATUS_BOOKED);
                            break;
                        }
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
            if (allRooms != null) {
                String[] roomNos = booking.getRoomNo().split(",\\s*");
                for (String rNo : roomNos) {
                    for (Room room : allRooms) {
                        if (room != null && room.getRoomNo().equalsIgnoreCase(rNo.trim())) {
                            room.setStatus(Room.STATUS_DIRTY);
                            break;
                        }
                    }
                }
                roomDAO.saveAllRooms(allRooms);
            }
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
        boolean isRefundable = (hoursUntilCheckIn >= 24);

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
        if (booking.getRoomNo() != null && !booking.getRoomNo().isEmpty()
                && !"Unassigned".equalsIgnoreCase(booking.getRoomNo())) {
            Room[] allRooms = roomDAO.getAllRooms();
            if (allRooms != null) {
                String[] roomNos = booking.getRoomNo().split(",\\s*");
                for (String rNo : roomNos) {
                    for (Room r : allRooms) {
                        if (r != null && r.getRoomNo().equalsIgnoreCase(rNo.trim())) {
                            r.setStatus(Room.STATUS_AVAILABLE);
                        }
                    }
                }
                roomDAO.saveAllRooms(allRooms);
            }
        }

        // 1. Save updated status to DAO JSON file (preserves financial/audit history)
        bookingDAO.saveBooking(booking);

        // 2. Delete node from in-memory BST search index (keeps BST lean & fast,
        // demonstrates delete() algorithm)
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
}