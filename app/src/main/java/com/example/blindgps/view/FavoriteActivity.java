package com.example.blindgps.view;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.blindgps.R;
import com.example.blindgps.databinding.ActivityFavoriteBinding;
import com.example.blindgps.databinding.ActivityRecentLocationsBinding;
import com.example.blindgps.model.RecentLocations;
import com.example.blindgps.utils.Methods;
import com.example.blindgps.viewmodel.AppDatabase;
import com.example.blindgps.viewmodel.ExecuteQueryListener;
import com.example.blindgps.viewmodel.OnLocationItemClickListener;
import com.example.blindgps.viewmodel.RecentLocationsDAO;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

public class FavoriteActivity extends AppCompatActivity {

    private ActivityFavoriteBinding binding;
    private static RecentLocationAdapter locationAdapter;
    private AppDatabase appDatabase;
    private RecentLocationsDAO locationsDAO;
    private LinearLayoutManager manager;

    private static ArrayList<RecentLocations> locationFavoriteList, locationList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFavoriteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (!Methods.isNetworkAvailable(FavoriteActivity.this)){
            Toast.makeText(FavoriteActivity.this, "Please connect to the internet", Toast.LENGTH_SHORT).show();
        }

        appDatabase = AppDatabase.getInstance(this);
        locationsDAO = appDatabase.locationsDAO();
        manager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        locationFavoriteList = new ArrayList<RecentLocations>();
        locationList = new ArrayList<RecentLocations>();

        FavoriteActivity.Load_Data load_data = new FavoriteActivity.Load_Data();
        load_data.execute();
        LoadData();
    }

    private void LoadData() {
        binding.imvBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(FavoriteActivity.this, RecentLocationsActivity.class);
                startActivity(intent);
            }
        });

        binding.progressCircular.setVisibility(View.VISIBLE);

        int height = getResources().getDisplayMetrics().heightPixels;
        ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) Math.round(height * 0.1));
        layoutParams.setMargins(5, 5, 5, 5);
        locationAdapter = new RecentLocationAdapter(layoutParams, locationFavoriteList, new OnLocationItemClickListener() {
            @Override
            public void onEdit(int position, String name) {
                FavoriteActivity.Update_Data update_data = new FavoriteActivity.Update_Data(name, position, new ExecuteQueryListener() {
                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onEnd() {
                        locationFavoriteList.clear();
                        locationList.clear();
                        FavoriteActivity.Load_Data load_data = new FavoriteActivity.Load_Data();
                        load_data.execute();
                    }

                });
                update_data.execute();
            }

            @Override
            public void onClick(int position) {
                try {
                    Intent intent = new Intent(FavoriteActivity.this, MapsActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("time", "gone");
                    bundle.putString("from", "favorite");
                    bundle.putSerializable("location", locationFavoriteList.get(position));
                    intent.putExtras(bundle);
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onDelete(int position, boolean isOne) {
                FavoriteActivity.Delete_Data delete_data = new FavoriteActivity.Delete_Data(position, new ExecuteQueryListener() {
                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onEnd() {
                        FavoriteActivity.Load_Data load_data = new FavoriteActivity.Load_Data();
                        load_data.execute();
                    }
                });
                delete_data.execute();
            }

            @Override
            public void onEnd() {
                locationAdapter.notifyDataSetChanged();

            }
        }, FavoriteActivity.this);
        binding.recyclerView.setAdapter(locationAdapter);
        binding.recyclerView.setLayoutManager(manager);
    }

    class Load_Data extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            locationFavoriteList.clear();
            locationList.clear();
            if (locationsDAO.getAllLocations().size()>0){
                locationList.addAll(locationsDAO.getAllLocations());
                for (RecentLocations l : locationList){
                        if (l.getFav()){
                            locationFavoriteList.add(l);
                        }
                    }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
            binding.progressCircular.setVisibility(View.GONE);
            locationAdapter.notifyDataSetChanged();
        }
    }

    class Update_Data extends AsyncTask<Void, Void, Void>{
        String name_new;
        int position;
        ExecuteQueryListener listener;
        public Update_Data(String name_new, int position, ExecuteQueryListener listener) {
            this.name_new = name_new;
            this.position = position;
            this.listener = listener;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try{
                RecentLocations location = locationFavoriteList.get(position);
                double la_from = Double.parseDouble(location.getLatitude())-0.00005;
                double la_to = Double.parseDouble(location.getLatitude())+0.00005;
                double lo_from = Double.parseDouble(location.getLongitude())-0.00005;
                double lo_to = Double.parseDouble(location.getLongitude())+0.00005;


                for (int i=0; i< locationList.size(); i++) {
                    double la_d = Double.parseDouble(locationList.get(i).getLatitude());
                    double lo_d = Double.parseDouble(locationList.get(i).getLongitude());
                    if (la_from < la_d && la_to > la_d && lo_from < lo_d && lo_to > lo_d) {
                        locationList.get(i).setLocation_name(name_new);
                        locationsDAO.update(locationList.get(i));
                    }

                }
                return null;
            }
            catch(Exception e){
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
            listener.onEnd();
        }
    }

    class Delete_Data extends AsyncTask<Void, Void, Void> {
        int position;
        ExecuteQueryListener listener;
        public Delete_Data(int position,  ExecuteQueryListener listener) {
            this.position = position;
            this.listener = listener;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try{
                RecentLocations location = locationFavoriteList.get(position);
                double la_from = Double.parseDouble(location.getLatitude())-0.00005;
                double la_to = Double.parseDouble(location.getLatitude())+0.00005;
                double lo_from = Double.parseDouble(location.getLongitude())-0.00005;
                double lo_to = Double.parseDouble(location.getLongitude())+0.00005;


                for (int i=0; i< locationList.size(); i++) {
                    double la_d = Double.parseDouble(locationList.get(i).getLatitude());
                    double lo_d = Double.parseDouble(locationList.get(i).getLongitude());
                    if (la_from < la_d && la_to > la_d && lo_from < lo_d && lo_to > lo_d) {
                        locationList.get(i).setFav(false);
                        locationsDAO.update(locationList.get(i));
                    }

                }


                return null;
            }
            catch(Exception e){
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
            listener.onEnd();
        }
    }
}