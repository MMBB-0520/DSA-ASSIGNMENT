package control;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import entity.Booking;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class DataRepository {

    private static final String BOOKINGS_FILE = "bookings.json";
    private static final String ROOMS_FILE = "rooms.json";
    private static final String STAFF_FILE = "staff.json";
    private Gson gson;

    public DataRepository() {
        this.gson = new GsonBuilder()
                .registerTypeAdapter(java.time.LocalDateTime.class, new LocalDateTimeAdapter())
                .setPrettyPrinting()
                .create();
    }

    public void saveBookings(Booking[] queueBookings, Booking[] processedBookings) {
        // Combine them to save
        List<Booking> allBookings = new ArrayList<>();
        if (queueBookings != null) {
            for (Booking b : queueBookings) allBookings.add(b);
        }
        if (processedBookings != null) {
            for (Booking b : processedBookings) allBookings.add(b);
        }

        try (FileWriter writer = new FileWriter(BOOKINGS_FILE)) {
            gson.toJson(allBookings, writer);
        } catch (IOException e) {
            System.err.println("Error saving bookings to JSON: " + e.getMessage());
        }
    }

    public Booking[] loadBookings() {
        try (FileReader reader = new FileReader(BOOKINGS_FILE)) {
            Type listType = new TypeToken<ArrayList<Booking>>(){}.getType();
            List<Booking> bookings = gson.fromJson(reader, listType);
            if (bookings != null) {
                return bookings.toArray(new Booking[0]);
            }
        } catch (IOException e) {
            // File might not exist yet, which is fine
        }
        return new Booking[0];
    }

    public void saveRooms(entity.Room[] rooms) {
        try (FileWriter writer = new FileWriter(ROOMS_FILE)) {
            gson.toJson(rooms, writer);
        } catch (IOException e) {
            System.err.println("Error saving rooms to JSON: " + e.getMessage());
        }
    }

    public entity.Room[] loadRooms() {
        try (FileReader reader = new FileReader(ROOMS_FILE)) {
            Type listType = new TypeToken<ArrayList<entity.Room>>(){}.getType();
            List<entity.Room> rooms = gson.fromJson(reader, listType);
            if (rooms != null && !rooms.isEmpty()) {
                return rooms.toArray(new entity.Room[0]);
            }
        } catch (IOException e) {
            // Ignore
        }
        return null;
    }

    public void saveStaff(entity.Staff[] staff) {
        try (FileWriter writer = new FileWriter(STAFF_FILE)) {
            gson.toJson(staff, writer);
        } catch (IOException e) {
            System.err.println("Error saving staff to JSON: " + e.getMessage());
        }
    }

    public entity.Staff[] loadStaff() {
        try (FileReader reader = new FileReader(STAFF_FILE)) {
            Type listType = new TypeToken<ArrayList<entity.Staff>>(){}.getType();
            List<entity.Staff> staff = gson.fromJson(reader, listType);
            if (staff != null && !staff.isEmpty()) {
                return staff.toArray(new entity.Staff[0]);
            }
        } catch (IOException e) {
            // Ignore
        }
        return null;
    }
}
