package com.example.blindgps.view;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.DatePicker;
import android.widget.SearchView;
import android.widget.Toast;

import com.example.blindgps.R;
import com.example.blindgps.databinding.ActivityMapsBinding;
import com.example.blindgps.databinding.ActivityRecentLocationsBinding;
import com.example.blindgps.listener.ExecuteQueryListener;
import com.example.blindgps.listener.OnLocationItemClickListener;
import com.example.blindgps.model.RecentLocations;
import com.example.blindgps.viewmodel.AppDatabase;
import com.example.blindgps.viewmodel.RecentLocationsDAO;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RecentLocationsActivity extends AppCompatActivity {
    private ActivityRecentLocationsBinding binding;
    private static RecentLocationAdapter locationAdapter;
    private AppDatabase appDatabase;
    private RecentLocationsDAO locationsDAO;
    private LinearLayoutManager manager;
    private static ArrayList<RecentLocations> locationList;
    private static String date_from, date_to;
    private static Boolean isRotate=false, isDatePicker=false;
    private ArrayList<RecentLocations> locationListByDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRecentLocationsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        appDatabase = AppDatabase.getInstance(this);
        locationsDAO = appDatabase.locationsDAO();
        manager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        locationList = new ArrayList<RecentLocations>();
        locationListByDate = new ArrayList<RecentLocations>();
        LoadData();
    }

    private void LoadData(){
        binding.imvBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isDatePicker = false;
                Intent intent = new Intent(RecentLocationsActivity.this, MapsActivity.class);
                startActivity(intent);
            }
        });
        binding.progressCircular.setVisibility(View.VISIBLE);
        RecentLocationsActivity.Load_Data load_data = new RecentLocationsActivity.Load_Data();
        load_data.execute();
        int height = getResources().getDisplayMetrics().heightPixels;
        ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int)Math.round(height*0.1));
        layoutParams.setMargins(5,5,5,5);
        locationAdapter = new RecentLocationAdapter(layoutParams, locationList, new OnLocationItemClickListener() {
            @Override
            public void onEdit(int position, String name) {
                RecentLocationsActivity.Update_Data update_data = new RecentLocationsActivity.Update_Data(name, position, locationList.get(position).getLatitude(), locationList.get(position).getLongitude(), new ExecuteQueryListener() {
                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onEnd() {

                        RecentLocationsActivity.Load_Data load_data = new RecentLocationsActivity.Load_Data();
                        load_data.execute();
                    }

                });
                update_data.execute();
            }

            @Override
            public void onClick(int position) {
                try{
                    Date date = new SimpleDateFormat("dd/MM/yyyy HH:mm").parse(locationList.get(position).getTime());
                    String time = getPastTimeString(date);
                    Intent intent = new Intent(RecentLocationsActivity.this, MapsActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("from", "one");
                    bundle.putString("time", time);
                    bundle.putSerializable("location", locationList.get(position));
                    intent.putExtras(bundle);
                    startActivity(intent);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }

            @Override
            public void onDelete(int position) {
                RecentLocationsActivity.Delete_Data delete_data = new RecentLocationsActivity.Delete_Data(position, new ExecuteQueryListener() {
                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onEnd() {
                        RecentLocationsActivity.Load_Data load_data = new RecentLocationsActivity.Load_Data();
                        load_data.execute();
                    }
                });
                delete_data.execute();

            }

            @Override
            public void onEnd() {
                locationAdapter.notifyDataSetChanged();

            }
        }, RecentLocationsActivity.this);
        binding.recyclerView.setAdapter(locationAdapter);
        binding.recyclerView.setLayoutManager(manager);

        FindLocation();
        ChooseDate();

        binding.ivRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.progressCircular.setVisibility(View.VISIBLE);
                binding.tvDate1.setText("From");
                binding.tvDate2.setText("To");
                date_from ="";
                date_to="";
                isRotate=true;
                if (isRotate){
                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            binding.ivRefresh.animate().rotationBy(360).withEndAction(this).setDuration(700).setInterpolator(new LinearInterpolator()).start();
                        }
                    };
                    binding.ivRefresh.animate().rotationBy(360).withEndAction(runnable).setDuration(700).setInterpolator(new LinearInterpolator()).start();

                }
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        binding.ivRefresh.animate().cancel();
                        binding.progressCircular.setVisibility(View.GONE);
                        locationAdapter.loadListLocation(locationList);
                        locationAdapter.notifyDataSetChanged();

                    }
                },400);

            }
        });

    }

    private void FindLocation(){
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String find) {
                ArrayList<RecentLocations> list_search = new ArrayList<RecentLocations>();
                try{
                    for (RecentLocations l : locationList) {
                        if (l.getLocation_name().toLowerCase().contains(find)) {
                            list_search.add(l);
                        }
                    }
                    if(list_search.isEmpty()) {
                        if (find.length() > 0){}
                        //Toast.makeText(RecentLocationsActivity.this, "No Location Found", Toast.LENGTH_SHORT).show();
                    } else {
                        locationAdapter.loadListLocation(list_search);
                        locationAdapter.notifyDataSetChanged();
                    }
                    if(find.length() == 0){
                        locationAdapter.loadListLocation(locationList);
                        locationAdapter.notifyDataSetChanged();
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                return false;
            }
        });
    }

    private void ChooseDate(){

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DATE);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);

        binding.ivDate1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatePickerDialog datePickerDialog =new DatePickerDialog(RecentLocationsActivity.this,R.style.DatePickerDialogTheme, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
                        calendar.set(i, i1, i2); //i: year, i1: month, i2: day

                        date_from = simpleDateFormat.format(calendar.getTime());
                        binding.tvDate1.setText("From: "+ date_from);
                        isDatePicker = true;
                        loadListByDate();
                    }
                },year,month,day);
                datePickerDialog.show();

            }
        });

        binding.ivDate2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatePickerDialog datePickerDialog =new DatePickerDialog(RecentLocationsActivity.this,R.style.DatePickerDialogTheme, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
                        calendar.set(i, i1, i2); //i: year, i1: month, i2: day

                        date_to = simpleDateFormat.format(calendar.getTime());
                        binding.tvDate2.setText("To: "+ date_to);
                        isDatePicker = true;
                        loadListByDate();
                    }
                },year,month,day);
                datePickerDialog.show();

            }
        });
    }

    private void loadListByDate(){
        try{
            if (date_from.length()>4 && date_to.length()>2) {
                binding.progressCircular.setVisibility(View.VISIBLE);
                for (RecentLocations location : locationList){
                    if (location.getTime().contains(date_from) || location.getTime().contains(date_to)){
                        locationListByDate.add(location);
                    }
                }

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(locationListByDate.isEmpty()) {
                            Toast.makeText(RecentLocationsActivity.this, "No Location Found", Toast.LENGTH_SHORT).show();
                            locationAdapter.loadListLocation(locationList);
                            locationAdapter.notifyDataSetChanged();
                            binding.progressCircular.setVisibility(View.GONE);
                        } else {
                            locationAdapter.loadListLocation(locationListByDate);
                            locationAdapter.notifyDataSetChanged();
                            binding.progressCircular.setVisibility(View.GONE);
                        }
                    }
                },400);


            }
        }
        catch(Exception e){
            e.printStackTrace();
        }

    }

    private String getPastTimeString(Date postDate) {

        Date currentDate = new Date();
        long diffInTime = currentDate.getTime() - postDate.getTime();
        long diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(diffInTime);
        long diffInHour = TimeUnit.MILLISECONDS.toHours(diffInTime);
        long diffInYear = TimeUnit.MILLISECONDS.toDays(diffInTime) / 365l;
        long diffInMonth = TimeUnit.MILLISECONDS.toDays(diffInTime) / 30l;
        long diffInDay = TimeUnit.MILLISECONDS.toDays(diffInTime);

        if (diffInYear < 1) {
            if (diffInMonth < 1) {
                if (diffInDay < 1) {
                    if (diffInHour < 1) {
                        if (diffInMinutes < 1) {
                            return "Just now";
                        } else {
                            return diffInMinutes + " minutes";
                        }
                    } else {
                        return diffInHour + " hours";
                    }
                } else {
                    return diffInDay + " days";
                }
            } else {
                return diffInMonth + " months";
            }
        } else {
            return diffInYear + " years";
        }
    }

    class Load_Data extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
//            RecentLocations location1 = new RecentLocations("16.034689", "108.214164", "location1", String.valueOf((int) new Date().getTime()));
//            RecentLocations location2 = new RecentLocations("16.037497", "108.211494", "location2", String.valueOf((int) new Date().getTime()));
//            RecentLocations location3 = new RecentLocations("16.037580", "108.216224", "location3", String.valueOf((int) new Date().getTime()));
//            RecentLocations location4 = new RecentLocations("16.040322", "108.221027", "location4", String.valueOf((int) new Date().getTime()));
//            RecentLocations location5 = new RecentLocations("16.036118", "108.217725", "location5", String.valueOf((int) new Date().getTime()));
//            locationsDAO.insert(location1, location2, location3, location4, location5);
            locationList.clear();
            locationList.addAll(locationsDAO.getAllLocations());
            Collections.reverse(locationList);

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
        String latitude, longitude;
        ExecuteQueryListener listener;
        public Update_Data(String name_new, int position, String la, String lo, ExecuteQueryListener listener) {
            this.name_new = name_new;
            this.position = position;
            this.latitude = la;
            this.longitude = lo;
            this.listener = listener;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try{
                locationsDAO.updateName(latitude, longitude, name_new);
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
        public Delete_Data(int position, ExecuteQueryListener listener) {
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
                RecentLocations location = locationList.get(position);
                double la_from = Double.parseDouble(location.getLatitude())-0.00005;
                double la_to = Double.parseDouble(location.getLatitude())+0.00005;
                double lo_from = Double.parseDouble(location.getLongitude())-0.00005;
                double lo_to = Double.parseDouble(location.getLongitude())+0.00005;


                for (int i=0; i< locationList.size(); i++) {
                    double la_d = Double.parseDouble(locationList.get(i).getLatitude());
                    double lo_d = Double.parseDouble(locationList.get(i).getLongitude());
                    if (la_from < la_d && la_to > la_d && lo_from < lo_d && lo_to > lo_d) {
                        locationsDAO.delete(locationList.get(i));
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