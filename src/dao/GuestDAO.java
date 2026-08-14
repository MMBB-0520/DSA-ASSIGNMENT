package dao;

import entity.Guest;

/**
 * Data Access Object (DAO) Interface for Guest entity persistence.
 */
public interface GuestDAO {
    Guest[] getAllGuests();
    void saveAllGuests(Guest[] guests);
    void saveGuest(Guest guest);
}
