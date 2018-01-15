package com.youtubeminimizer.youtubebackgroundplayer;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.youtubeminimizer.youtubebackgroundplayer.retrofit.ApiClient;
import com.youtubeminimizer.youtubebackgroundplayer.retrofit.VersionResponse;
import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final int CODE_DRAW_OVER_OTHER_APP_PERMISSION = 2084;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkConnection();
        /*Appodeal.disableLocationPermissionCheck();
        Appodeal.initialize(MainActivity.this, getResources().getString(R.string.appodeal_app_id), Appodeal.INTERSTITIAL);

        if(getIntent().getBooleanExtra("ad",false)){
            Appodeal.show(this,Appodeal.INTERSTITIAL);
            Appodeal.setInterstitialCallbacks(new InterstitialCallbacks() {
                @Override
                public void onInterstitialLoaded(boolean b) {

                }

                @Override
                public void onInterstitialFailedToLoad() {

                }

                @Override
                public void onInterstitialShown() {

                }

                @Override
                public void onInterstitialClicked() {

                }

                @Override
                public void onInterstitialClosed() {
                    finish();
                }
            });
        }else{
            setContentView(R.layout.activity_main);
            checkConnection();
        }*/
    }

    private void checkConnection(){

        if(Utils.isNetworkAvailable(MainActivity.this)){
            Fabric.with(this, new Crashlytics());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {

                if(checkSelfPermission(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)== PackageManager.PERMISSION_GRANTED){
                    getVersion();
                }else{
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, CODE_DRAW_OVER_OTHER_APP_PERMISSION);
                }
            } else {
               getVersion();
            }
        }else{

            Snackbar.make(findViewById(R.id.imgLogo),"No Internet Connection!",Snackbar.LENGTH_INDEFINITE)
                    .setActionTextColor(Color.RED)
                    .setAction("Retry", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            checkConnection();
                        }
                    })
                    .show();
        }
    }

    private void getVersion(){
        Call<VersionResponse> call= ApiClient.getClient().getVersion();
        call.enqueue(new Callback<VersionResponse>() {
            @Override
            public void onResponse(Call<VersionResponse> call, final Response<VersionResponse> response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            String currentVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                            String newVersion=response.body().getVersion_android();
                            Log.d("testing","Current Version: "+currentVersion+" New Version: "+newVersion);
                            if(Float.valueOf(currentVersion)<Float.valueOf(newVersion)){
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse("market://details?id="+getPackageName()));
                                startActivity(intent);
                            }else{
                                continueExecution();
                            }
                        }catch (Exception e){
                            Log.d("testing","Exception in getting current version: "+e.toString());
                            Snackbar.make(findViewById(R.id.imgLogo),"Version check failed!",Snackbar.LENGTH_INDEFINITE)
                                    .setActionTextColor(Color.GREEN)
                                    .setAction("Retry", new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            getVersion();
                                        }
                                    });
                        }

                    }
                });
            }

            @Override
            public void onFailure(Call<VersionResponse> call, Throwable t) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Snackbar.make(findViewById(R.id.imgLogo),"Version check failed!",Snackbar.LENGTH_INDEFINITE)
                                .setActionTextColor(Color.GREEN)
                                .setAction("Retry", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        getVersion();
                                    }
                                });
                    }
                });
            }
        });
    }



    private void continueExecution(){

        Handler handler=new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent serviceIntent=new Intent(MainActivity.this,FloatingViewService.class);
                startService(serviceIntent);
                finish();
            }
        },1000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CODE_DRAW_OVER_OTHER_APP_PERMISSION) {
            //Check if the permission is granted or not.
            if (resultCode == RESULT_OK) {
                getVersion();
            } else { //Permission is not available
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    if(!Settings.canDrawOverlays(MainActivity.this)){
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + getPackageName()));
                        startActivityForResult(intent, CODE_DRAW_OVER_OTHER_APP_PERMISSION);
                        Toast.makeText(this, "Please provide permission to draw over other apps", Toast.LENGTH_LONG).show();
                    }else{
                        getVersion();
                    }

                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
