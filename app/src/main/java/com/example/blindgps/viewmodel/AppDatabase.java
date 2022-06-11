package com.example.blindgps.viewmodel;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.blindgps.model.RecentLocations;

@Database(entities = {RecentLocations.class}, version = 3,  exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract RecentLocationsDAO locationsDAO();

    private static AppDatabase instance;


    public static AppDatabase getInstance(Context context){
        if (instance==null){
            instance = Room.databaseBuilder(context, AppDatabase.class, "recent_locations").fallbackToDestructiveMigration().build();
        }
        return instance;
    }

}
