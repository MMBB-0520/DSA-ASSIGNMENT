package dao.impl;

import dao.GuestDAO;
import data.JsonManager;
import entity.Guest;

/**
 * Concrete JSON implementation of GuestDAO.
 */
public class GuestDAOImpl implements GuestDAO {

    @Override
    public Guest[] getAllGuests() {
        return JsonManager.loadGuests();
    }

    @Override
    public void saveAllGuests(Guest[] guests) {
        JsonManager.saveGuests(guests);
    }

    @Override
    public void saveGuest(Guest guest) {
        if (guest == null || guest.getGuestId() == null)
            return;
        Guest[] existing = getAllGuests();
        boolean found = false;
        for (int i = 0; i < existing.length; i++) {
            if (existing[i] != null && guest.getGuestId().equalsIgnoreCase(existing[i].getGuestId())) {
                existing[i] = guest;
                found = true;
                break;
            }
        }

        if (found) {
            saveAllGuests(existing);
        } else {
            Guest[] updated = new Guest[existing.length + 1];
            System.arraycopy(existing, 0, updated, 0, existing.length);
            updated[existing.length] = guest;
            saveAllGuests(updated);
        }
    }
}
