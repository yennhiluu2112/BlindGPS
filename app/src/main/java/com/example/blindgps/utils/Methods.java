package com.example.blindgps.utils;

import android.content.Context;
import android.net.ConnectivityManager;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Methods {

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
        if (connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected()){
            try {
                InetAddress address = InetAddress.getByName("www.google.com");
                if (address.equals("")){
                    return false;
                }
                else return true;
            } catch (UnknownHostException e) {
                return false;
            }
        }
        else{
            return false;
        }
    }

}
