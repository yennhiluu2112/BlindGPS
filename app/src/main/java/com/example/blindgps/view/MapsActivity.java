package com.example.blindgps.view;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.example.blindgps.R;
import com.example.blindgps.databinding.ActivityMapsBinding;
import com.example.blindgps.viewmodel.ExecuteQueryListener;
import com.example.blindgps.model.RecentLocations;
import com.example.blindgps.utils.Methods;
import com.example.blindgps.viewmodel.AppDatabase;
import com.example.blindgps.viewmodel.RecentLocationsDAO;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener, RoutingListener, LocationListener {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private SupportMapFragment mapFragment;
    private FusedLocationProviderClient client;
    private MarkerOptions marker1, marker2;
    private static Marker marker_1, marker_2;
    private static List<Polyline> polylines;
    private static LatLng latLng2,latLng1;
    PolylineOptions polylineOptions;
    private static String lo2, la2;
    private String provider;
    private Socket mSocket;
    private LocationManager locationManager;
    private Criteria criteria;
    private static double la1, lo1;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private AppDatabase appDatabase;
    private RecentLocationsDAO locationsDAO;
    private static ArrayList<RecentLocations> locationsArrayList;
    private Notification notification;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (!Methods.isNetworkAvailable(MapsActivity.this)){
            Toast.makeText(MapsActivity.this, "Please connect to the internet", Toast.LENGTH_SHORT).show();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel("My Notification", "My Notification", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("My Notification");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        appDatabase = AppDatabase.getInstance(this);
        locationsDAO = appDatabase.locationsDAO();

        locationsArrayList = new ArrayList<RecentLocations>();

        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        client = LocationServices.getFusedLocationProviderClient(this);

        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        sharedPreferences = getSharedPreferences("Location_History", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        MapsActivity.Load_Data load_data = new MapsActivity.Load_Data();
        load_data.execute();

        ItemClick();

        setUpNotification();

    }



    private void ItemClick(){
        binding.imvDirection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FindRoutes(latLng1, latLng2);

            }
        });

        binding.btnOutDirection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for(Polyline line : polylines)
                {
                    line.remove();
                }
                polylines.clear();
                binding.btnOutDirection.setVisibility(View.GONE);
            }
        });

        binding.imvGgMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (la2!=null && lo2!=null){
                    Open_Google_Map(la2, lo2);
                }
                else{
                    Toast.makeText(MapsActivity.this, "No destination", Toast.LENGTH_SHORT).show();
                }

            }
        });


        binding.imvHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(MapsActivity.this, RecentLocationsActivity.class);
                startActivity(intent);
            }
        });
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MapsActivity.this, new String[]{ Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 44);
        }
        else{
            //mMap.setMyLocationEnabled(true);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
            //criteria = new Criteria();
            //criteria.setAccuracy(Criteria.ACCURACY_COARSE);
            //provider = String.valueOf(locationManager.getBestProvider(criteria, true));
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if(location!=null){
                la1 = location.getLatitude();
                lo1 = location.getLongitude();
                latLng1 = new LatLng(la1, lo1);
                //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng1, 10));
                //mMap.animateCamera(CameraUpdateFactory.zoomTo(14));
                marker1 = new MarkerOptions()
                        .position(latLng1)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                        .title("You are here");
                marker_1 = mMap.addMarker(marker1);
                marker_1.setVisible(true);


                CircleOptions circleOptions = new CircleOptions()
                        .center(latLng1)
                        .radius(100)
                        .strokeWidth(3f)
                        .strokeColor(Color.BLUE)
                        .fillColor(Color.argb(70,110, 164, 255));
                mMap.addCircle(circleOptions);

                ConnectSocket();
            }
            else{
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            }
        }
    }

    private void ConnectSocket(){
        try {
//            mSocket = IO.socket("http://192.168.1.20:5000");
            mSocket = IO.socket("http://192.168.1.154:5000");

            mSocket.on("server-send-data", onRetrieveData);
            mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
            mSocket.emit("client-send-data", "Lap trinh android");
        } catch (Exception e) {
            e.printStackTrace();
            e.toString();
        }
        mSocket.connect();
    }

    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //CMD
                    Toast.makeText(MapsActivity.this, "Lỗi socket: " + args[0], Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    private Emitter.Listener onRetrieveData = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String obj = args[0].toString();
                    try {
                        JSONObject jsObj = new JSONObject(obj);
                        String serversend = jsObj.getString("serversend");
                        JSONObject jsonobj1 = new JSONObject(serversend);
                        lo2 = jsonobj1.getString("longtitude");
                        la2 = jsonobj1.getString("latitude");

                        editor.putString("lo2", lo2);
                        editor.putString("la2", la2);
                        editor.apply();

                        latLng2 = new LatLng(Double.parseDouble(la2), Double.parseDouble(lo2));
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng2, 10));
                        mMap.animateCamera(CameraUpdateFactory.zoomTo(14));
                        marker2 = new MarkerOptions()
                                .position(latLng2)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                                .title(setMarkerName(la2, lo2));
                        marker_2 = mMap.addMarker(marker2);
                        marker_2.setVisible(true);
                        marker_2.showInfoWindow();

                        CircleOptions circleOptions = new CircleOptions()
                                .center(latLng2)
                                .radius(100)
                                .strokeWidth(3f)
                                .strokeColor(Color.RED)
                                .fillColor(Color.argb(70,150,50,50));
                        mMap.addCircle(circleOptions);

                        InsertNewLocation(la2, lo2);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    };

    private String setMarkerName(String la2, String lo2){
        String title="";
        for (RecentLocations l : locationsArrayList) {

            double la_from = Double.parseDouble(l.getLatitude())-0.00005;
            double la_to = Double.parseDouble(l.getLatitude())+0.00005;
            double lo_from = Double.parseDouble(l.getLongitude())-0.00005;
            double lo_to = Double.parseDouble(l.getLongitude())+0.00005;
            double la_d = Double.parseDouble(la2);
            double lo_d = Double.parseDouble(lo2);

            if (la_from < la_d && la_to > la_d && lo_from < lo_d && lo_to > lo_d) {
                title = l.getLocation_name();
                break;
            }
            else{
                title="Your partner is here";
            }
        }
        return title;
    }

    private void InsertNewLocation(String la2, String lo2){
        if (locationsArrayList.isEmpty()){
            MapsActivity.Insert_Data insert_data = new MapsActivity.Insert_Data(lo2, la2, null);
            insert_data.execute();
            sendNotification();
        }
        else{
            RecentLocations last_location = locationsArrayList.get(locationsArrayList.size()-1);
            try{
                Date currentDate = new Date();
                Date date = new SimpleDateFormat("dd/MM/yyyy HH:mm").parse(last_location.getTime());
                long diffInTime = currentDate.getTime() - date.getTime();
                long diffInHour = TimeUnit.MILLISECONDS.toHours(diffInTime);
                double la_from = Double.parseDouble(last_location.getLatitude())-0.00005;
                double la_to = Double.parseDouble(last_location.getLatitude())+0.00005;
                double lo_from = Double.parseDouble(last_location.getLongitude())-0.00005;
                double lo_to = Double.parseDouble(last_location.getLongitude())+0.00005;
                double la2_d = Double.parseDouble(la2);
                double lo2_d = Double.parseDouble(lo2);
                String name ="";

                if (!(la_from<la2_d && la_to>la2_d && lo_from<lo2_d && lo_to>lo2_d)) {
                    for (RecentLocations l : locationsArrayList){
                        double la_d = Double.parseDouble(l.getLatitude());
                        double lo_d= Double.parseDouble(l.getLongitude());
                        if (la_from>la_d && la_to<la_d && lo_from>lo_d && lo_to<lo_d){
                            name = l.getLocation_name();
                            break;
                        }
                        else
                            name = "New Location";
                    }
                    MapsActivity.Insert_Data insert_data = new MapsActivity.Insert_Data(lo2, la2, name);
                    insert_data.execute();
                    sendNotification();
                }
                else if (diffInHour>5){
                    MapsActivity.Insert_Data insert_data = new MapsActivity.Insert_Data(lo2, la2, name);
                    insert_data.execute();
                    sendNotification();
                }
            }
            catch(Exception e){
                e.printStackTrace();
            }




        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 44){
            if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                getCurrentLocation();
            }
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        if (getIntent()!=null){
            Bundle extras = getIntent().getExtras();
            if (extras!=null) {
                ShowHistory(extras);
            }
            else{
                getCurrentLocation();
                addFavMarker();
            }

        }

    }

    private void addFavMarker() {
        for (RecentLocations l : locationsArrayList){
            if (l.getFav()){
                String la = l.getLatitude();
                String lo = l.getLongitude();
                double la_from = Double.parseDouble(l.getLatitude())-0.00005;
                double la_to = Double.parseDouble(l.getLatitude())+0.00005;
                double lo_from = Double.parseDouble(l.getLongitude())-0.00005;
                double lo_to = Double.parseDouble(l.getLongitude())+0.00005;
                if (la2!=null && lo2!=null){
                    double la_d = Double.parseDouble(la2);
                    double lo_d = Double.parseDouble(lo2);

                    if (!(la_from < la_d && la_to > la_d && lo_from < lo_d && lo_to > lo_d))
                    {
                        LatLng latLng = new LatLng(Double.parseDouble(la), Double.parseDouble(lo));
                        MarkerOptions marker = new MarkerOptions()
                                .position(latLng)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                                .title(l.getLocation_name());
                        mMap.addMarker(marker);

                        CircleOptions circleOptions = new CircleOptions()
                                .center(latLng)
                                .radius(100)
                                .strokeWidth(3f)
                                .strokeColor(Color.rgb(255,165,0))
                                .fillColor(Color.argb(70,255,165,0));
                        mMap.addCircle(circleOptions);
                    }
                }
            }
        }
    }

    public void FindRoutes(LatLng start, LatLng end){
        if (start==null || end==null){
            Toast.makeText(MapsActivity.this, "Unable to get direction", Toast.LENGTH_LONG).show();
        }
        else{
            Routing routing = new Routing.Builder()
                    .travelMode(AbstractRouting.TravelMode.DRIVING)
                    .withListener(this)
                    .alternativeRoutes(true)
                    .waypoints(start, end)
                    .key("AIzaSyCaIgerehFWzBZuERI0lkVpp3y-fZIz94s")
                    .build();
            routing.execute();

        }
    }

    @Override
    public void onRoutingFailure(RouteException e) {
        View view = findViewById(android.R.id.content);
        Toast.makeText(MapsActivity.this, e.toString(), Toast.LENGTH_LONG).show();
        e.toString();
        binding.btnOutDirection.setVisibility(View.GONE);
        Toast.makeText(MapsActivity.this, "Cannot get directions. Please choose Google Maps.", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRoutingStart() {

    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> arrayList, int i) {
        binding.btnOutDirection.setVisibility(View.VISIBLE);
        polylineOptions= new PolylineOptions();
        polylines = new ArrayList<>();
        for (int j=0; j<arrayList.size(); j++){
            if (j==i){
                polylineOptions.color(getResources().getColor(com.google.android.material.R.color.primary_dark_material_dark));
                polylineOptions.width(7);
                polylineOptions.addAll(arrayList.get(i).getPoints());
                polylines.add(mMap.addPolyline(polylineOptions));
            }
        }

    }

    @Override
    public void onRoutingCancelled() {
        if(latLng1!=null && latLng2!=null){
            //FindRoutes(latLng1,latLng2);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if(latLng1!=null && latLng2!=null){
            //FindRoutes(latLng1,latLng2);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(this);

    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        locationManager.removeUpdates(this);
        la1 = location.getLatitude();
        lo1 = location.getLongitude();

    }

    private void setUpNotification() {
        Intent intent = new Intent(this, MapsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(MapsActivity.this, "My Notification");
        builder.setContentTitle("New location!")
                .setContentText("Open maps to see")
                .setSmallIcon(R.drawable.small_ic)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notification = builder.build();
        notification.flags = Notification.FLAG_NO_CLEAR;


    }

    private void sendNotification() {
        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(MapsActivity.this);
        managerCompat.notify((int) new Date().getTime(),notification);
    }

    private void ShowHistory(Bundle extras) {
        try {
            mMap.clear();
            RecentLocations locations = (RecentLocations) extras.getSerializable("location");
            String time_count = extras.getString("time");
            if (time_count.equals("gone")){
                binding.tvTime.setVisibility(View.GONE);
                binding.tvTimeCount.setVisibility(View.GONE);
                binding.tvName.setVisibility(View.GONE);
                binding.constraintNameFav.setVisibility(View.VISIBLE);
            }
            binding.imvHistory.setVisibility(View.GONE);
            binding.imvDirection.setVisibility(View.GONE);
            binding.constraintBack.setVisibility(View.VISIBLE);



            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            String la_his = locations.getLatitude();
            String lo_his = locations.getLongitude();
            LatLng latLng = new LatLng(Double.parseDouble(la_his), Double.parseDouble(lo_his));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(14));
            MarkerOptions marker = new MarkerOptions()
                    .position(latLng)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                    .title(locations.getLocation_name());
            Marker marker_ = mMap.addMarker(marker);

            CircleOptions circleOptions = new CircleOptions()
                    .center(latLng)
                    .radius(100)
                    .strokeWidth(3f)
                    .strokeColor(Color.rgb(255,165,0))
                    .fillColor(Color.argb(70,255,165,0));
            mMap.addCircle(circleOptions);
//            marker_1.setVisible(false);
//            marker_2.setVisible(false);

            binding.tvTime.setText(locations.getTime());
            binding.tvTimeCount.setText(time_count);

            Edit_Name(locations, time_count);

            binding.btnBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (extras.getString("from").equals("history")){
                        Intent intent = new Intent(MapsActivity.this, RecentLocationsActivity.class);
                        startActivity(intent);
                    }
                    else{
                        Intent intent = new Intent(MapsActivity.this, FavoriteActivity.class);
                        startActivity(intent);
                    }

//                    marker_1.setVisible(true);
//                    marker_2.setVisible(true);
                    //onBackPressed();
                    marker_.remove();
                }
            });

            if (locations.getFav()){
                binding.ivFav.setImageResource(R.drawable.ic_fav);
            }
            else{
                binding.ivFav.setImageResource(R.drawable.ic_no_fav);
            }

            binding.ivFav.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (locations.getFav())
                    {
                        locations.setFav(false);
                        binding.ivFav.setImageResource(R.drawable.ic_no_fav);
                    }
                    else{
                        locations.setFav(true);
                        binding.ivFav.setImageResource(R.drawable.ic_fav);
                    }

                    MapsActivity.Set_Fav set_fav = new MapsActivity.Set_Fav(locations, new ExecuteQueryListener(){
                        @Override
                        public void onStart() {

                        }

                        @Override
                        public void onEnd() {
                            MapsActivity.Load_Data load_data = new MapsActivity.Load_Data();
                            load_data.execute();
                        }
                    });
                    set_fav.execute();
                }
            });


        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    private void Edit_Name(RecentLocations locations, String time){
        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        View view1 = LayoutInflater.from(MapsActivity.this).inflate(R.layout.dialog_change_name, null,false);
        builder.setView(view1);
        builder.setCancelable(false);

        final AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        Button btn_ok = view1.findViewById(R.id.btn_ok);
        Button btn_cancel = view1.findViewById(R.id.btn_back);
        EditText edt_name = view1.findViewById(R.id.edt_name);

        if (time.equals("gone")){
            binding.ivEdit1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    alertDialog.show();
                }
            });
            binding.tvName1.setText(locations.getLocation_name());
        }
        else{
            binding.ivEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    alertDialog.show();
                }
            });
            binding.tvName.setText(locations.getLocation_name());
        }

        edt_name.setText(locations.getLocation_name());

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });




        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try{
                    String name = edt_name.getText().toString();
                    if(name.length()!=0){
                        MapsActivity.Update_Data update_data = new MapsActivity.Update_Data(name, locations, new ExecuteQueryListener() {
                            @Override
                            public void onStart() {

                            }

                            @Override
                            public void onEnd() {
                                binding.tvName.setText(name);
                                MapsActivity.Load_Data load_data = new MapsActivity.Load_Data();
                                load_data.execute();

                            }
                        });
                        update_data.execute();
                        alertDialog.dismiss();
                    }
                    else{
                        Toast.makeText(MapsActivity.this, "Please re enter", Toast.LENGTH_SHORT).show();
                    }

                }
                catch(Exception e){
                    e.printStackTrace();
                    Toast.makeText(MapsActivity.this, "\"Please re enter", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    private void Open_Google_Map(String latitude, String longitude){
        //String uri = "http://maps.google.com/maps/dir/?api=1&destination=" + latitude + "%2C-" + longitude + " (" + "Your partner is here" + ")";
        //String uri = "http://maps.google.com/maps/dir/?api=1&query=" + latitude + "%2C-" + longitude + " (" + "Your partner is here" + ")";
        //String uri = "http://maps.google.com/maps?q=loc:" + latitude + "," + longitude + " (" + "Your partner is here" + ")";
        //String uri = "https://maps.googleapis.com/maps/api/directions/json?origin="+latLng1.latitude+","+latLng1.longitude+"&destination="+latitude+","+longitude+"  &key=AIzaSyCaIgerehFWzBZuERI0lkVpp3y-fZIz94s";
        String uri = "http://maps.google.com/maps?saddr="+ latLng1.latitude + "," + latLng1.longitude+ "&daddr="+latitude+","+longitude+"";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");
        try
        {
            startActivity(intent);
        }
        catch(ActivityNotFoundException ex)
        {
            try
            {
                Intent unrestrictedIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                startActivity(unrestrictedIntent);
            }
            catch(ActivityNotFoundException innerEx)
            {
                Toast.makeText(this, "Please install a Google Maps application", Toast.LENGTH_LONG).show();
            }
        }
    }


    class Load_Data extends AsyncTask<Void,Void,Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            if (locationsArrayList!=null){
                locationsArrayList.clear();
            }

//            RecentLocations location1 = new RecentLocations( "108.214164","16.034689","location1", new SimpleDateFormat("dd/MM/yyyy HH:mm").format(Calendar.getInstance().getTime()), false);
//            RecentLocations location2 = new RecentLocations("108.211494","16.037497",  "location2", new SimpleDateFormat("dd/MM/yyyy HH:mm").format(Calendar.getInstance().getTime()),false);
//            RecentLocations location3 = new RecentLocations( "108.216224","16.037580", "location3", new SimpleDateFormat("dd/MM/yyyy HH:mm").format(Calendar.getInstance().getTime()),false);
//            RecentLocations location4 = new RecentLocations( "108.221027","16.040322", "location4", new SimpleDateFormat("dd/MM/yyyy HH:mm").format(Calendar.getInstance().getTime()),false);
//            RecentLocations location5 = new RecentLocations( "108.217725", "16.036118","location5", new SimpleDateFormat("dd/MM/yyyy HH:mm").format(Calendar.getInstance().getTime()),false);
//            locationsDAO.insert(location1, location2, location3, location4, location5);

            locationsArrayList.addAll(locationsDAO.getAllLocations());

            //locationsDAO.deleteAll();

            return null;
        }
    }

    class Insert_Data extends AsyncTask<Void,Void,Void> {
        String lo,la, name;
        public Insert_Data(String lo, String la, String name) {
            this.lo = lo;
            this.la = la;
            this.name = name;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try{

                locationsDAO.insert(new RecentLocations(lo, la, name, new SimpleDateFormat("dd/MM/yyyy HH:mm").format(Calendar.getInstance().getTime()), false));

                return null;
            }
            catch(Exception e){
                e.printStackTrace();
                return null;
            }
        }
    }

    class Set_Fav extends AsyncTask<Void, Void, Void>{
        RecentLocations locations;
        ExecuteQueryListener listener;
        public Set_Fav(RecentLocations locations, ExecuteQueryListener listener) {
            this.locations = locations;
            this.listener = listener;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try{
                int count = 0;
                for (RecentLocations l : locationsArrayList){
                    if (l.getFav()){
                        count++;
                    }
                }
                if (count<=10){
                    locationsDAO.update(locations);
                }
                else{
                    Toast.makeText(MapsActivity.this, "You can choose only 10 favorite locations", Toast.LENGTH_SHORT).show();
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

    class Update_Data extends AsyncTask<Void, Void, Void>{
        String name_new;
        RecentLocations locations;
        ExecuteQueryListener listener;
        public Update_Data(String name_new, RecentLocations locations, ExecuteQueryListener listener) {
            this.name_new = name_new;
            this.locations = locations;
            this.listener = listener;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try{
                double la_from = Double.parseDouble(locations.getLatitude())-0.00005;
                double la_to = Double.parseDouble(locations.getLatitude())+0.00005;
                double lo_from = Double.parseDouble(locations.getLongitude())-0.00005;
                double lo_to = Double.parseDouble(locations.getLongitude())+0.00005;


                for (int i=0; i< locationsArrayList.size(); i++) {
                    double la_d = Double.parseDouble(locationsArrayList.get(i).getLatitude());
                    double lo_d = Double.parseDouble(locationsArrayList.get(i).getLongitude());
                    if (la_from < la_d && la_to > la_d && lo_from < lo_d && lo_to > lo_d) {
                        locationsArrayList.get(i).setLocation_name(name_new);
                        locationsDAO.update(locationsArrayList.get(i));
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
