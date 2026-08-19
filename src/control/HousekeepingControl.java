package control;

import adt.ArrayStack;
import adt.CustomLinkedList;
import adt.ListInterface;
import adt.StackInterface;
import dao.RoomDAO;
import dao.impl.RoomDAOImpl;
import entity.Room;
import entity.RoomTaskLog;

public class HousekeepingControl {
    private ListInterface<RoomTaskLog> taskLogList;   // Linear ADT: task history for reporting
    private StackInterface<RoomTaskLog> undoStack;     // Stack ADT: LIFO for rollback/undo
    private ListInterface<Room> roomList;              // Linear ADT: room list (custom, NOT java.util)
    private RoomDAO roomDAO;                           // DAO: shared data layer with other modules

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

            // Scenario 1: Standard Room - Completed on time & Approved by Supervisor (Pass)
            RoomTaskLog log1 = new RoomTaskLog("101", "Standard", "Cleaned", "Inspected", "SP001", 30);
            log1.setStartTime(now - 2 * hour);
            log1.setEndTime(now - 2 * hour + 25 * min);
            log1.setPenaltyFine(0.0);
            taskLogList.add(log1);
            undoStack.push(log1);

            // Scenario 2: Deluxe Room - Exceeded Target Duration (Late Penalty Fine Charged)
            RoomTaskLog log2 = new RoomTaskLog("203", "Deluxe", "Cleaning In Progress", "Cleaned", "S002", 40);
            log2.setStartTime(now - 90 * min);
            log2.setEndTime(now - 25 * min); // Took 65 mins (25 mins overdue -> RM 20.00 fine)
            log2.setPenaltyFine(20.0);
            taskLogList.add(log2);
            undoStack.push(log2);

            // Scenario 3: Suite Room - Failed Supervisor Inspection (Rejected & Reverted to Dirty)
            RoomTaskLog log3 = new RoomTaskLog("301", "Suite", "Cleaned", "Inspected (Failed)", "SP002", 45);
            log3.setStartTime(now - 50 * min);
            log3.setEndTime(now - 45 * min);
            log3.setPenaltyFine(0.0);
            taskLogList.add(log3);
            undoStack.push(log3);

            // Scenario 4: Suite Room - Cleaning Currently In Progress
            RoomTaskLog log4 = new RoomTaskLog("302", "Suite", "Dirty", "Cleaning In Progress", "S003", 45);
            log4.setStartTime(now - 15 * min);
            log4.setEndTime(0); // 0 = In Progress
            log4.setPenaltyFine(0.0);
            taskLogList.add(log4);
            undoStack.push(log4);

            // Scenario 5: Standard Room - Inspected & Approved
            RoomTaskLog log5 = new RoomTaskLog("105", "Standard", "Cleaned", "Inspected", "SP001", 30);
            log5.setStartTime(now - 10 * min);
            log5.setEndTime(now - 10 * min);
            log5.setPenaltyFine(0.0);
            taskLogList.add(log5);
            undoStack.push(log5);
        }
    }

    /**
     * Loads rooms from RoomDAO (same data source as Walk-In Registration & Front Desk modules).
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
     * Filters and returns rooms with status 'Dirty' (checked-out rooms ready for cleaning)
     * using CustomLinkedList (Linear ADT).
     */
    public Room[] getDirtyRooms() {
        loadRoomsFromData(); // Always refresh latest room status from RoomDAO / rooms.json
        ListInterface<Room> dirtyList = new CustomLinkedList<>();
        for (int i = 1; i <= roomList.size(); i++) {
            Room r = roomList.get(i);
            if (r != null && r.getStatus().equalsIgnoreCase("Dirty")) {
                dirtyList.add(r);
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
     * Saves rooms via RoomDAO (shared data layer — changes are visible to all modules).
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
     * Other modules can iterate via for-each (CustomLinkedList implements Iterable).
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
        if (roomType == null) return 30;
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
        taskLogList.add(log);        // Add to list (for reporting)
        undoStack.push(log);         // Push to stack (for undo/rollback - LIFO)
        
        System.out.println("\n[√] CLEANING ASSIGNED SUCCESSFULLY:");
        System.out.println("    Room Number       : " + roomId + " (" + room.getRoomType() + ")");
        System.out.println("    Housekeeper ID    : " + staffId);
        System.out.println("    Current Status    : " + nextStatus);
        System.out.println("    Target Duration   : " + targetMins + " minutes");
        System.out.println("    Expected End Time : " + log.getFormattedTime(log.getExpectedEndTimeMillis()));
        return true;
    }

    /**
     * Calculates penalty fine for housekeeper when cleaning exceeds the target duration.
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
            System.out.println("[!] Error: Room " + roomId + " is currently '" + room.getStatus() + "'. Only 'Cleaning In Progress' rooms can be completed.");
            return false;
        }

        // Verify that the housekeeper completing the task is the SAME housekeeper who started it
        RoomTaskLog activeLog = findActiveCleaningLog(roomId);
        if (activeLog != null && !activeLog.getStaffId().equalsIgnoreCase(staffId)) {
            System.out.println("[!] Error: Room " + roomId + " cleaning was started by Housekeeper " + activeLog.getStaffId() 
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
            RoomTaskLog log = new RoomTaskLog(roomId, room.getRoomType(), "Cleaning In Progress", logStatus, staffId, durationMins);
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
            System.out.printf("    [!] LATE PENALTY  : RM %.2f (Exceeded target duration by %d mins)%n", fine, overdueMins);
        } else {
            System.out.println("    Performance       : COMPLETED ON TIME (RM 0.00 fine)");
        }
        return true;
    }

    private RoomTaskLog findActiveCleaningLog(String roomId) {
        for (int i = taskLogList.size(); i >= 1; i--) {
            RoomTaskLog log = taskLogList.get(i);
            if (log != null && log.getRoomId().equalsIgnoreCase(roomId) && log.getNewStatus().equalsIgnoreCase("Cleaning In Progress")) {
                return log;
            }
        }
        return null;
    }

    private RoomTaskLog findLogByRoomAndStatus(String roomId, String status) {
        for (int i = taskLogList.size(); i >= 1; i--) {
            RoomTaskLog log = taskLogList.get(i);
            if (log != null && log.getRoomId().equalsIgnoreCase(roomId) && log.getNewStatus().equalsIgnoreCase(status)) {
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
            System.out.println("[!] Notice: Room " + roomId + " is currently '" + currentRoomStatus + "'. Only cleaned rooms waiting for inspection can be inspected.");
            return false;
        }

        String roomNextStatus = isClean ? Room.STATUS_INSPECTED : "Dirty";
        String logStatus = isClean ? "Inspected" : "Inspected (Failed)";

        room.setStatus(roomNextStatus);
        saveRoomsToData();

        int targetMins = getStandardCleaningDurationMinutes(room.getRoomType());
        // Update the existing task log directly so Task ID remains the same
        RoomTaskLog activeLog = findLogByRoomAndStatus(roomId, "Cleaned");
        if (activeLog != null) {
            activeLog.setNewStatus(logStatus);
        } else {
            RoomTaskLog log = new RoomTaskLog(roomId, room.getRoomType(), "Cleaned", logStatus, supervisorId, targetMins);
            log.setEndTime(System.currentTimeMillis());
            taskLogList.add(log);
            undoStack.push(log);
        }

        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String timeStr = java.time.LocalDateTime.now().format(dtf);

        System.out.println("\n============================================================");
        System.out.println("            SUPERVISOR ROOM INSPECTION REPORT               ");
        System.out.println("============================================================");
        System.out.printf(" Room Number       : %s%n", room.getRoomNo());
        System.out.printf(" Room Type         : %s (Standard Cleaning: %d mins)%n", room.getRoomType(), targetMins);
        System.out.printf(" Room Rate         : RM %.2f / night%n", room.getPricePerNight());
        System.out.println("------------------------------------------------------------");
        System.out.printf(" Supervisor ID     : %s%n", supervisorId);
        System.out.printf(" Inspection Time   : %s%n", timeStr);
        System.out.printf(" Inspection Result : %s%n", isClean ? "APPROVED (PASS)" : "REJECTED (FAILED)");
        System.out.println("------------------------------------------------------------");
        System.out.printf(" NEW ROOM STATUS   : %s%n", roomNextStatus);
        if (isClean) {
            System.out.println(" REMARK            : Room is clean & ready for Guest Check-In.");
        } else {
            System.out.println(" REMARK            : Room failed inspection. Reverted to Dirty.");
        }
        System.out.println("============================================================\n");

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

    public void displayOperationalReport() {
        String equalHeader = "===========================================================================================================================================================================";
        System.out.println("\n" + equalHeader);
        System.out.println("                                                                   HOUSEKEEPING TASK HISTORY REPORT                                                                        ");
        System.out.println(equalHeader);
        if (taskLogList.isEmpty()) {
            System.out.println(" (No task history entries recorded yet)");
        } else {
            int totalCount = taskLogList.size();
            double totalFines = 0.0;
            String border = "+---------+----------+---------------+--------------------------+----------+------------------------+------------------------+-------------+-------------+--------------+";
            
            System.out.println(border);
            System.out.printf("| %-7s | %-8s | %-13s | %-24s | %-8s | %-22s | %-22s | %-11s | %-11s | %-12s |\n", 
                    "Task ID", "Room No.", "Room Type", "Status", "Staff ID", "Start Time", "End Time", "Target Mins", "Total Time", "Penalty Fine");
            System.out.println(border);

            for (int i = totalCount; i >= 1; i--) {
                RoomTaskLog log = taskLogList.get(i);
                if (log != null) {
                    totalFines += log.getPenaltyFine();
                    String taskId = String.format("HK%03d", i);
                    String fineStr = log.getPenaltyFine() > 0 ? String.format("RM %.2f", log.getPenaltyFine()) : "RM 0.00";
                    String actualTimeStr;
                    if (log.getEndTime() == 0) {
                        actualTimeStr = "In Progress";
                    } else {
                        long mins = Math.max(0, (log.getEndTime() - log.getStartTime()) / (60 * 1000L));
                        actualTimeStr = mins + " mins";
                    }

                    System.out.printf("| %-7s | %-8s | %-13s | %-24s | %-8s | %-22s | %-22s | %-11s | %-11s | %-12s |\n", 
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
            System.out.printf(" Total Tasks Logged : %d | Total Late Penalty Fines Accumulated: RM %.2f%n", totalCount, totalFines);
        }
        System.out.println(equalHeader + "\n");
    }
}
