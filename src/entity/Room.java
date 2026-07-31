package entity;
import java.time.LocalDateTime;

public class Room {
    public static final String STATUS_AVAILABLE = "AVAILABLE";
    public static final String STATUS_OCCUPIED = "OCCUPIED";
    public static final String STATUS_DIRTY = "DIRTY";

    private String roomId;
    private String number;
    private String type;
    private double price;
    private String status;
    private LocalDateTime availableFrom;

    public Room(String roomId, String number, String type, double price, String status) {
        this.roomId = roomId;
        this.number = number;
        this.type = type;
        this.price = price;
        this.status = status;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        if (STATUS_AVAILABLE.equals(status)) {
            this.availableFrom = null;
        }
    }

    public LocalDateTime getAvailableFrom() {
        return availableFrom;
    }

    public void setAvailableFrom(LocalDateTime availableFrom) {
        this.availableFrom = availableFrom;
    }

    @Override
    public String toString() {
        String availStr = (availableFrom != null) ? " availableFrom=" + availableFrom. toString().replace("T", " ") : "";
        return "Room{id=" + roomId + ", number=" + number + ", type=" + type + ", price=RM" + price + ", status="
                + status + availStr + "}";
    }
}
