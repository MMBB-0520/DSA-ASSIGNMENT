package control;

import adt.ArrayStack;
import adt.StackInterface;
import data.JsonManager;
import entity.Room;
import entity.RoomTaskLog;

import java.util.ArrayList;
import java.util.List;

public class HousekeepingControl {
    private StackInterface<RoomTaskLog> taskLogStack;
    private List<Room> roomList;

    public HousekeepingControl() {
        this.taskLogStack = new ArrayStack<>();
        this.roomList = new ArrayList<>();
        loadRoomsFromData();
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
        taskLogStack.push(log);
        
        System.out.println("Success: Assigned Room " + roomId + " to Housekeeper " + staffId + ". Status: " + nextStatus);
        return true;
    }

    public boolean completeCleaning(String roomId, String staffId) {
        Room room = findRoom(roomId);
        if (room == null) {
            System.out.println("Error: Room " + roomId + " not found.");
            return false;
        }
        if (!room.getStatus().equalsIgnoreCase("Cleaning In Progress")) {
            System.out.println("Error: Room is not in 'Cleaning In Progress' state.");
            return false;
        }

        String currentStatus = room.getStatus();
        String nextStatus = "Cleaned";

        room.setStatus(nextStatus);
        saveRoomsToData();

        // If the top log is the assignment log, update its end time
        if (!taskLogStack.isEmpty()) {
            RoomTaskLog topLog = taskLogStack.peek();
            if (topLog.getRoomId().equals(roomId) && topLog.getNewStatus().equals("Cleaning In Progress")) {
                topLog.setEndTime(System.currentTimeMillis());
            }
        }
        
        RoomTaskLog log = new RoomTaskLog(roomId, currentStatus, nextStatus, staffId);
        log.setEndTime(System.currentTimeMillis());
        taskLogStack.push(log);

        System.out.println("Success: Room " + roomId + " cleaning completed by Housekeeper " + staffId + ". Status: " + nextStatus);
        return true;
    }

    public boolean inspectRoom(String roomId, String supervisorId, boolean isClean) {
        Room room = findRoom(roomId);
        if (room == null) {
            System.out.println("Error: Room " + roomId + " not found.");
            return false;
        }

        if (!room.getStatus().equalsIgnoreCase("Cleaned")) {
            System.out.println("Error: Room is not in 'Cleaned' state.");
            return false;
        }

        String currentStatus = room.getStatus();
        String nextStatus = isClean ? Room.STATUS_AVAILABLE : "Dirty";

        room.setStatus(nextStatus);
        saveRoomsToData();

        RoomTaskLog log = new RoomTaskLog(roomId, currentStatus, nextStatus, supervisorId);
        log.setEndTime(System.currentTimeMillis());
        taskLogStack.push(log);

        if (isClean) {
            System.out.println("Success: Room " + roomId + " inspected and approved by Supervisor " + supervisorId + ". Status: " + nextStatus);
        } else {
            System.out.println("Notice: Room " + roomId + " failed inspection by Supervisor " + supervisorId + ". Status reverted to: " + nextStatus + " (Housekeeper must redo).");
        }

        return true;
    }

    /**
     * Requirement: Instantly Rollback the schedule (Undo using Stack pop).
     */
    public boolean undoLastAction() {
        if (taskLogStack.isEmpty()) {
            System.out.println("Notice: No actions in the task log stack to roll back.");
            return false;
        }

        RoomTaskLog lastLog = taskLogStack.pop();
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
        taskLogStack.push(log);

        System.out.println("\n[LATE CHECK-OUT ROLLBACK EXECUTED]");
        System.out.println("Guest late check-out approved for Room " + roomId + ".");
        System.out.println("Status instantly updated from '" + currentStatus + "' to '" + newStatus + "'.");
        return true;
    }

    public StackInterface<RoomTaskLog> getTaskLogStack() {
        return taskLogStack;
    }

    public void displayOperationalReport() {
        System.out.println("\n=========== HOUSEKEEPING TASK HISTORY ===========");
        if (taskLogStack.isEmpty()) {
            System.out.println("(No task history entries recorded yet)");
        } else {
            System.out.printf("%-8s %-10s %-22s %-22s %-10s %-12s %-12s\n", 
                    "Task ID", "Room No.", "Previous Status", "New Status", "Staff ID", "Start Time", "End Time");
            System.out.println("---------------------------------------------------------------------------------------------------------");

            StackInterface<RoomTaskLog> tempStack = new ArrayStack<>();
            int count = taskLogStack.size();
            while (!taskLogStack.isEmpty()) {
                RoomTaskLog log = taskLogStack.pop();
                String taskId = String.format("HK%03d", count--);
                System.out.printf("%-8s %-10s %-22s %-22s %-10s %-12s %-12s\n", 
                        taskId, 
                        log.getRoomId(), 
                        log.getPreviousStatus(), 
                        log.getNewStatus(), 
                        log.getStaffId(), 
                        log.getFormattedTime(log.getStartTime()),
                        log.getFormattedTime(log.getEndTime()));
                tempStack.push(log);
            }

            while (!tempStack.isEmpty()) {
                taskLogStack.push(tempStack.pop());
            }
            System.out.println("---------------------------------------------------------------------------------------------------------");
            System.out.println("Total tasks: " + tempStack.size());
        }
        System.out.println("=========================================================================================================\n");
    }
}


