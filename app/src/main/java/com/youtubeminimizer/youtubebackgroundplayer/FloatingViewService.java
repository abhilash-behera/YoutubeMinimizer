package com.youtubeminimizer.youtubebackgroundplayer;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import io.fabric.sdk.android.Fabric;

import static android.view.View.GONE;
import static android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD;
import static android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN;
import static android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
import static android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;

public class FloatingViewService extends Service {
    private WindowManager mWindowManager;
    private View mFloatingView;
    private WindowManager.LayoutParams params;
    private RelativeLayout controlLayout;
    private NotificationManager notificationManager;
    private WebView webView;
    private FrameLayout frameLayout;
    private boolean minimized;
    //private InterstitialAd mInterstitialAd;
    private BroadcastReceiver broadcastReceiver;
    private PowerManager.WakeLock mWakeLock;
    private AdView adView;
    private LinearLayout menuLayout;
    private LinearLayout facebookLayout;
    private LinearLayout twitterLayout;
    private LinearLayout contactLayout;
    private LinearLayout cancelLayout;
    private LinearLayout rateLayout;
    private LinearLayout imgMinimize;
    private final String FACEBOOK_URL="https://www.facebook.com/YouTubeMinimizer";
    private final String FACEBOOK_PAGE_ID="718966274963018";

    public FloatingViewService() {

    }


    public String getFacebookPageUrl(Context context){
        PackageManager packageManager=context.getPackageManager();
        try{
            int versionCode=packageManager.getPackageInfo("com.facebook.katana",0).versionCode;
            String facebookPageId="";
            facebookPageId="fb://page/"+FACEBOOK_PAGE_ID;
            Log.d("awesome","Facebook Page ID: "+facebookPageId);
            return facebookPageId;
        }catch (Exception e){
            Log.d("awesome","Exception in getting facebook page url: "+e.toString());
            return FACEBOOK_URL;
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try{
            if(intent.getAction().equalsIgnoreCase(Utils.ACTION_CLOSE)){
                notificationManager.cancel(1);
                stopSelf();
            }else if(intent.getAction().equalsIgnoreCase(Utils.ACTION_MAXIMIZE)){
                Log.d("awesome","Called maximize");
                minimized=false;
                if(adView!=null){
                    adView.setVisibility(View.VISIBLE);
                    adView.resume();
                }
                /*Intent intent1=new Intent(getBaseContext(),MainActivity.class);
                intent1.putExtra("ad",true);
                startActivity(intent);*/

                notificationManager.cancel(1);

                params = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        0,
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
                                FLAG_KEEP_SCREEN_ON|
                                FLAG_TURN_SCREEN_ON|
                                FLAG_DISMISS_KEYGUARD,
                        PixelFormat.TRANSLUCENT);
                params.gravity=Gravity.CENTER;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    params.type|= WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
                } else {
                    params.type|= WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
                }

                /*params.flags=WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|FLAG_KEEP_SCREEN_ON;
                params.height=MATCH_PARENT;
                params.width=MATCH_PARENT;
                params.gravity=Gravity.CENTER;*/
                mWindowManager.updateViewLayout(frameLayout,params);
                webView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        return false;
                    }
                });
                webView.scrollTo(0,0);

                controlLayout.setVisibility(View.VISIBLE);
                mFloatingView.setAlpha(1.0f);
            }
        }catch(Exception e){
            Log.d("awesome","Exception in catching intent: "+e.toString());
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        Log.d("FAS","Service created");
        super.onCreate();
        Fabric.with(this,new Crashlytics());
        /*mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.interstitial_ad_id));*/

        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);

        broadcastReceiver=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                    Log.d("ybp","screen off");
                    mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "tag");
                    mWakeLock.acquire();
                } /*else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                    Log.d("ybp","screen on");
                }else if(intent.getAction().equals(Intent.ACTION_USER_PRESENT)){
                    Log.d("ybp","screen unlocked");
                    params.type=TYPE_APPLICATION_OVERLAY;
                    mWindowManager.updateViewLayout(frameLayout,params);
                }*/
            }
        };

        registerReceiver(broadcastReceiver,filter);

        // set the ad unit ID
        //mInterstitialAd.setAdUnitId(getString(R.string.interstitial_ad_id));
        frameLayout=new FrameLayout(FloatingViewService.this){
            @Override
            public boolean dispatchKeyEvent(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_BACK)
                {
                    if (event.getAction() == KeyEvent.ACTION_DOWN  &&  event.getRepeatCount() == 0)
                    {
                        getKeyDispatcherState().startTracking(event, this);
                        return true;

                    }

                    else if (event.getAction() == KeyEvent.ACTION_UP)
                    {
                        getKeyDispatcherState().handleUpEvent(event);

                        if (event.isTracking() && !event.isCanceled())
                        {
                            if(webView.canGoBack()){
                                webView.goBack();
                            }else{
                                Toast.makeText(FloatingViewService.this, "You are already on the first page.", Toast.LENGTH_SHORT).show();
                            }
                            return true;
                        }
                    }
                }

                return super.dispatchKeyEvent(event);
            }
        };
        //Inflate the floating view layout we created
        mFloatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_widget,null);
        frameLayout.addView(mFloatingView);

        /*params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                0,
                PixelFormat.TRANSLUCENT);*/

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
                            FLAG_KEEP_SCREEN_ON|
                            FLAG_TURN_SCREEN_ON|
                            FLAG_DISMISS_KEYGUARD,
                    PixelFormat.TRANSLUCENT);
        } else {
            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
                            FLAG_KEEP_SCREEN_ON|
                            FLAG_TURN_SCREEN_ON|
                            FLAG_DISMISS_KEYGUARD,
                    PixelFormat.TRANSLUCENT);
        }


        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(frameLayout, params);

        mFloatingView.setFocusable(false);
        mFloatingView.setFocusableInTouchMode(false);

        webView=mFloatingView.findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new MyWebViewClient());
        webView.loadUrl("https://www.youtube.com");
        webView.setFocusableInTouchMode(true);
        webView.requestFocus();

        adView=mFloatingView.findViewById(R.id.adView);
        if(Utils.isDebuggable(FloatingViewService.this)){
            AdRequest adRequest=new AdRequest.Builder()
                    .addTestDevice("7E38B8C20969D2538EBBB38C341D48EC")
                    .build();
            adView.loadAd(adRequest);
        }else{
            AdRequest adRequest=new AdRequest.Builder()
                    .build();
            adView.loadAd(adRequest);
        }
        AdRequest adRequest=new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        controlLayout=mFloatingView.findViewById(R.id.controlLayout);
        imgMinimize=mFloatingView.findViewById(R.id.imgMinimize);
        imgMinimize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(adView!=null){
                    adView.setVisibility(GONE);
                    adView.pause();
                }
                /*if(Utils.isDebuggable(FloatingViewService.this)){
                    AdRequest adRequest=new AdRequest.Builder()
                            .addTestDevice("7E38B8C20969D2538EBBB38C341D48EC")
                            .build();
                    mInterstitialAd.loadAd(adRequest);
                    mInterstitialAd.setAdListener(new AdListener(){
                        @Override
                        public void onAdLoaded() {
                            super.onAdLoaded();
                            Log.d("Ads","Interstitial ad loaded");
                            mInterstitialAd.show();
                        }
                    });
                }else{
                    AdRequest adRequest=new AdRequest.Builder()
                            .build();
                    mInterstitialAd.loadAd(adRequest);
                    mInterstitialAd.setAdListener(new AdListener(){
                        @Override
                        public void onAdLoaded() {
                            super.onAdLoaded();
                            Log.d("Ads","Interstitial ad loaded");
                            mInterstitialAd.show();
                        }
                    });
                }*/
                minimized=true;
                params.height=200;
                params.width=360;
                params.gravity=Gravity.BOTTOM|Gravity.END;
                params.flags=FLAG_NOT_FOCUSABLE|FLAG_NOT_TOUCHABLE;
                mWindowManager.updateViewLayout(frameLayout,params);
                controlLayout.setVisibility(GONE);
                webView.scrollTo(0,180);
                webView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        return true;
                    }
                });
                mFloatingView.setAlpha(0.3f);

                notificationManager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);

                //Setting maximize action
                Intent maximizeButton=new Intent(FloatingViewService.this,FloatingViewService.class);
                maximizeButton.setAction(Utils.ACTION_MAXIMIZE);
                PendingIntent maximizePendingIntent=PendingIntent.getService(FloatingViewService.this,0,maximizeButton,0);

                //Setting close action
                Intent closeButton=new Intent(FloatingViewService.this,FloatingViewService.class);
                closeButton.setAction(Utils.ACTION_CLOSE);
                closeButton.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
                PendingIntent closePendingIntent=PendingIntent.getService(FloatingViewService.this,0,closeButton,0);

                //Setting view for notification
                RemoteViews contentView=new RemoteViews(getPackageName(),R.layout.notification_layout);

                //Creating notification
                NotificationCompat.Builder builder=new NotificationCompat.Builder(FloatingViewService.this);
                builder.setContentTitle(getString(R.string.app_name));
                builder.setSmallIcon(R.mipmap.ic_launcher);
                builder.setContent(contentView);
                builder.setOngoing(true);

                //Attaching listeners to notification actions
                contentView.setOnClickPendingIntent(R.id.imgClose,closePendingIntent);
                contentView.setOnClickPendingIntent(R.id.imgMaximize,maximizePendingIntent);

                notificationManager.notify(1,builder.build());
            }
        });


        mFloatingView.findViewById(R.id.imgClose).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopSelf();
            }
        });

        //Inflating menu layout
        menuLayout=mFloatingView.findViewById(R.id.menuLayout);



        //Inflating facebook layout
        facebookLayout=mFloatingView.findViewById(R.id.facebookLayout);
        facebookLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getBaseContext(), "You clicked on facebook layout", Toast.LENGTH_SHORT).show();
                Intent facebookIntent=new Intent(Intent.ACTION_VIEW);
                String facebookUrl=getFacebookPageUrl(getBaseContext());
                facebookIntent.setData(Uri.parse(facebookUrl));
                facebookIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(facebookIntent);

                imgMinimize.callOnClick();
            }
        });

        //Inflating twitter layout
        twitterLayout=mFloatingView.findViewById(R.id.twitterLayout);
        twitterLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getBaseContext(), "You clicked on twitter layout", Toast.LENGTH_SHORT).show();
                imgMinimize.callOnClick();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Intent twitterIntent=new Intent(Intent.ACTION_VIEW);
                        twitterIntent.setData(Uri.parse("twitter://user?user_id=923794022837211137"));
                        twitterIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(twitterIntent);
                    }
                }).start();
            }
        });

        //Inflating contact layout
        contactLayout=mFloatingView.findViewById(R.id.contactLayout);
        contactLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imgMinimize.callOnClick();
                Intent contactIntent=new Intent(getBaseContext(),ContactActivity.class);
                contactIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(contactIntent);
            }
        });

        //Inflate cancel layout
        cancelLayout=mFloatingView.findViewById(R.id.cancelLayout);
        cancelLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getBaseContext(), "You clicked on cancel layout", Toast.LENGTH_SHORT).show();
                menuLayout.animate().alpha(0.0f)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                menuLayout.setVisibility(GONE);
                            }
                        })
                        .start();
            }
        });

        rateLayout=mFloatingView.findViewById(R.id.rateLayout);
        rateLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imgMinimize.callOnClick();
                Uri uri = Uri.parse("market://details?id=" + getBaseContext().getPackageName());
                Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                // To count with Play market backstack, After pressing back button,
                // to taken back to our application, we need to add following flags to intent.
                goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                        Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                try {
                    startActivity(goToMarket);
                } catch (ActivityNotFoundException e) {
                    Log.d("awesome","Opening PlayStore in browser");
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("http://play.google.com/store/apps/details?id=" + getBaseContext().getPackageName())));
                }
            }
        });


        final TextView textViewOptions=mFloatingView.findViewById(R.id.textViewOptions);

        textViewOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menuLayout.setVisibility(View.VISIBLE);
                menuLayout.setAlpha(0.0f);
                menuLayout.animate().alpha(1.0f)
                        .setDuration(300)
                        .start();
            }
        });

        NotificationCompat.Builder builder=new NotificationCompat.Builder(FloatingViewService.this);
        builder.setContentTitle(getString(R.string.app_name));
        builder.setSmallIcon(R.mipmap.ic_launcher);
        startForeground(2,builder.build());
    }

    @Override
    public void onDestroy() {
        Log.d("FAS","Service destroyed");
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
        if (frameLayout != null) mWindowManager.removeView(frameLayout);
        try{
            this.mWakeLock.release();
        }catch (Exception ignored){}
        try{
            notificationManager.cancel(1);
            notificationManager.cancel(2);
        }catch (Exception e){
            Log.d("awesome","Exception in closing notification: "+e);
        }
    }


    private class MyWebViewClient extends WebViewClient{
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return false;
        }

        @Override
        public void onLoadResource(WebView view, String url) {
            if(webView.getScrollY()!=180){
                if(webView.getUrl().contains("watch?v")&&minimized){
                    webView.scrollTo(0,180);
                }
            }
            super.onLoadResource(view, url);
        }
    }

    @Override
    public void onLowMemory() {
        Toast.makeText(this, "Service stopping because of low memory.", Toast.LENGTH_SHORT).show();
    }

}