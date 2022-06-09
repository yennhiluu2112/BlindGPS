package com.example.blindgps.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

@Entity(tableName = "recent_locations")
public class RecentLocations implements Serializable {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String longitude;
    private String latitude;
    private String location_name;
    private String time;
    private Boolean isFav;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public Boolean getFav() {
        return isFav;
    }

    public void setFav(Boolean fav) {
        isFav = fav;
    }

    public String getLocation_name() {
        return location_name;
    }

    public void setLocation_name(String location_name) {
        this.location_name = location_name;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public RecentLocations(String longitude, String latitude, String location_name, String time, Boolean isFav) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.location_name = location_name;
        this.time = time;
        this.isFav = isFav;
    }


}
