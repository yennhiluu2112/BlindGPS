package com.example.blindgps.utils;

import android.content.Context;
import android.net.ConnectivityManager;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class Methods {

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
        if (connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected()){
            return true;
        }
        else{
            return false;
        }
    }

    public static String getPastTimeString(Date postDate) {

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
                        if (diffInMinutes<60){
                            return diffInHour + " hours" + diffInMinutes + "minutes";
                        }
                        else{
                            return diffInHour + " hours";
                        }

                    }
                } else {
                    if (diffInHour<24){
                        return diffInDay + " days" + diffInHour + " hours";
                    }
                    else{
                        return diffInDay + " days";
                    }
                }

            } else {
                if (diffInDay<30){
                    return diffInMonth + " months" + diffInDay + " days";

                }else{
                    return diffInMonth + " months";
                }

            }
        } else {
            if (diffInMonth<12){
                return diffInYear + " years" + diffInMonth + " months";
            }
            else{
                return diffInYear + " years";
            }


        }
    }

}
