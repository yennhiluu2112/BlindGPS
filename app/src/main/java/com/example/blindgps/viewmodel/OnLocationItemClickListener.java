package com.example.blindgps.viewmodel;

public interface OnLocationItemClickListener {
    public void onEdit(int position, String name);
    public void onClick(int position);
    public void onDelete(int position, boolean isOne);
    public void onEnd();


}
