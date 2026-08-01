package data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import entity.Room;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class JsonManager {

    public static Room[] loadRooms(){

        try {

            Gson gson = new Gson();
            FileReader reader = new FileReader("src/data/rooms.json");

            Room[] rooms = gson.fromJson(reader, Room[].class);
            reader.close();

            return rooms;

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
            FileWriter writer = new FileWriter("src/data/rooms.json");

            gson.toJson(rooms, writer);
            writer.close();

        } catch (IOException e) {
            System.out.println("Cannot save rooms.json");
        }
    }
}