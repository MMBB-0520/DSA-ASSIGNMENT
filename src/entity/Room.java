package entity;

public class Room {
    public static final String STATUS_AVAILABLE = "AVAILABLE";
    public static final String STATUS_BOOKED = "BOOKED";
    public static final String STATUS_DIRTY = "DIRTY";
    public static final String STATUS_CLEANING = "CLEANING";
    public static final String STATUS_INSPECTED = "INSPECTED";
    public static final String STATUS_LATE_30MINS = "LATE (30 MINS)";
    public static final String STATUS_LATE_2HRS = "LATE (2HRS)";
    public static final String STATUS_LATE_4HRS = "LATE (4HRS)";

    private String roomNo;
    private String roomType;
    private double pricePerNight;
    private String status;

    public Room(String roomNo, String roomType, double pricePerNight) {
        this(roomNo, roomType, pricePerNight, STATUS_AVAILABLE);
    }

    public Room(String roomNo, String roomType, double pricePerNight, String status) {
        this.roomNo = roomNo;
        this.roomType = roomType;
        this.pricePerNight = pricePerNight;
        this.status = status;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getPricePerNight() {
        return pricePerNight;
    }

    public void setPricePerNight(double pricePerNight) {
        this.pricePerNight = pricePerNight;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Room room = (Room) obj;
        return roomNo != null && roomNo.equalsIgnoreCase(room.roomNo);
    }

    @Override
    public String toString() {
        return String.format("Room[No=%s, Type=%s, Status=%s, Price=RM%.2f]", roomNo, roomType, status, pricePerNight);
    }
}
