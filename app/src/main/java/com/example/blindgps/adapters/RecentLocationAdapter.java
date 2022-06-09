package com.example.blindgps.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.chauthai.swipereveallayout.SwipeRevealLayout;
import com.example.blindgps.R;
import com.example.blindgps.listeners.OnLocationItemClickListener;
import com.example.blindgps.model.RecentLocations;
import com.example.blindgps.viewmodel.AppDatabase;
import com.example.blindgps.viewmodel.RecentLocationsDAO;

import java.util.ArrayList;

public class RecentLocationAdapter extends RecyclerView.Adapter<RecentLocationAdapter.ViewHolder> {
    private static ArrayList<RecentLocations> locations;
    private OnLocationItemClickListener listener;
    private ConstraintLayout.LayoutParams params;
    private Context context;
    private AppDatabase appDatabase;
    private RecentLocationsDAO locationsDAO;

    public RecentLocationAdapter(ConstraintLayout.LayoutParams params, ArrayList<RecentLocations> locations, OnLocationItemClickListener listener, Context context) {
        this.locations = locations;
        this.listener = listener;
        this.params = params;
        this.context = context;
        appDatabase = AppDatabase.getInstance(context);
        locationsDAO =appDatabase.locationsDAO();
    }

    public void loadListLocation(ArrayList<RecentLocations>  list_location){
        this.locations = list_location;
    }

    @NonNull
    @Override
    public RecentLocationAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_location, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return locations.size();
    }

    @Override
    public void onBindViewHolder(@NonNull RecentLocationAdapter.ViewHolder holder, int position) {
        try{
            holder.tv_name.setEnabled(false);
            holder.constraint1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onClick(holder.getAdapterPosition());
                }
            });

            Edit_Location(holder.iv_edit, position);

            Delete_Location(holder.tv_delete, position);

            if (locations.get(position).getFav()){
                holder.iv_fav.setImageResource(R.drawable.ic_fav);
            }
            else{
                holder.iv_fav.setImageResource(R.drawable.ic_no_fav);
            }

            holder.tv_name.setText(locations.get(position).getLocation_name());
            holder.tv_time.setText(locations.get(position).getTime());
            holder.swipeRevealLayout.setLayoutParams(params);

            holder.iv_fav.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    boolean isFav;
                    if (locations.get(holder.getAdapterPosition()).getFav())
                    {
                        isFav = false;
                        holder.iv_fav.setImageResource(R.drawable.ic_no_fav);
                    }
                    else{
                        isFav = true;
                        holder.iv_fav.setImageResource(R.drawable.ic_fav);
                    }

                    listener.onFav(holder.getAdapterPosition(), isFav);
                }
            });

        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private void Delete_Location(View v_delete, int position){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view1 = LayoutInflater.from(context).inflate(R.layout.dialog_delete, null,false);
        builder.setView(view1);
        builder.setCancelable(false);

        final AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        Button btn_one = view1.findViewById(R.id.btn_one);
        Button btn_all = view1.findViewById(R.id.btn_all);
        TextView tv_cancel = view1.findViewById(R.id.tv_cancel);

        tv_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        v_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.show();
            }
        });

        btn_one.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try{
                    listener.onDelete(position, true);
                    alertDialog.dismiss();
                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }
        });

        btn_all.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onDelete(position, false);
                alertDialog.dismiss();
            }
        });
    }

    private void Edit_Location(View v_edit, int position){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view1 = LayoutInflater.from(context).inflate(R.layout.dialog_change_name, null,false);
        builder.setView(view1);
        builder.setCancelable(false);

        final AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        Button btn_ok = view1.findViewById(R.id.btn_ok);
        Button btn_cancel = view1.findViewById(R.id.btn_back);
        EditText edt_name = view1.findViewById(R.id.edt_name);

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        v_edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.show();
            }
        });

        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try{
                    String name = edt_name.getText().toString();
                    if(name.length()!=0){
                        listener.onEdit(position, name);
                        alertDialog.dismiss();
                    }
                    else{
                        Toast.makeText(context, "Vui lòng nhập lại", Toast.LENGTH_SHORT).show();
                    }

                }
                catch(Exception e){
                    e.printStackTrace();
                    Toast.makeText(context, "Vui lòng nhập lại", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        SwipeRevealLayout swipeRevealLayout;
        ConstraintLayout layout, constraint1;
        TextView tv_time, tv_delete;
        EditText tv_name;
        ImageView iv_edit, iv_fav;

        public ViewHolder(@NonNull View itemView) {

            super(itemView);
            layout  = itemView.findViewById(R.id.layout_item);
            constraint1 = itemView.findViewById(R.id.constraint1);
            tv_name = itemView.findViewById(R.id.tv_name);
            tv_time = itemView.findViewById(R.id.tv_time);
            iv_edit = itemView.findViewById(R.id.iv_edit);
            swipeRevealLayout = itemView.findViewById(R.id.swipe_layout);
            tv_delete = itemView.findViewById(R.id.tv_delete);
            iv_fav = itemView.findViewById(R.id.iv_Fav);

        }
    }


}
