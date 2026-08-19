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
            RoomTaskLog log105 = new RoomTaskLog("105", "Dirty", "Cleaning In Progress", "S001");
            log105.setStartTime(System.currentTimeMillis());
            taskLogList.add(log105);
            undoStack.push(log105);
        }
    }

    /**
     * Loads rooms from RoomDAO (same data source as Walk-In Registration & Front Desk modules).
     * Uses CustomLinkedList (Linear ADT) instead of java.util.ArrayList.
     */
    private void loadRoomsFromData() {
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
        
        RoomTaskLog log = new RoomTaskLog(roomId, currentStatus, nextStatus, staffId);
        taskLogList.add(log);        // Add to list (for reporting)
        undoStack.push(log);         // Push to stack (for undo/rollback - LIFO)
        
        System.out.println("Success: Assigned Room " + roomId + " to Housekeeper " + staffId + ". Status: " + nextStatus);
        return true;
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
            RoomTaskLog log = new RoomTaskLog(roomId, "Cleaning In Progress", logStatus, staffId);
            log.setEndTime(System.currentTimeMillis());
            taskLogList.add(log);
            undoStack.push(log);
        }

        System.out.println("Success: Room " + roomId + " cleaning completed by Housekeeper " + staffId + ". Status: " + roomStatus);
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

        // Update the existing task log directly so Task ID remains the same
        RoomTaskLog activeLog = findLogByRoomAndStatus(roomId, "Cleaned");
        if (activeLog != null) {
            activeLog.setNewStatus(logStatus);
        } else {
            RoomTaskLog log = new RoomTaskLog(roomId, "Cleaned", logStatus, supervisorId);
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
        System.out.printf(" Room Type         : %s%n", room.getRoomType());
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

        RoomTaskLog log = new RoomTaskLog(roomId, currentStatus, newStatus, staffId);
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
        System.out.println("\n===========================================================================================================================");
        System.out.println("                                      HOUSEKEEPING TASK HISTORY REPORT                                                     ");
        System.out.println("===========================================================================================================================");
        if (taskLogList.isEmpty()) {
            System.out.println(" (No task history entries recorded yet)");
        } else {
            int totalCount = taskLogList.size();
            String border = "+---------+----------+--------------------------+----------+-----------------------+-----------------------+";
            
            System.out.println(border);
            System.out.printf("| %-7s | %-8s | %-24s | %-8s | %-21s | %-21s |\n", 
                    "Task ID", "Room No.", "Status", "Staff ID", "Start Time", "End Time");
            System.out.println(border);

            for (int i = totalCount; i >= 1; i--) {
                RoomTaskLog log = taskLogList.get(i);
                if (log != null) {
                    String taskId = String.format("HK%03d", i);
                    System.out.printf("| %-7s | %-8s | %-24s | %-8s | %-21s | %-21s |\n", 
                            taskId, 
                            log.getRoomId(), 
                            log.getNewStatus(), 
                            log.getStaffId(), 
                            log.getFormattedTime(log.getStartTime()),
                            log.getFormattedTime(log.getEndTime()));
                }
            }

            System.out.println(border);
            System.out.println(" Total tasks: " + totalCount);
        }
        System.out.println("===========================================================================================================================\n");
    }
}
