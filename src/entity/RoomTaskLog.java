package entity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class RoomTaskLog {
    private String roomId;
    private String previousStatus;
    private String newStatus;
    private String staffId;
    private long startTime;
    private long endTime;

    public RoomTaskLog(String roomId, String previousStatus, String newStatus, String staffId) {
        this.roomId = roomId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.staffId = staffId;
        this.startTime = System.currentTimeMillis();
        this.endTime = 0;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }
    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }
    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }

    public String getStaffId() {
        return staffId;
    }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    
    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    public String getFormattedTime(long timeMillis) {
        if (timeMillis == 0) return "In Progress";
        LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(timeMillis), ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a");
        return dt.format(formatter);
    }

    @Override
    public String toString() {
        return String.format("[Start: %s | End: %s] Room: %-5s | %-20s -> %-20s | Staff: %s", 
                getFormattedTime(startTime), getFormattedTime(endTime), roomId, previousStatus, newStatus, staffId);
    }
}
