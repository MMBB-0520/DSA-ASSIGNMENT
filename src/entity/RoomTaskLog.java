package entity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class RoomTaskLog {
    private String roomId;
    private String roomType;
    private String previousStatus;
    private String newStatus;
    private String staffId;
    private long startTime;
    private long endTime;
    private int estimatedDurationMinutes;
    private double penaltyFine;

    public RoomTaskLog(String roomId, String previousStatus, String newStatus, String staffId) {
        this(roomId, "Standard", previousStatus, newStatus, staffId, 30);
    }

    public RoomTaskLog(String roomId, String roomType, String previousStatus, String newStatus, String staffId,
            int estimatedDurationMinutes) {
        this.roomId = roomId;
        this.roomType = roomType != null ? roomType : "Standard";
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.staffId = staffId;
        this.startTime = System.currentTimeMillis();
        this.endTime = 0;
        this.estimatedDurationMinutes = estimatedDurationMinutes;
        this.penaltyFine = 0.0;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
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

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public int getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public void setEstimatedDurationMinutes(int estimatedDurationMinutes) {
        this.estimatedDurationMinutes = estimatedDurationMinutes;
    }

    public double getPenaltyFine() {
        return penaltyFine;
    }

    public void setPenaltyFine(double penaltyFine) {
        this.penaltyFine = penaltyFine;
    }

    public long getExpectedEndTimeMillis() {
        if (startTime == 0)
            return 0;
        return startTime + (estimatedDurationMinutes * 60 * 1000L);
    }

    public String getFormattedTime(long timeMillis) {
        if (timeMillis == 0)
            return "In Progress";
        LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(timeMillis), ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a");
        return dt.format(formatter);
    }

    public long getActualDurationMinutes() {
        if (startTime == 0)
            return 0;
        long end = (endTime == 0) ? System.currentTimeMillis() : endTime;
        return Math.max(0, (end - startTime) / (60 * 1000L));
    }

    public boolean isOverdue() {
        if (endTime == 0) {
            return getActualDurationMinutes() > estimatedDurationMinutes;
        }
        return getActualDurationMinutes() > estimatedDurationMinutes || penaltyFine > 0;
    }

    public boolean isInspectionLog() {
        if (newStatus == null)
            return false;
        String s = newStatus.toLowerCase();
        return s.contains("inspected");
    }

    public boolean isInspectionPassed() {
        if (newStatus == null)
            return false;
        return newStatus.equalsIgnoreCase("Inspected") || newStatus.equalsIgnoreCase("Ready for Check-In");
    }

    public boolean isInspectionFailed() {
        if (newStatus == null)
            return false;
        return newStatus.toLowerCase().contains("failed") || newStatus.equalsIgnoreCase("dirty");
    }

    @Override
    public String toString() {
        return String.format("[Start: %s | End: %s | Est: %d mins] Room: %-5s (%s) | %-20s -> %-20s | Staff: %s",
                getFormattedTime(startTime), getFormattedTime(endTime), estimatedDurationMinutes, roomId, roomType,
                previousStatus, newStatus, staffId);
    }
}
