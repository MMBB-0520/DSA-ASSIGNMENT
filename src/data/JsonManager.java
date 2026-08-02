package data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import entity.Booking;
import entity.Guest;
import entity.Room;
import control.LocalDateTimeAdapter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

public class JsonManager {

    private static final String ROOMS_FILE_PATH = "src/data/rooms.json";
    private static final String BOOKINGS_FILE_PATH = "src/data/bookings.json";
    private static final String GUESTS_FILE_PATH = "src/data/guests.json";

    public static Room[] loadRooms() {
        try {
            Gson gson = new Gson();
            File file = new File(ROOMS_FILE_PATH);
            if (!file.exists()) {
                return new Room[0];
            }
            FileReader reader = new FileReader(file);
            Room[] rooms = gson.fromJson(reader, Room[].class);
            reader.close();
            return (rooms != null) ? rooms : new Room[0];
        } catch (IOException e) {
            System.out.println("Cannot load rooms.json");
            return new Room[0];
        }
    }

    public static void saveRooms(Room[] rooms) {
        try {
            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .create();
            FileWriter writer = new FileWriter(ROOMS_FILE_PATH);
            gson.toJson(rooms, writer);
            writer.close();
        } catch (IOException e) {
            System.out.println("Cannot save rooms.json");
        }
    }

    public static Booking[] loadBookings() {
        try {
            File file = new File(BOOKINGS_FILE_PATH);
            if (!file.exists()) {
                return new Booking[0];
            }
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                    .create();
            FileReader reader = new FileReader(file);
            Booking[] bookings = gson.fromJson(reader, Booking[].class);
            reader.close();
            return (bookings != null) ? bookings : new Booking[0];
        } catch (IOException e) {
            System.out.println("Cannot load bookings.json: " + e.getMessage());
            return new Booking[0];
        }
    }

    public static void saveBookings(Booking[] bookings) {
        try {
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                    .setPrettyPrinting()
                    .create();
            FileWriter writer = new FileWriter(BOOKINGS_FILE_PATH);
            gson.toJson(bookings, writer);
            writer.close();
        } catch (IOException e) {
            System.out.println("Cannot save bookings.json: " + e.getMessage());
        }
    }

    public static Guest[] loadGuests() {
        try {
            File file = new File(GUESTS_FILE_PATH);
            if (!file.exists()) {
                return new Guest[0];
            }
            Gson gson = new Gson();
            FileReader reader = new FileReader(file);
            Guest[] guests = gson.fromJson(reader, Guest[].class);
            reader.close();
            return (guests != null) ? guests : new Guest[0];
        } catch (IOException e) {
            System.out.println("Cannot load guests.json: " + e.getMessage());
            return new Guest[0];
        }
    }

    public static void saveGuests(Guest[] guests) {
        try {
            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .create();
            FileWriter writer = new FileWriter(GUESTS_FILE_PATH);
            gson.toJson(guests, writer);
            writer.close();
        } catch (IOException e) {
            System.out.println("Cannot save guests.json: " + e.getMessage());
        }
    }
}