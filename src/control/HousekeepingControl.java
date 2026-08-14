package control;

import adt.CustomLinkedList;
import adt.ListInterface;
import data.JsonManager;
import entity.Room;
import entity.RoomTaskLog;

import java.util.ArrayList;
import java.util.List;

public class HousekeepingControl {
    private ListInterface<RoomTaskLog> taskLogList;
    private List<Room> roomList;

    public HousekeepingControl() {
        this.taskLogList = new CustomLinkedList<>();
        this.roomList = new ArrayList<>();
        loadRoomsFromData();
        initSampleTaskLogs();
    }

    private void initSampleTaskLogs() {
        if (taskLogList.isEmpty()) {
            RoomTaskLog log105 = new RoomTaskLog("105", "Dirty", "Cleaning In Progress", "S001");
            log105.setStartTime(System.currentTimeMillis());
            taskLogList.add(log105);
        }
    }

    private void loadRoomsFromData() {
        Room[] loaded = JsonManager.loadRooms();
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

    public void saveRoomsToData() {
        JsonManager.saveRooms(roomList.toArray(new Room[0]));
    }

    public List<Room> getAllRooms() {
        return roomList;
    }

    public Room findRoom(String roomId) {
        for (Room r : roomList) {
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
        taskLogList.add(log);
        
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

        String nextStatus = "Cleaned";
        room.setStatus(nextStatus);
        saveRoomsToData();

        // Update the existing task log directly so Task ID remains the same
        if (activeLog != null) {
            activeLog.setNewStatus(nextStatus);
            activeLog.setEndTime(System.currentTimeMillis());
        } else {
            RoomTaskLog log = new RoomTaskLog(roomId, "Cleaning In Progress", nextStatus, staffId);
            log.setEndTime(System.currentTimeMillis());
            taskLogList.add(log);
        }

        System.out.println("Success: Room " + roomId + " cleaning completed by Housekeeper " + staffId + ". Status: " + nextStatus);
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

        if (!room.getStatus().equalsIgnoreCase("Cleaned")) {
            System.out.println("[!] Error: Room is not in 'Cleaned' state.");
            return false;
        }

        String nextStatus = isClean ? Room.STATUS_INSPECTED : "Dirty";
        room.setStatus(nextStatus);
        saveRoomsToData();

        // Update the existing task log directly so Task ID remains the same
        RoomTaskLog activeLog = findLogByRoomAndStatus(roomId, "Cleaned");
        if (activeLog != null) {
            activeLog.setNewStatus(nextStatus);
        } else {
            RoomTaskLog log = new RoomTaskLog(roomId, "Cleaned", nextStatus, supervisorId);
            log.setEndTime(System.currentTimeMillis());
            taskLogList.add(log);
        }

        if (isClean) {
            System.out.println("Success: Room " + roomId + " inspected and approved by Supervisor " + supervisorId + ". Status: " + nextStatus);
        } else {
            System.out.println("Notice: Room " + roomId + " failed inspection by Supervisor " + supervisorId + ". Status reverted to: " + nextStatus + " (Housekeeper must redo).");
        }

        return true;
    }

    /**
     * Requirement: Instantly Rollback the schedule (Undo using List removeLast / remove).
     */
    public boolean undoLastAction() {
        if (taskLogList.isEmpty()) {
            System.out.println("Notice: No actions in the task log stack to roll back.");
            return false;
        }

        RoomTaskLog lastLog = taskLogList.removeLast();
        Room room = findRoom(lastLog.getRoomId());

        if (room != null) {
            room.setStatus(lastLog.getPreviousStatus());
            saveRoomsToData();
            System.out.println("\n[ROLLBACK SUCCESSFUL]");
            System.out.println("Rolled back log entry:");
            System.out.println("  " + lastLog);
            System.out.println("  Room " + room.getRoomNo() + " status reverted to: '" + room.getStatus() + "'");
            return true;
        } else {
            System.out.println("Error: Target room for rollback (" + lastLog.getRoomId() + ") was not found.");
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

        System.out.println("\n[LATE CHECK-OUT ROLLBACK EXECUTED]");
        System.out.println("Guest late check-out approved for Room " + roomId + ".");
        System.out.println("Status instantly updated from '" + currentStatus + "' to '" + newStatus + "'.");
        return true;
    }

    public ListInterface<RoomTaskLog> getTaskLogList() {
        return taskLogList;
    }

    public void displayOperationalReport() {
        System.out.println("\n=========================================================================================");
        System.out.println("                         HOUSEKEEPING TASK HISTORY REPORT                                ");
        System.out.println("=========================================================================================");
        if (taskLogList.isEmpty()) {
            System.out.println(" (No task history entries recorded yet)");
        } else {
            int totalCount = taskLogList.size();
            String border = "+---------+----------+--------------------------+----------+------------+------------+";
            
            System.out.println(border);
            System.out.printf("| %-7s | %-8s | %-24s | %-8s | %-10s | %-10s |\n", 
                    "Task ID", "Room No.", "Status", "Staff ID", "Start Time", "End Time");
            System.out.println(border);

            for (int i = totalCount; i >= 1; i--) {
                RoomTaskLog log = taskLogList.get(i);
                if (log != null) {
                    String taskId = String.format("HK%03d", i);
                    System.out.printf("| %-7s | %-8s | %-24s | %-8s | %-10s | %-10s |\n", 
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
        System.out.println("=========================================================================================\n");
    }
}
