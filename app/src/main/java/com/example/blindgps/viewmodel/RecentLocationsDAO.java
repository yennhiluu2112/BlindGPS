package com.example.blindgps.viewmodel;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import com.example.blindgps.model.RecentLocations;

@Dao
public interface RecentLocationsDAO {
    @Insert
    public void insert(RecentLocations... items);
    @Update
    public void update(RecentLocations... items);
    @Delete
    public void delete(RecentLocations... items);

    @Query("UPDATE recent_locations SET location_name = :name WHERE latitude = :latitude AND longitude = :longitude")
    public void updateName(String latitude, String longitude, String name);

    @Query("UPDATE recent_locations SET isFav = :isFav WHERE id = :id")
    public void setFav(int id, boolean isFav);

    @Query("SELECT * FROM recent_locations")
    public List<RecentLocations> getAllLocations();

    @Query("DELETE FROM recent_locations")
    public void deleteAll();
}
