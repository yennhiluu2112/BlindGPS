package com.example.blindgps.listener;

public interface OnLocationItemClickListener {
    public void onEdit(int position, String name);
    public void onClick(int position);
    public void onDelete(int position);
    public void onEnd();
}
