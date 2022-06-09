package com.example.blindgps.listeners;

public interface OnLocationItemClickListener {
    public void onEdit(int position, String name);
    public void onClick(int position);
    public void onDelete(int position, boolean isOne);
    public void onFav(int position, boolean isFav);
    public void onEnd();
}
