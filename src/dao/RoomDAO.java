package dao;

import entity.Room;

/**
 * Data Access Object (DAO) Interface for Room entity persistence.
 */
public interface RoomDAO {
    Room[] getAllRooms();
    void saveAllRooms(Room[] rooms);
    Room getRoomByNo(String roomNo);
    void saveRoom(Room room);
}
