package com.youtubeminimizer.youtubebackgroundplayer;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by Abhilash on 22-09-2017
 */

public class Utils {
    public static final String ACTION_MAXIMIZE="com.abhilash.developer.youtubebackgroundplayer.maximize";
    public static final String ACTION_CLOSE="com.abhilash.developer.youtubebackgroundplayer.close";

    public static boolean isNetworkAvailable(Context context){
        ConnectivityManager connectivityManager=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo=connectivityManager.getActiveNetworkInfo();
        return networkInfo!=null&&networkInfo.isConnectedOrConnecting();
    }


    public static boolean isDebuggable(Context ctx)
    {
        boolean debuggable = false;

        PackageManager pm = ctx.getPackageManager();
        try
        {
            ApplicationInfo appinfo = pm.getApplicationInfo(ctx.getPackageName(), 0);
            debuggable = (0 != (appinfo.flags & ApplicationInfo.FLAG_DEBUGGABLE));
        }
        catch(PackageManager.NameNotFoundException e)
        {
        /*debuggable variable will remain false*/
        }

        return debuggable;
    }

}
