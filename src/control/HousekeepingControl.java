package control;

import adt.ArrayStack;
import adt.CustomLinkedList;
import adt.ListInterface;
import adt.StackInterface;
import dao.RoomDAO;
import dao.impl.RoomDAOImpl;
import entity.Room;
import entity.RoomTaskLog;

/**
 * HousekeepingControl class manages room cleaning, task logging,
 * supervisor inspection, and rollback/undo history for the Housekeeping module.
 * 
 * @author Pang Jia Yie
 */
public class HousekeepingControl {
    private ListInterface<RoomTaskLog> taskLogList; // Linear ADT: task history for reporting
    private StackInterface<RoomTaskLog> undoStack; // Stack ADT: LIFO for rollback/undo
    private ListInterface<Room> roomList; // Linear ADT: room list (custom, NOT java.util)
    private RoomDAO roomDAO; // DAO: shared data layer with other modules

    public HousekeepingControl() {
        this.taskLogList = new CustomLinkedList<>();
        this.undoStack = new ArrayStack<>();
        this.roomList = new CustomLinkedList<>();
        this.roomDAO = new RoomDAOImpl();
        loadRoomsFromData();
        initSampleTaskLogs();
    }

    private void initSampleTaskLogs() {
        if (taskLogList.isEmpty()) {
            long now = System.currentTimeMillis();
            long min = 60 * 1000L;
            long hour = 3600 * 1000L;

            // --- Cleaning Task 1: Standard Room - Completed on time by S001 (Pass
            // benchmark)
            RoomTaskLog log1 = new RoomTaskLog("101", "Standard", "Dirty", "Cleaned", "S001", 30);
            log1.setStartTime(now - 5 * hour);
            log1.setEndTime(now - 5 * hour + 24 * min);
            log1.setPenaltyFine(0.0);
            taskLogList.add(log1);
            undoStack.push(log1);

            // --- Inspection Task 1: Standard Room - Inspected & Approved by SP001
            RoomTaskLog log2 = new RoomTaskLog("101", "Standard", "Cleaned", "Inspected", "SP001", 30);
            log2.setStartTime(now - 4 * hour - 30 * min);
            log2.setEndTime(now - 4 * hour - 25 * min);
            log2.setPenaltyFine(0.0);
            taskLogList.add(log2);
            undoStack.push(log2);

            // --- Cleaning Task 2: Deluxe Room - Exceeded duration by 25 mins by S002 (RM
            // 20 fine)
            RoomTaskLog log3 = new RoomTaskLog("203", "Deluxe", "Dirty", "Cleaned", "S002", 40);
            log3.setStartTime(now - 4 * hour);
            log3.setEndTime(now - 4 * hour + 65 * min); // 65 mins vs 40 mins -> 25 mins overdue
            log3.setPenaltyFine(20.0);
            taskLogList.add(log3);
            undoStack.push(log3);

            // --- Cleaning Task 3: Suite Room - Completed on time by S003
            RoomTaskLog log4 = new RoomTaskLog("301", "Suite", "Dirty", "Cleaned", "S003", 45);
            log4.setStartTime(now - 3 * hour);
            log4.setEndTime(now - 3 * hour + 42 * min);
            log4.setPenaltyFine(0.0);
            taskLogList.add(log4);
            undoStack.push(log4);

            // --- Inspection Task 2: Suite Room - Failed inspection by SP002 (Defects found
            // -> Revert to Dirty)
            RoomTaskLog log5 = new RoomTaskLog("301", "Suite", "Cleaned", "Inspected (Failed)", "SP002", 45);
            log5.setStartTime(now - 2 * hour - 10 * min);
            log5.setEndTime(now - 2 * hour - 5 * min);
            log5.setPenaltyFine(0.0);
            taskLogList.add(log5);
            undoStack.push(log5);

            // --- Cleaning Task 4: Standard Room - Completed on time by S001
            RoomTaskLog log6 = new RoomTaskLog("105", "Standard", "Dirty", "Cleaned", "S001", 30);
            log6.setStartTime(now - 2 * hour);
            log6.setEndTime(now - 2 * hour + 26 * min);
            log6.setPenaltyFine(0.0);
            taskLogList.add(log6);
            undoStack.push(log6);

            // --- Inspection Task 3: Standard Room - Inspected & Approved by SP001
            RoomTaskLog log7 = new RoomTaskLog("105", "Standard", "Cleaned", "Inspected", "SP001", 30);
            log7.setStartTime(now - 90 * min);
            log7.setEndTime(now - 85 * min);
            log7.setPenaltyFine(0.0);
            taskLogList.add(log7);
            undoStack.push(log7);

            // --- Cleaning Task 5: Deluxe Room - Exceeded duration by 35 mins by S004 (RM
            // 30 fine)
            RoomTaskLog log8 = new RoomTaskLog("205", "Deluxe", "Dirty", "Cleaned", "S004", 40);
            log8.setStartTime(now - 80 * min);
            log8.setEndTime(now - 80 * min + 75 * min); // 75 mins vs 40 mins -> 35 mins overdue
            log8.setPenaltyFine(30.0);
            taskLogList.add(log8);
            undoStack.push(log8);

            // --- Inspection Task 4: Deluxe Room - Inspected & Approved by SP002
            RoomTaskLog log9 = new RoomTaskLog("205", "Deluxe", "Cleaned", "Inspected", "SP002", 40);
            log9.setStartTime(now - 40 * min);
            log9.setEndTime(now - 35 * min);
            log9.setPenaltyFine(0.0);
            taskLogList.add(log9);
            undoStack.push(log9);

            // --- Cleaning Task 6: Suite Room - Completed on time by S002
            RoomTaskLog log10 = new RoomTaskLog("303", "Suite", "Dirty", "Cleaned", "S002", 45);
            log10.setStartTime(now - 35 * min);
            log10.setEndTime(now - 35 * min + 40 * min);
            log10.setPenaltyFine(0.0);
            taskLogList.add(log10);
            undoStack.push(log10);

            // --- Cleaning Task 7: Standard Room - Completed on time by S001
            RoomTaskLog log11 = new RoomTaskLog("102", "Standard", "Dirty", "Cleaned", "S001", 30);
            log11.setStartTime(now - 25 * min);
            log11.setEndTime(now - 25 * min + 18 * min);
            log11.setPenaltyFine(0.0);
            taskLogList.add(log11);
            undoStack.push(log11);

            // --- Inspection Task 5: Standard Room - Inspected & Approved by SP001
            RoomTaskLog log12 = new RoomTaskLog("102", "Standard", "Cleaned", "Inspected", "SP001", 30);
            log12.setStartTime(now - 7 * min);
            log12.setEndTime(now - 2 * min);
            log12.setPenaltyFine(0.0);
            taskLogList.add(log12);
            undoStack.push(log12);
        }
    }

    /**
     * Loads rooms from RoomDAO (same data source as Walk-In Registration & Front
     * Desk modules).
     * Uses CustomLinkedList (Linear ADT) instead of java.util.ArrayList.
     */
    private void loadRoomsFromData() {
        roomList.clear();
        Room[] loaded = roomDAO.getAllRooms();
        if (loaded != null && loaded.length > 0) {
            for (Room r : loaded) {
                roomList.add(r);
            }
        } else {
            // Fallback default rooms if file is empty
            roomList.add(new Room("101", "Deluxe Suite", 250.0, "Dirty"));
            roomList.add(new Room("102", "Standard Room", 120.0, "Cleaning In Progress"));
            roomList.add(new Room("103", "Standard Room", 120.0, "Cleaned"));
            roomList.add(new Room("104", "Ocean View", 300.0, "Inspected"));
            roomList.add(new Room("105", "Presidents Suite", 800.0, "Ready for Check-In"));
        }
    }

    /**
     * Filters and returns rooms with status 'Dirty' (checked-out rooms ready for
     * cleaning)
     * using CustomLinkedList (Linear ADT).
     */
    public Room[] getDirtyRooms() {
        loadRoomsFromData(); // Always refresh latest room status from RoomDAO / rooms.json
        ListInterface<Room> dirtyList = new CustomLinkedList<>();
        for (int i = 1; i <= roomList.size(); i++) {
            Room r = roomList.get(i);
            if (r != null) {
                String st = r.getStatus().toLowerCase();
                if (st.startsWith("dirty") || st.contains("late check-out")) {
                    dirtyList.add(r);
                }
            }
        }
        Object[] rawArray = dirtyList.toArray();
        Room[] result = new Room[rawArray.length];
        for (int i = 0; i < rawArray.length; i++) {
            result[i] = (Room) rawArray[i];
        }
        return result;
    }

    /**
     * Saves rooms via RoomDAO (shared data layer — changes are visible to all
     * modules).
     * Converts CustomLinkedList to Room[] array using toArray().
     */
    public void saveRoomsToData() {
        Object[] rawArray = roomList.toArray();
        Room[] rooms = new Room[rawArray.length];
        for (int i = 0; i < rawArray.length; i++) {
            rooms[i] = (Room) rawArray[i];
        }
        roomDAO.saveAllRooms(rooms);
    }

    /**
     * Returns all rooms as ListInterface (custom ADT).
     * Other modules can iterate via for-each (CustomLinkedList implements
     * Iterable).
     */
    public ListInterface<Room> getAllRooms() {
        return roomList;
    }

    /**
     * Returns all rooms as an array for UI display.
     */
    public Room[] getAllRoomsArray() {
        Object[] rawArray = roomList.toArray();
        Room[] rooms = new Room[rawArray.length];
        for (int i = 0; i < rawArray.length; i++) {
            rooms[i] = (Room) rawArray[i];
        }
        return rooms;
    }

    /**
     * Filters rooms waiting for inspection using CustomLinkedList (Linear ADT)
     * and returns them as an array for UI display.
     */
    public Room[] getCleanedRooms() {
        ListInterface<Room> cleanedList = new CustomLinkedList<>();
        for (int i = 1; i <= roomList.size(); i++) {
            Room r = roomList.get(i);
            if (r != null && r.getStatus().toLowerCase().startsWith("cleaned")) {
                cleanedList.add(r);
            }
        }
        Object[] rawArray = cleanedList.toArray();
        Room[] result = new Room[rawArray.length];
        for (int i = 0; i < rawArray.length; i++) {
            result[i] = (Room) rawArray[i];
        }
        return result;
    }

    /**
     * Finds a room by room number using CustomLinkedList iteration.
     */
    public Room findRoom(String roomId) {
        for (int i = 1; i <= roomList.size(); i++) {
            Room r = roomList.get(i);
            if (r.getRoomNo().equalsIgnoreCase(roomId)) {
                return r;
            }
        }
        return null;
    }

    /**
     * Calculates standard target cleaning duration in minutes based on room type:
     * - Standard: 30 minutes
     * - Deluxe: 40 minutes
     * - Suite: 45 minutes
     */
    public int getStandardCleaningDurationMinutes(String roomType) {
        if (roomType == null)
            return 30;
        String type = roomType.trim().toLowerCase();
        if (type.contains("deluxe")) {
            return 40;
        } else if (type.contains("suite")) {
            return 45;
        } else {
            return 30; // Standard room
        }
    }

    public boolean assignCleaning(String roomId, String staffId) {
        Room room = findRoom(roomId);
        if (room == null) {
            System.out.println("Error: Room " + roomId + " not found.");
            return false;
        }
        if (!room.getStatus().equalsIgnoreCase("Dirty")) {
            System.out.println("Error: Room is not 'Dirty'. Cannot assign cleaning.");
            return false;
        }

        String currentStatus = room.getStatus();
        String nextStatus = "Cleaning In Progress";

        room.setStatus(nextStatus);
        saveRoomsToData();

        int targetMins = getStandardCleaningDurationMinutes(room.getRoomType());
        RoomTaskLog log = new RoomTaskLog(roomId, room.getRoomType(), currentStatus, nextStatus, staffId, targetMins);
        taskLogList.add(log); // Add to list (for reporting)
        undoStack.push(log); // Push to stack (for undo/rollback - LIFO)

        System.out.println("\n[√] CLEANING ASSIGNED SUCCESSFULLY:");
        System.out.println("    Room Number       : " + roomId + " (" + room.getRoomType() + ")");
        System.out.println("    Housekeeper ID    : " + staffId);
        System.out.println("    Current Status    : " + nextStatus);
        System.out.println("    Target Duration   : " + targetMins + " minutes");
        System.out.println("    Expected End Time : " + log.getFormattedTime(log.getExpectedEndTimeMillis()));
        return true;
    }

    /**
     * Calculates penalty fine for housekeeper when cleaning exceeds the target
     * duration.
     * Rate: RM 10.00 for every 15 minutes overdue.
     */
    public double calculatePenaltyFine(long actualMinutes, int targetMinutes) {
        if (actualMinutes <= targetMinutes) {
            return 0.0;
        }
        long overdueMinutes = actualMinutes - targetMinutes;
        long blocks = (long) Math.ceil(overdueMinutes / 15.0);
        return blocks * 10.0;
    }

    public boolean completeCleaning(String roomId, String staffId) {
        Room room = findRoom(roomId);
        if (room == null) {
            System.out.println("[!] Error: Room " + roomId + " not found.");
            return false;
        }
        if (!room.getStatus().equalsIgnoreCase("Cleaning In Progress")) {
            System.out.println("[!] Error: Room " + roomId + " is currently '" + room.getStatus()
                    + "'. Only 'Cleaning In Progress' rooms can be completed.");
            return false;
        }

        // Verify that the housekeeper completing the task is the SAME housekeeper who
        // started it
        RoomTaskLog activeLog = findActiveCleaningLog(roomId);
        if (activeLog != null && !activeLog.getStaffId().equalsIgnoreCase(staffId)) {
            System.out.println(
                    "[!] Error: Room " + roomId + " cleaning was started by Housekeeper " + activeLog.getStaffId()
                            + ". Only Housekeeper " + activeLog.getStaffId() + " can complete this cleaning task.");
            return false;
        }

        String roomStatus = "Cleaned (Waiting for Inspection)";
        String logStatus = "Cleaned";

        room.setStatus(roomStatus);
        saveRoomsToData();

        // Update the existing task log directly so Task ID remains the same
        if (activeLog != null) {
            activeLog.setNewStatus(logStatus);
            activeLog.setEndTime(System.currentTimeMillis());
        } else {
            int durationMins = getStandardCleaningDurationMinutes(room.getRoomType());
            RoomTaskLog log = new RoomTaskLog(roomId, room.getRoomType(), "Cleaning In Progress", logStatus, staffId,
                    durationMins);
            log.setEndTime(System.currentTimeMillis());
            taskLogList.add(log);
            undoStack.push(log);
            activeLog = log;
        }

        long actualMins = Math.max(0, (activeLog.getEndTime() - activeLog.getStartTime()) / (60 * 1000L));
        double fine = calculatePenaltyFine(actualMins, activeLog.getEstimatedDurationMinutes());
        activeLog.setPenaltyFine(fine);

        System.out.println("\n[√] CLEANING COMPLETED:");
        System.out.println("    Room Number       : " + roomId + " (" + room.getRoomType() + ")");
        System.out.println("    Housekeeper ID    : " + staffId);
        System.out.println("    New Room Status   : " + roomStatus);
        System.out.println("    Target Duration   : " + activeLog.getEstimatedDurationMinutes() + " minutes");
        System.out.println("    Actual Time Taken : " + actualMins + " minute(s)");
        if (fine > 0) {
            long overdueMins = actualMins - activeLog.getEstimatedDurationMinutes();
            System.out.printf("    [!] LATE PENALTY  : RM %.2f (Exceeded target duration by %d mins)%n", fine,
                    overdueMins);
        } else {
            System.out.println("    Performance       : COMPLETED ON TIME (RM 0.00 fine)");
        }
        return true;
    }

    private RoomTaskLog findActiveCleaningLog(String roomId) {
        for (int i = taskLogList.size(); i >= 1; i--) {
            RoomTaskLog log = taskLogList.get(i);
            if (log != null && log.getRoomId().equalsIgnoreCase(roomId)
                    && log.getNewStatus().equalsIgnoreCase("Cleaning In Progress")) {
                return log;
            }
        }
        return null;
    }

    private RoomTaskLog findLogByRoomAndStatus(String roomId, String status) {
        for (int i = taskLogList.size(); i >= 1; i--) {
            RoomTaskLog log = taskLogList.get(i);
            if (log != null && log.getRoomId().equalsIgnoreCase(roomId)
                    && log.getNewStatus().equalsIgnoreCase(status)) {
                return log;
            }
        }
        return null;
    }

    public boolean inspectRoom(String roomId, String supervisorId, boolean isClean) {
        Room room = findRoom(roomId);
        if (room == null) {
            System.out.println("[!] Error: Room " + roomId + " not found.");
            return false;
        }

        String currentRoomStatus = room.getStatus();
        if (!currentRoomStatus.toLowerCase().startsWith("cleaned")) {
            System.out.println("[!] Notice: Room " + roomId + " is currently '" + currentRoomStatus
                    + "'. Only cleaned rooms waiting for inspection can be inspected.");
            return false;
        }

        String roomNextStatus = isClean ? Room.STATUS_INSPECTED : "Dirty";
        String logStatus = isClean ? "Inspected" : "Inspected (Failed)";

        room.setStatus(roomNextStatus);
        saveRoomsToData();

        int targetMins = getStandardCleaningDurationMinutes(room.getRoomType());
        long now = System.currentTimeMillis();
        RoomTaskLog inspectionLog = new RoomTaskLog(roomId, room.getRoomType(), "Cleaned", logStatus, supervisorId,
                targetMins);
        inspectionLog.setStartTime(now - 5 * 60 * 1000L);
        inspectionLog.setEndTime(now);
        inspectionLog.setPenaltyFine(0.0);
        taskLogList.add(inspectionLog);
        undoStack.push(inspectionLog);

        if (isClean) {
            System.out.println("\n[√] Room " + roomId + " PASSED inspection by " + supervisorId
                    + ". Status updated to: " + roomNextStatus + ".");
        } else {
            System.out.println("\n[!] Room " + roomId + " FAILED inspection by " + supervisorId
                    + ". Status reverted to: " + roomNextStatus + ".");
        }

        return true;
    }

    /**
     * Requirement: Instantly Rollback the schedule (Undo).
     * Uses Stack ADT (LIFO — Last-In-First-Out) to pop the most recent action.
     * Also removes the corresponding entry from the task log list.
     */
    public boolean undoLastAction() {
        if (undoStack.isEmpty()) {
            System.out.println("\n============================================================");
            System.out.println("            HOUSEKEEPING ACTION ROLLBACK (UNDO)             ");
            System.out.println("============================================================");
            System.out.println(" [!] Notice: No task history entries available to roll back.");
            System.out.println("============================================================\n");
            return false;
        }

        // Pop from Stack ADT (LIFO — undo the LAST action)
        RoomTaskLog lastLog = undoStack.pop();

        // Also remove from the task log list (for report consistency)
        taskLogList.remove(lastLog);

        Room room = findRoom(lastLog.getRoomId());

        if (room != null) {
            room.setStatus(lastLog.getPreviousStatus());
            saveRoomsToData();

            System.out.println("\n============================================================");
            System.out.println("          HOUSEKEEPING ACTION ROLLBACK (UNDO) REPORT        ");
            System.out.println("============================================================");
            System.out.printf(" Room Number       : %s%n", room.getRoomNo());
            System.out.printf(" Room Type         : %s%n", room.getRoomType());
            System.out.println("------------------------------------------------------------");
            System.out.printf(" Staff ID          : %s%n", lastLog.getStaffId());
            System.out.printf(" Action Undone     : %s -> %s%n", lastLog.getNewStatus(), lastLog.getPreviousStatus());
            System.out.printf(" Task Timestamp    : %s%n", lastLog.getFormattedTime(lastLog.getStartTime()));
            System.out.println("------------------------------------------------------------");
            System.out.printf(" REVERTED STATUS   : %s%n", room.getStatus());
            System.out.println(" REMARK            : Task popped from Stack ADT (LIFO) & removed from List");
            System.out.println("============================================================\n");
            return true;
        } else {
            System.out.println("\n============================================================");
            System.out.println("          HOUSEKEEPING ACTION ROLLBACK (UNDO) REPORT        ");
            System.out.println("============================================================");
            System.out.println(" [!] Error: Target room (" + lastLog.getRoomId() + ") was not found.");
            System.out.println("============================================================\n");
            return false;
        }
    }

    /**
     * Requirement: Guest requests a late check-out during cleaning.
     * Instantly roll back status to Occupied / Dirty.
     */
    public boolean handleLateCheckoutRequest(String roomId, String staffId) {
        Room room = findRoom(roomId);
        if (room == null) {
            System.out.println("Error: Room " + roomId + " not found.");
            return false;
        }

        String currentStatus = room.getStatus();
        if (currentStatus.equalsIgnoreCase("Occupied")) {
            System.out.println("Notice: Room " + roomId + " is already Occupied.");
            return false;
        }

        String newStatus = "Occupied (Late Check-Out)";
        room.setStatus(newStatus);
        saveRoomsToData();

        int targetMins = getStandardCleaningDurationMinutes(room.getRoomType());
        RoomTaskLog log = new RoomTaskLog(roomId, room.getRoomType(), currentStatus, newStatus, staffId, targetMins);
        taskLogList.add(log);
        undoStack.push(log);

        System.out.println("\n[LATE CHECK-OUT ROLLBACK EXECUTED]");
        System.out.println("Guest late check-out approved for Room " + roomId + ".");
        System.out.println("Status instantly updated from '" + currentStatus + "' to '" + newStatus + "'.");
        return true;
    }

    public ListInterface<RoomTaskLog> getTaskLogList() {
        return taskLogList;
    }

    // =========================================================================
    // SECTION: SEARCHING & MULTI-CRITERIA FILTERING ALGORITHMS (DSA)
    // =========================================================================

    /**
     * Extracts all cleaning-related task logs (excluding supervisor inspections).
     */
    public RoomTaskLog[] getAllCleaningLogs() {
        ListInterface<RoomTaskLog> list = new CustomLinkedList<>();
        for (int i = 1; i <= taskLogList.size(); i++) {
            RoomTaskLog log = taskLogList.get(i);
            if (log != null && !log.isInspectionLog()) {
                list.add(log);
            }
        }
        Object[] raw = list.toArray();
        RoomTaskLog[] result = new RoomTaskLog[raw.length];
        for (int i = 0; i < raw.length; i++) {
            result[i] = (RoomTaskLog) raw[i];
        }
        return result;
    }

    /**
     * Extracts all supervisor inspection task logs.
     */
    public RoomTaskLog[] getAllInspectionLogs() {
        ListInterface<RoomTaskLog> list = new CustomLinkedList<>();
        for (int i = 1; i <= taskLogList.size(); i++) {
            RoomTaskLog log = taskLogList.get(i);
            if (log != null && log.isInspectionLog()) {
                list.add(log);
            }
        }
        Object[] raw = list.toArray();
        RoomTaskLog[] result = new RoomTaskLog[raw.length];
        for (int i = 0; i < raw.length; i++) {
            result[i] = (RoomTaskLog) raw[i];
        }
        return result;
    }

    /**
     * Multi-Criteria Filtering Algorithm for Staff Cleaning Performance.
     * Criteria:
     * 1. Staff ID: Matches exact staff ID (or "ALL" / null for any staff)
     * 2. Room Type: Matches room type (or "ALL" / null for any room type)
     * 3. Punctuality: "ALL", "ON_TIME", "OVERDUE", "IN_PROGRESS"
     * 4. Only Penalties: If true, only include records with penalty fine > 0
     */
    public RoomTaskLog[] filterStaffCleaningLogs(String staffId, String roomType, String punctuality,
            boolean onlyPenalties) {
        ListInterface<RoomTaskLog> filtered = new CustomLinkedList<>();
        RoomTaskLog[] allCleaning = getAllCleaningLogs();

        for (RoomTaskLog log : allCleaning) {
            // Criterion 1: Staff ID
            if (staffId != null && !staffId.equalsIgnoreCase("ALL") && !staffId.trim().isEmpty()) {
                if (!log.getStaffId().equalsIgnoreCase(staffId.trim())) {
                    continue;
                }
            }

            // Criterion 2: Room Type
            if (roomType != null && !roomType.equalsIgnoreCase("ALL") && !roomType.trim().isEmpty()) {
                if (!log.getRoomType().toLowerCase().contains(roomType.trim().toLowerCase())) {
                    continue;
                }
            }

            // Criterion 3: Punctuality
            if (punctuality != null && !punctuality.equalsIgnoreCase("ALL")) {
                if (punctuality.equalsIgnoreCase("ON_TIME")) {
                    if (log.getEndTime() == 0 || log.isOverdue()) {
                        continue;
                    }
                } else if (punctuality.equalsIgnoreCase("OVERDUE")) {
                    if (!log.isOverdue()) {
                        continue;
                    }
                } else if (punctuality.equalsIgnoreCase("IN_PROGRESS")) {
                    if (log.getEndTime() != 0) {
                        continue;
                    }
                }
            }

            // Criterion 4: Penalty Fines threshold
            if (onlyPenalties && log.getPenaltyFine() <= 0) {
                continue;
            }

            filtered.add(log);
        }

        Object[] raw = filtered.toArray();
        RoomTaskLog[] result = new RoomTaskLog[raw.length];
        for (int i = 0; i < raw.length; i++) {
            result[i] = (RoomTaskLog) raw[i];
        }
        return result;
    }

    /**
     * Multi-Criteria Filtering Algorithm for Room Hygiene & Inspection Quality.
     * Criteria:
     * 1. Supervisor ID: Matches supervisor ID (or "ALL" / null for any supervisor)
     * 2. Room Type: Matches room type (or "ALL" / null for any room type)
     * 3. Result: "ALL", "PASSED", "FAILED"
     */
    public RoomTaskLog[] filterInspectionLogs(String supervisorId, String roomType, String resultFilter) {
        ListInterface<RoomTaskLog> filtered = new CustomLinkedList<>();
        RoomTaskLog[] allInspections = getAllInspectionLogs();

        for (RoomTaskLog log : allInspections) {
            // Criterion 1: Supervisor ID
            if (supervisorId != null && !supervisorId.equalsIgnoreCase("ALL") && !supervisorId.trim().isEmpty()) {
                if (!log.getStaffId().equalsIgnoreCase(supervisorId.trim())) {
                    continue;
                }
            }

            // Criterion 2: Room Type
            if (roomType != null && !roomType.equalsIgnoreCase("ALL") && !roomType.trim().isEmpty()) {
                if (!log.getRoomType().toLowerCase().contains(roomType.trim().toLowerCase())) {
                    continue;
                }
            }

            // Criterion 3: Inspection Result
            if (resultFilter != null && !resultFilter.equalsIgnoreCase("ALL")) {
                if (resultFilter.equalsIgnoreCase("PASSED")) {
                    if (!log.isInspectionPassed()) {
                        continue;
                    }
                } else if (resultFilter.equalsIgnoreCase("FAILED")) {
                    if (!log.isInspectionFailed()) {
                        continue;
                    }
                }
            }

            filtered.add(log);
        }

        Object[] raw = filtered.toArray();
        RoomTaskLog[] result = new RoomTaskLog[raw.length];
        for (int i = 0; i < raw.length; i++) {
            result[i] = (RoomTaskLog) raw[i];
        }
        return result;
    }

    /**
     * Custom Binary Search on an array sorted ascendingly by Room Number.
     * Time Complexity: O(log n)
     * 
     * @param sortedByRoom Array of RoomTaskLog sorted ascendingly by room number
     * @param targetRoomNo Room number to search for
     * @return index of matching log, or -1 if not found
     */
    public int binarySearchByRoomNumber(RoomTaskLog[] sortedByRoom, String targetRoomNo) {
        if (sortedByRoom == null || sortedByRoom.length == 0 || targetRoomNo == null)
            return -1;
        int low = 0;
        int high = sortedByRoom.length - 1;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            int cmp = sortedByRoom[mid].getRoomId().compareToIgnoreCase(targetRoomNo.trim());
            if (cmp == 0) {
                return mid;
            } else if (cmp < 0) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return -1;
    }

    // =========================================================================
    // SECTION: CUSTOM SORTING ALGORITHMS (DSA)
    // =========================================================================

    /**
     * Custom QuickSort Algorithm: Sorts RoomTaskLog array by Penalty Fine.
     * Time Complexity: O(n log n) average
     */
    public void quickSortLogsByPenalty(RoomTaskLog[] array, int low, int high, boolean descending) {
        if (array == null || low >= high)
            return;

        int pivotIndex = partitionByPenalty(array, low, high, descending);
        quickSortLogsByPenalty(array, low, pivotIndex - 1, descending);
        quickSortLogsByPenalty(array, pivotIndex + 1, high, descending);
    }

    private int partitionByPenalty(RoomTaskLog[] array, int low, int high, boolean descending) {
        double pivot = array[high].getPenaltyFine();
        int i = low - 1;

        for (int j = low; j < high; j++) {
            boolean condition = descending ? (array[j].getPenaltyFine() >= pivot)
                    : (array[j].getPenaltyFine() <= pivot);
            if (condition) {
                i++;
                swap(array, i, j);
            }
        }
        swap(array, i + 1, high);
        return i + 1;
    }

    /**
     * Custom QuickSort Algorithm: Sorts RoomTaskLog array by Actual Cleaning
     * Duration (mins).
     * Time Complexity: O(n log n) average
     */
    public void quickSortLogsByDuration(RoomTaskLog[] array, int low, int high, boolean descending) {
        if (array == null || low >= high)
            return;

        int pivotIndex = partitionByDuration(array, low, high, descending);
        quickSortLogsByDuration(array, low, pivotIndex - 1, descending);
        quickSortLogsByDuration(array, pivotIndex + 1, high, descending);
    }

    private int partitionByDuration(RoomTaskLog[] array, int low, int high, boolean descending) {
        long pivot = array[high].getActualDurationMinutes();
        int i = low - 1;

        for (int j = low; j < high; j++) {
            boolean condition = descending ? (array[j].getActualDurationMinutes() >= pivot)
                    : (array[j].getActualDurationMinutes() <= pivot);
            if (condition) {
                i++;
                swap(array, i, j);
            }
        }
        swap(array, i + 1, high);
        return i + 1;
    }

    /**
     * Custom QuickSort Algorithm: Sorts RoomTaskLog array by Room Number
     * (Alphabetical/Numerical).
     * Time Complexity: O(n log n) average
     */
    public void quickSortLogsByRoomNo(RoomTaskLog[] array, int low, int high, boolean ascending) {
        if (array == null || low >= high)
            return;

        int pivotIndex = partitionByRoomNo(array, low, high, ascending);
        quickSortLogsByRoomNo(array, low, pivotIndex - 1, ascending);
        quickSortLogsByRoomNo(array, pivotIndex + 1, high, ascending);
    }

    private int partitionByRoomNo(RoomTaskLog[] array, int low, int high, boolean ascending) {
        String pivot = array[high].getRoomId();
        int i = low - 1;

        for (int j = low; j < high; j++) {
            int cmp = array[j].getRoomId().compareToIgnoreCase(pivot);
            boolean condition = ascending ? (cmp <= 0) : (cmp >= 0);
            if (condition) {
                i++;
                swap(array, i, j);
            }
        }
        swap(array, i + 1, high);
        return i + 1;
    }

    /**
     * Custom QuickSort Algorithm: Sorts RoomTaskLog array by Start Timestamp.
     * Time Complexity: O(n log n) average
     */
    public void quickSortLogsByStartTime(RoomTaskLog[] array, int low, int high, boolean ascending) {
        if (array == null || low >= high)
            return;

        int pivotIndex = partitionByStartTime(array, low, high, ascending);
        quickSortLogsByStartTime(array, low, pivotIndex - 1, ascending);
        quickSortLogsByStartTime(array, pivotIndex + 1, high, ascending);
    }

    private int partitionByStartTime(RoomTaskLog[] array, int low, int high, boolean ascending) {
        long pivot = array[high].getStartTime();
        int i = low - 1;

        for (int j = low; j < high; j++) {
            boolean condition = ascending ? (array[j].getStartTime() <= pivot) : (array[j].getStartTime() >= pivot);
            if (condition) {
                i++;
                swap(array, i, j);
            }
        }
        swap(array, i + 1, high);
        return i + 1;
    }

    /**
     * Custom InsertionSort Algorithm: Sorts RoomTaskLog array by Inspection Status
     * (Failed vs Passed).
     * Time Complexity: O(n^2) worst/average, O(n) best
     */
    public void insertionSortLogsByInspectionStatus(RoomTaskLog[] array, boolean failedFirst) {
        if (array == null || array.length <= 1)
            return;

        for (int i = 1; i < array.length; i++) {
            RoomTaskLog key = array[i];
            int j = i - 1;

            while (j >= 0 && shouldSwapInspection(array[j], key, failedFirst)) {
                array[j + 1] = array[j];
                j = j - 1;
            }
            array[j + 1] = key;
        }
    }

    private boolean shouldSwapInspection(RoomTaskLog current, RoomTaskLog key, boolean failedFirst) {
        int currentRank = current.isInspectionFailed() ? (failedFirst ? 1 : 2) : (failedFirst ? 2 : 1);
        int keyRank = key.isInspectionFailed() ? (failedFirst ? 1 : 2) : (failedFirst ? 2 : 1);
        return currentRank > keyRank;
    }

    /**
     * Custom InsertionSort Algorithm: Sorts RoomTaskLog array by Staff ID.
     */
    public void insertionSortLogsByStaffId(RoomTaskLog[] array, boolean ascending) {
        if (array == null || array.length <= 1)
            return;

        for (int i = 1; i < array.length; i++) {
            RoomTaskLog key = array[i];
            int j = i - 1;

            while (j >= 0) {
                int cmp = array[j].getStaffId().compareToIgnoreCase(key.getStaffId());
                boolean swapNeeded = ascending ? (cmp > 0) : (cmp < 0);
                if (!swapNeeded)
                    break;
                array[j + 1] = array[j];
                j--;
            }
            array[j + 1] = key;
        }
    }

    private void swap(RoomTaskLog[] array, int i, int j) {
        RoomTaskLog temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }

    // =========================================================================
    // SECTION: MANAGEMENT REPORT GENERATION METHODS (ANALYTICS & FORMATTING)
    // =========================================================================

    /**
     * REPORT 1: Housekeeping Staff Cleaning Efficiency & Penalty Audit Report.
     * Combines Multi-Criteria Filtering, Custom QuickSort/InsertionSort, and
     * Analytical KPIs.
     */
    public void displayStaffEfficiencyReport(String staffFilter, String roomTypeFilter, String punctualityFilter,
            boolean onlyPenalties, int sortChoice) {
        // 1. Multi-Criteria Filtering
        RoomTaskLog[] filtered = filterStaffCleaningLogs(staffFilter, roomTypeFilter, punctualityFilter, onlyPenalties);

        // 2. Custom Sorting
        String sortDesc;
        if (filtered.length > 1) {
            switch (sortChoice) {
                case 1 -> {
                    quickSortLogsByPenalty(filtered, 0, filtered.length - 1, true);
                    sortDesc = "Penalty Fine (Highest to Lowest) [QuickSort - O(n log n)]";
                }
                case 2 -> {
                    quickSortLogsByDuration(filtered, 0, filtered.length - 1, true);
                    sortDesc = "Actual Turnaround Time (Longest to Shortest) [QuickSort - O(n log n)]";
                }
                case 3 -> {
                    quickSortLogsByDuration(filtered, 0, filtered.length - 1, false);
                    sortDesc = "Actual Turnaround Time (Shortest to Longest) [QuickSort - O(n log n)]";
                }
                case 4 -> {
                    quickSortLogsByRoomNo(filtered, 0, filtered.length - 1, true);
                    sortDesc = "Room Number (Ascending) [QuickSort - O(n log n)]";
                }
                case 5 -> {
                    insertionSortLogsByStaffId(filtered, true);
                    sortDesc = "Staff ID (Ascending) [InsertionSort - O(n)]";
                }
                default -> {
                    quickSortLogsByStartTime(filtered, 0, filtered.length - 1, false);
                    sortDesc = "Task Timestamp (Most Recent First) [QuickSort - O(n log n)]";
                }
            }
        } else {
            sortDesc = "Default Sequence";
        }

        // 3. Render Executive Formatted Report
        String sep = "==========================================================================================================================================";
        String line = "+---------+----------+----------+---------------+-------------+-------------+-------------+--------------+------------------------------+";

        System.out.println("\n" + sep);
        System.out.println(
                "                                TARUMT RESORTS - HOUSEKEEPING PERFORMANCE & PENALTY AUDIT REPORT                                          ");
        System.out.println(
                "                                      Operational Analytical Summary for Hotel Management Review                                          ");
        System.out.println(sep);
        System.out.printf("  [Audit Cycle Metainfo] Generated At: %s | Report ID: HK-RPT-01%n",
                java.time.LocalDateTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.printf(
                "  [Filter Criteria]     Staff: %-8s | Room Type: %-12s | Punctuality: %-11s | Only Penalties: %b%n",
                (staffFilter == null || staffFilter.trim().isEmpty() ? "ALL" : staffFilter),
                (roomTypeFilter == null || roomTypeFilter.trim().isEmpty() ? "ALL" : roomTypeFilter),
                (punctualityFilter == null || punctualityFilter.trim().isEmpty() ? "ALL" : punctualityFilter),
                onlyPenalties);
        System.out.printf("  [Applied Algorithm]   Sorting Technique: %s%n", sortDesc);
        System.out.println(sep);

        if (filtered.length == 0) {
            System.out.println("  [!] No cleaning records matched the specified filter criteria.");
            System.out.println(sep + "\n");
            return;
        }

        // Table Header
        System.out.println(line);
        System.out.printf("| %-7s | %-8s | %-8s | %-13s | %-11s | %-11s | %-11s | %-12s | %-28s |\n",
                "Task ID", "Staff ID", "Room No.", "Room Type", "Target Time", "Actual Time", "Variance",
                "Penalty Fine", "Operational Status");
        System.out.println(line);

        // Metrics Accumulators
        int totalTasks = filtered.length;
        int onTimeCount = 0;
        int overdueCount = 0;
        int inProgressCount = 0;
        double totalPenalty = 0.0;
        long totalActualDuration = 0;
        int completedCount = 0;

        // Breakdown per room category
        int stdCount = 0, delCount = 0, suiCount = 0;
        long stdDuration = 0, delDuration = 0, suiDuration = 0;

        for (int i = 0; i < filtered.length; i++) {
            RoomTaskLog log = filtered[i];
            String taskId = String.format("HK%03d", i + 1);
            int targetMins = log.getEstimatedDurationMinutes();
            long actualMins = log.getActualDurationMinutes();
            totalPenalty += log.getPenaltyFine();

            String actualStr;
            String varianceStr;
            String statusStr;

            if (log.getEndTime() == 0) {
                inProgressCount++;
                actualStr = actualMins + "m (act)";
                varianceStr = (actualMins > targetMins) ? ("+" + (actualMins - targetMins) + "m Late") : "In Progress";
                statusStr = "CLEANING IN PROGRESS";
            } else {
                completedCount++;
                totalActualDuration += actualMins;
                actualStr = actualMins + " mins";
                long diff = actualMins - targetMins;
                if (diff <= 0) {
                    onTimeCount++;
                    varianceStr = (diff == 0) ? "0 mins" : (diff + " mins");
                    statusStr = "COMPLETED (ON-TIME)";
                } else {
                    overdueCount++;
                    varianceStr = "+" + diff + " mins";
                    statusStr = String.format("OVERDUE (PENALTY RM%.0f)", log.getPenaltyFine());
                }

                // Category aggregates
                String type = log.getRoomType().toLowerCase();
                if (type.contains("standard")) {
                    stdCount++;
                    stdDuration += actualMins;
                } else if (type.contains("deluxe")) {
                    delCount++;
                    delDuration += actualMins;
                } else if (type.contains("suite")) {
                    suiCount++;
                    suiDuration += actualMins;
                }
            }

            String fineFormatted = log.getPenaltyFine() > 0 ? String.format("RM %7.2f", log.getPenaltyFine())
                    : "RM    0.00";

            System.out.printf("| %-7s | %-8s | %-8s | %-13s | %-11s | %-11s | %-11s | %-12s | %-28s |\n",
                    taskId, log.getStaffId(), log.getRoomId(), log.getRoomType(),
                    targetMins + " mins", actualStr, varianceStr, fineFormatted, statusStr);
        }
        System.out.println(line);

        // 4. Operational KPIs and Analytical Summary
        double onTimeRate = completedCount > 0 ? ((double) onTimeCount / completedCount) * 100.0 : 0.0;
        double overdueRate = completedCount > 0 ? ((double) overdueCount / completedCount) * 100.0 : 0.0;
        double avgActualDuration = completedCount > 0 ? ((double) totalActualDuration / completedCount) : 0.0;

        System.out.println("\n" + sep);
        System.out.println(
                "                             EXECUTIVE PERFORMANCE METRICS & KPI DASHBOARD (BUSINESS CYCLE SUMMARY)                               ");
        System.out.println(sep);
        System.out.printf("  * Total Cleaning Tasks Evaluated  : %2d task(s)%n", totalTasks);
        System.out.printf("  * Completed Tasks / In-Progress   : %2d completed | %2d in-progress%n", completedCount,
                inProgressCount);
        System.out.printf("  * On-Time Turnaround Rate (Benchmark): %5.1f%% (%d/%d tasks)%n", onTimeRate, onTimeCount,
                completedCount);
        System.out.printf("  * Overdue / Delay Rate             : %5.1f%% (%d/%d tasks)%n", overdueRate, overdueCount,
                completedCount);
        System.out.printf("  * Average Cleaning Turnaround Time : %5.1f minutes%n", avgActualDuration);
        System.out.printf("  * Total Penalty Fines Incurred     : RM %.2f%n", totalPenalty);
        System.out.println(
                "--------------------------------------------------------------------------------------------------------------------------");
        System.out.println("  CATEGORY TURNAROUND BENCHMARK COMPARISON:");
        System.out.printf("   - Standard Rooms : Target: 30 mins | Actual Avg: %-5s (Sample: %d tasks)%n",
                stdCount > 0 ? String.format("%.1f mins", (double) stdDuration / stdCount) : "N/A", stdCount);
        System.out.printf("   - Deluxe Rooms   : Target: 40 mins | Actual Avg: %-5s (Sample: %d tasks)%n",
                delCount > 0 ? String.format("%.1f mins", (double) delDuration / delCount) : "N/A", delCount);
        System.out.printf("   - Suite Rooms    : Target: 45 mins | Actual Avg: %-5s (Sample: %d tasks)%n",
                suiCount > 0 ? String.format("%.1f mins", (double) suiDuration / suiCount) : "N/A", suiCount);
        System.out.println(
                "--------------------------------------------------------------------------------------------------------------------------");
        System.out.println("  ACTIONABLE MANAGEMENT RECOMMENDATIONS:");
        if (overdueRate > 25.0) {
            System.out.println("   [!] CAUTION: High overdue rate observed (" + String.format("%.1f%%", overdueRate)
                    + "). Recommend reallocating staff to Deluxe/Suite cleaning rosters.");
        } else {
            System.out.println(
                    "   [√] EXCELLENT: Housekeeping team met turnaround speed targets. Workforce punctuality is healthy.");
        }
        if (totalPenalty > 0) {
            System.out.printf(
                    "   [!] PENALTY AUDIT: RM %.2f in late fines deducted. Advise shift supervisors to monitor high-turnaround periods.%n",
                    totalPenalty);
        }
        System.out.println(sep + "\n");
    }

    /**
     * REPORT 2: Room Hygiene & Supervisor Quality Audit Report.
     * Combines Multi-Criteria Filtering, Custom InsertionSort/QuickSort, and
     * Quality Assurance Analytics.
     */
    public void displayRoomHygieneReport(String supervisorFilter, String roomTypeFilter, String resultFilter,
            int sortChoice) {
        // 1. Multi-Criteria Filtering
        RoomTaskLog[] filtered = filterInspectionLogs(supervisorFilter, roomTypeFilter, resultFilter);

        // 2. Custom Sorting
        String sortDesc;
        if (filtered.length > 1) {
            switch (sortChoice) {
                case 1 -> {
                    insertionSortLogsByInspectionStatus(filtered, true); // Failed inspections first
                    sortDesc = "Inspection Outcome (Failed / Rework First) [InsertionSort - O(n^2)]";
                }
                case 2 -> {
                    insertionSortLogsByInspectionStatus(filtered, false); // Passed inspections first
                    sortDesc = "Inspection Outcome (Approved / Pass First) [InsertionSort - O(n^2)]";
                }
                case 3 -> {
                    quickSortLogsByRoomNo(filtered, 0, filtered.length - 1, true);
                    sortDesc = "Room Number (Ascending) [QuickSort - O(n log n)]";
                }
                case 4 -> {
                    insertionSortLogsByStaffId(filtered, true);
                    sortDesc = "Supervisor ID (Ascending) [InsertionSort - O(n)]";
                }
                default -> {
                    quickSortLogsByStartTime(filtered, 0, filtered.length - 1, false);
                    sortDesc = "Inspection Timestamp (Most Recent First) [QuickSort - O(n log n)]";
                }
            }
        } else {
            sortDesc = "Default Sequence";
        }

        // 3. Render Executive Formatted Report
        String sep = "====================================================================================================================================================";
        String line = "+------------+-----------------+------------+--------------+-------------------------------+----------------------------+--------------------------+";

        System.out.println("\n" + sep);
        System.out.println(
                "                                          TARUMT RESORTS - ROOM HYGIENE & SUPERVISOR QUALITY AUDIT REPORT                                           ");
        System.out.println(
                "                                             Operational Quality Assurance (QA) Summary for Management                                              ");
        System.out.println(sep);
        System.out.printf("  [Audit Cycle Metainfo] Generated At: %s | Report ID: HK-RPT-02%n",
                java.time.LocalDateTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.printf("  [Filter Criteria]     Supervisor: %-8s | Room Type: %-12s | Result Filter: %-10s%n",
                (supervisorFilter == null || supervisorFilter.trim().isEmpty() ? "ALL" : supervisorFilter),
                (roomTypeFilter == null || roomTypeFilter.trim().isEmpty() ? "ALL" : roomTypeFilter),
                (resultFilter == null || resultFilter.trim().isEmpty() ? "ALL" : resultFilter));
        System.out.printf("  [Applied Algorithm]   Sorting Technique: %s%n", sortDesc);
        System.out.println(sep);

        if (filtered.length == 0) {
            System.out.println("  [!] No inspection records matched the specified filter criteria.");
            System.out.println(sep + "\n");
            return;
        }

        // Table Header
        System.out.println(line);
        System.out.printf("| %-10s | %-15s | %-10s | %-12s | %-29s | %-26s | %-24s |\n",
                "Audit ID", "Supervisor ID", "Room No.", "Room Type", "Inspection Outcome", "Re-Clean Required?",
                "Audit Timestamp");
        System.out.println(line);

        // Quality Metrics Accumulators
        int totalInspections = filtered.length;
        int passCount = 0;
        int failCount = 0;

        int stdTotal = 0, stdPass = 0;
        int delTotal = 0, delPass = 0;
        int suiTotal = 0, suiPass = 0;

        for (int i = 0; i < filtered.length; i++) {
            RoomTaskLog log = filtered[i];
            String auditId = String.format("QA%03d", i + 1);
            boolean isPass = log.isInspectionPassed();

            String outcomeStr;
            String reCleanStr;

            if (isPass) {
                passCount++;
                outcomeStr = "[PASS] Approved for Guest";
                reCleanStr = "No (Ready for Check-In)";
            } else {
                failCount++;
                outcomeStr = "[FAIL] Quality Defect Found";
                reCleanStr = "YES -> Reverted to Dirty";
            }

            // Room Type category analysis
            String type = log.getRoomType().toLowerCase();
            if (type.contains("standard")) {
                stdTotal++;
                if (isPass)
                    stdPass++;
            } else if (type.contains("deluxe")) {
                delTotal++;
                if (isPass)
                    delPass++;
            } else if (type.contains("suite")) {
                suiTotal++;
                if (isPass)
                    suiPass++;
            }

            System.out.printf("| %-10s | %-15s | %-10s | %-12s | %-29s | %-26s | %-24s |\n",
                    auditId, log.getStaffId(), log.getRoomId(), log.getRoomType(),
                    outcomeStr, reCleanStr, log.getFormattedTime(log.getStartTime()));
        }
        System.out.println(line);

        // 4. Quality Assurance KPIs & Decision Support
        double passRate = totalInspections > 0 ? ((double) passCount / totalInspections) * 100.0 : 0.0;
        double failRate = totalInspections > 0 ? ((double) failCount / totalInspections) * 100.0 : 0.0;

        System.out.println("\n" + sep);
        System.out.println(
                "                                          QUALITY ASSURANCE AUDIT METRICS & HYGIENE PERFORMANCE DASHBOARD                                   ");
        System.out.println(sep);
        System.out.printf("  * Total Room Inspections Conducted  : %2d inspection(s)%n", totalInspections);
        System.out.printf("  * First-Time Pass Rate (Hygiene QA) : %5.1f%% (%d/%d passed)%n", passRate, passCount,
                totalInspections);
        System.out.printf("  * Rework / Defect Rejection Rate    : %5.1f%% (%d/%d failed - sent for re-cleaning)%n",
                failRate, failCount, totalInspections);
        System.out.println(
                "----------------------------------------------------------------------------------------------------------------------------------------------------");
        System.out.println("  HYGIENE PASS RATE BY ROOM CATEGORY:");
        System.out.printf("   - Standard Rooms : %5.1f%% pass rate (%d/%d passed)%n",
                stdTotal > 0 ? ((double) stdPass / stdTotal) * 100.0 : 0.0, stdPass, stdTotal);
        System.out.printf("   - Deluxe Rooms   : %5.1f%% pass rate (%d/%d passed)%n",
                delTotal > 0 ? ((double) delPass / delTotal) * 100.0 : 0.0, delPass, delTotal);
        System.out.printf("   - Suite Rooms    : %5.1f%% pass rate (%d/%d passed)%n",
                suiTotal > 0 ? ((double) suiPass / suiTotal) * 100.0 : 0.0, suiPass, suiTotal);
        System.out.println(
                "----------------------------------------------------------------------------------------------------------------------------------------------------");
        System.out.println("  MANAGEMENT DECISION & QUALITY ASSURANCE DIRECTIVES:");
        if (failRate > 20.0) {
            System.out.println(
                    "   [!] QUALITY ALERT: Room rejection rate exceeded acceptable threshold (20%). Implement mandatory supervisor checklists.");
        } else {
            System.out.println(
                    "   [√] QUALITY COMPLIANCE: Room turnaround standards meet 5-star resort quality compliance targets.");
        }
        if (suiTotal > 0 && ((double) suiPass / suiTotal) < 0.80) {
            System.out.println(
                    "   [!] SPECIAL ATTENTION: Suite room turnovers require senior housekeeper assignment due to complex amenity requirements.");
        }
        System.out.println(sep + "\n");
    }

    /**
     * REPORT 3: Comprehensive Operational Cycle Executive Dashboard.
     * Combines both Cleaning Efficiency and Room Hygiene Quality for high-level
     * management review.
     */
    public void displayExecutiveDashboardSummary() {
        System.out.println(
                "\n==========================================================================================================================================");
        System.out.println(
                "                                TARUMT RESORTS - HOUSEKEEPING EXECUTIVE ALL-IN-ONE OPERATIONAL DASHBOARD                                  ");
        System.out.println(
                "==========================================================================================================================================");

        RoomTaskLog[] cleaningLogs = getAllCleaningLogs();
        RoomTaskLog[] inspectionLogs = getAllInspectionLogs();

        int totalCleaning = cleaningLogs.length;
        int onTimeCleaning = 0;
        int overdueCleaning = 0;
        double totalFines = 0.0;
        for (RoomTaskLog c : cleaningLogs) {
            totalFines += c.getPenaltyFine();
            if (c.getEndTime() > 0) {
                if (c.isOverdue())
                    overdueCleaning++;
                else
                    onTimeCleaning++;
            }
        }

        int totalInspections = inspectionLogs.length;
        int passInspections = 0;
        int failInspections = 0;
        for (RoomTaskLog ins : inspectionLogs) {
            if (ins.isInspectionPassed())
                passInspections++;
            else if (ins.isInspectionFailed())
                failInspections++;
        }

        int totalCompleted = onTimeCleaning + overdueCleaning;
        double onTimeRate = totalCompleted > 0 ? ((double) onTimeCleaning / totalCompleted) * 100.0 : 0.0;
        double passRate = totalInspections > 0 ? ((double) passInspections / totalInspections) * 100.0 : 0.0;

        System.out.println(" [1] WORKFORCE CLEANING THROUGHPUT & SPEED:");
        System.out.printf("     * Total Cleaning Tasks Logged     : %d task(s)%n", totalCleaning);
        System.out.printf("     * Punctuality On-Time Success Rate: %.1f%% (%d on-time / %d completed)%n", onTimeRate,
                onTimeCleaning, totalCompleted);
        System.out.printf("     * Overdue / Delayed Tasks         : %d task(s)%n", overdueCleaning);
        System.out.printf("     * Total Penalty Fines Accrued     : RM %.2f%n", totalFines);

        System.out.println("\n [2] ROOM HYGIENE & QUALITY CONTROL (QA):");
        System.out.printf("     * Total Inspections Conducted     : %d inspection(s)%n", totalInspections);
        System.out.printf("     * First-Time Approval Pass Rate   : %.1f%% (%d passed)%n", passRate, passInspections);
        System.out.printf("     * Defect / Re-Clean Rejections    : %d room(s)%n", failInspections);

        System.out.println("\n [3] INVENTORY & ROOM READINESS STATUS:");
        loadRoomsFromData();
        int dirtyCount = 0, cleanCount = 0, inspectedCount = 0, occupiedCount = 0;
        for (int i = 1; i <= roomList.size(); i++) {
            Room r = roomList.get(i);
            if (r != null) {
                String st = r.getStatus().toLowerCase();
                if (st.startsWith("dirty"))
                    dirtyCount++;
                else if (st.startsWith("cleaning") || st.startsWith("cleaned"))
                    cleanCount++;
                else if (st.startsWith("inspected") || st.startsWith("ready"))
                    inspectedCount++;
                else if (st.startsWith("occupied"))
                    occupiedCount++;
            }
        }
        System.out.printf("     * Total Managed Rooms : %d rooms%n", roomList.size());
        System.out.printf(
                "     * Ready for Check-In  : %d rooms | In Turnover/Cleaning: %d rooms | Dirty Awaiting: %d rooms | Occupied: %d rooms%n",
                inspectedCount, cleanCount, dirtyCount, occupiedCount);

        System.out.println(
                "==========================================================================================================================\n");
    }

    public void viewRawTaskLog() {
        String equalHeader = "===========================================================================================================================================================================";
        System.out.println("\n" + equalHeader);
        System.out.println(
                "                                                                           HOUSEKEEPING TASK LOG                                                                           ");
        System.out.println(equalHeader);
        if (taskLogList.isEmpty()) {
            System.out.println(" (No task history entries recorded yet)");
        } else {
            int totalCount = taskLogList.size();
            double totalFines = 0.0;
            String border = "+---------+----------+---------------+------------------------------------+----------+------------------------+------------------------+-------------+-------------+--------------+";

            System.out.println(border);
            System.out.printf("| %-7s | %-8s | %-13s | %-34s | %-8s | %-22s | %-22s | %-11s | %-11s | %-12s |\n",
                    "Task ID", "Room No.", "Room Type", "Status", "Staff ID", "Start Time", "End Time", "Target Mins",
                    "Total Time", "Penalty Fine");
            System.out.println(border);

            for (int i = totalCount; i >= 1; i--) {
                RoomTaskLog log = taskLogList.get(i);
                if (log != null) {
                    totalFines += log.getPenaltyFine();
                    String taskId = String.format("HK%03d", i);
                    String fineStr = log.getPenaltyFine() > 0 ? String.format("RM %.2f", log.getPenaltyFine())
                            : "RM 0.00";
                    String actualTimeStr;
                    if (log.getEndTime() == 0) {
                        actualTimeStr = "In Progress";
                    } else {
                        long mins = Math.max(0, (log.getEndTime() - log.getStartTime()) / (60 * 1000L));
                        actualTimeStr = mins + " mins";
                    }

                    System.out.printf(
                            "| %-7s | %-8s | %-13s | %-34s | %-8s | %-22s | %-22s | %-11s | %-11s | %-12s |\n",
                            taskId,
                            log.getRoomId(),
                            log.getRoomType(),
                            log.getNewStatus(),
                            log.getStaffId(),
                            log.getFormattedTime(log.getStartTime()),
                            log.getFormattedTime(log.getEndTime()),
                            log.getEstimatedDurationMinutes() + " mins",
                            actualTimeStr,
                            fineStr);
                }
            }

            System.out.println(border);
            System.out.printf(" Total Tasks Logged : %d | Total Late Penalty Fines Accumulated: RM %.2f%n", totalCount,
                    totalFines);
        }
        System.out.println(equalHeader + "\n");
    }

    public void displayOperationalReport() {
        displayStaffEfficiencyReport("ALL", "ALL", "ALL", false, 0);
    }
}
