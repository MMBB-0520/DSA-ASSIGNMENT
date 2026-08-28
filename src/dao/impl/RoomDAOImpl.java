package dao.impl;

import dao.RoomDAO;
import data.JsonManager;
import entity.Room;

/**
 * Concrete JSON implementation of RoomDAO.
 */
public class RoomDAOImpl implements RoomDAO {

    @Override
    public Room[] getAllRooms() {
        return JsonManager.loadRooms();
    }

    @Override
    public void saveAllRooms(Room[] rooms) {
        JsonManager.saveRooms(rooms);
    }

    @Override
    public Room getRoomByNo(String roomNo) {
        if (roomNo == null)
            return null;
        Room[] rooms = getAllRooms();
        for (Room r : rooms) {
            if (r != null && roomNo.equalsIgnoreCase(r.getRoomNo())) {
                return r;
            }
        }
        return null;
    }

    @Override
    public void saveRoom(Room room) {
        if (room == null || room.getRoomNo() == null) return;
        Room[] rooms = getAllRooms();
        for (int i = 0; i < rooms.length; i++) {
            if (rooms[i] != null && rooms[i].getRoomNo().equalsIgnoreCase(room.getRoomNo())) {
                rooms[i] = room;
                break;
            }
        }
        saveAllRooms(rooms);
    }
}