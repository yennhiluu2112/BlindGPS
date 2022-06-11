package com.example.blindgps.listeners;

import com.example.blindgps.activities.RecentLocationsActivity;

public interface OnLocationItemClickListener {
    public void onEdit(int position, String name);
    public void onClick(int position);
    public void onDelete(int position, boolean isOne);
    public void onEnd();


}
