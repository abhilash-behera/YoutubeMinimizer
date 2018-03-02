package com.youtubeminimizer.youtubebackgroundplayer;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdListener;
import com.facebook.ads.AdSize;
import com.facebook.ads.AdView;
import com.facebook.ads.InterstitialAd;
import com.facebook.ads.InterstitialAdListener;

import io.fabric.sdk.android.Fabric;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
import static android.view.View.GONE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD;
import static android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN;
import static android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
import static android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
import static android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;

public class FloatingViewService extends Service {


    private WindowManager mWindowManager;
    private View mFloatingView;
    private WindowManager.LayoutParams params;
    private RelativeLayout controlLayout;
    private NotificationManager notificationManager;
    //private VideoEnabledWebView webView;
    //private WebView webView;
    private FrameLayout frameLayout;
    private boolean minimized;


    private BroadcastReceiver broadcastReceiver;
    private PowerManager.WakeLock mWakeLock;
    //private AdView adView;
    private LinearLayout menuLayout;
    private LinearLayout facebookLayout;
    private LinearLayout twitterLayout;
    private LinearLayout contactLayout;
    private LinearLayout cancelLayout;
    private LinearLayout rateLayout;
    private LinearLayout imgMinimize;
    private LinearLayout imgMenu;
    private final String FACEBOOK_URL="https://www.facebook.com/YouTubeMinimizer";
    private final String FACEBOOK_PAGE_ID="718966274963018";
    public static final String SERVICE_STARTED_INTENT="com.youtubeminimizer.youtubebackgroundplayer";
    private boolean makeFullScreen;

    private VideoEnabledWebView webView;
    private VideoEnabledWebChromeClient webChromeClient;
    private RelativeLayout adViewContainer;
    private AdView adView;
    private InterstitialAd interstitialAd;

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
                //Send service started broadcast
                if(adView!=null){
                    adView.setVisibility(View.VISIBLE);
                }
                Intent serviceStartedIntent=new Intent(SERVICE_STARTED_INTENT);
                sendBroadcast(serviceStartedIntent);
                minimized=false;
                /*if(adView!=null){
                    adView.setVisibility(View.VISIBLE);
                    adView.resume();
                }*/
                /*Intent intent1=new Intent(getBaseContext(),MainActivity.class);
                intent1.putExtra("ad",true);
                startActivity(intent);*/

                notificationManager.cancel(1);

                params.flags=FLAG_SHOW_WHEN_LOCKED|
                        FLAG_KEEP_SCREEN_ON|
                        FLAG_TURN_SCREEN_ON|
                        FLAG_FULLSCREEN|
                        FLAG_DISMISS_KEYGUARD;

                params.width=MATCH_PARENT;
                params.height=MATCH_PARENT;


                Log.d("awesome","makeFullScreen: "+makeFullScreen);
                if(makeFullScreen){
                    params.screenOrientation= SCREEN_ORIENTATION_LANDSCAPE;
                    controlLayout.setVisibility(View.GONE);
                    adViewContainer.setVisibility(GONE);
                }else{
                    params.screenOrientation=ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    controlLayout.setVisibility(View.VISIBLE);
                    adViewContainer.setVisibility(View.VISIBLE);
                }
                mWindowManager.updateViewLayout(frameLayout,params);
                webView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        return false;
                    }
                });
                webView.scrollTo(0,0);
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
        Log.d("awesome","Service created");
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
                try{}catch (Exception e){
                    Log.d("awesome","Exception in key event: "+e.toString());
                }
                if (event.getKeyCode() == KeyEvent.KEYCODE_BACK)
                {
                    if (event.getAction() == KeyEvent.ACTION_DOWN  &&  event.getRepeatCount() == 0) {
                        getKeyDispatcherState().startTracking(event, this);
                        return true;
                    } else if (event.getAction() == KeyEvent.ACTION_UP) {
                        getKeyDispatcherState().handleUpEvent(event);

                        if (event.isTracking() && !event.isCanceled())
                        {
                            if(webView.canGoBack()){
                                webView.goBack();
                                if(makeFullScreen){
                                    makeFullScreen=false;
                                    params.screenOrientation=SCREEN_ORIENTATION_PORTRAIT;
                                    mWindowManager.updateViewLayout(frameLayout,params);
                                }
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
                    FLAG_SHOW_WHEN_LOCKED|
                            FLAG_KEEP_SCREEN_ON|
                            FLAG_TURN_SCREEN_ON|
                            FLAG_DISMISS_KEYGUARD,
                    PixelFormat.TRANSLUCENT);
        } else {
            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
                    FLAG_SHOW_WHEN_LOCKED|
                            FLAG_KEEP_SCREEN_ON|
                            FLAG_TURN_SCREEN_ON|
                            FLAG_DISMISS_KEYGUARD|
                            FLAG_FULLSCREEN,
                    PixelFormat.TRANSLUCENT);
        }

        if(makeFullScreen){
            params.screenOrientation=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            if(adViewContainer!=null){
                adViewContainer.setVisibility(GONE);
            }

        }else{
            params.screenOrientation= SCREEN_ORIENTATION_PORTRAIT;
            if(adViewContainer!=null){
                adViewContainer.setVisibility(View.VISIBLE);
            }

        }


        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(frameLayout, params);

        mFloatingView.setFocusable(false);
        mFloatingView.setFocusableInTouchMode(false);

        webView=mFloatingView.findViewById(R.id.webView);
        View nonVideoLayout = mFloatingView.findViewById(R.id.nonVideoLayout); // Your own view, read class comments
        final ViewGroup videoLayout = mFloatingView.findViewById(R.id.videoLayout); // Your own view, read class comments
        //noinspection all
        View loadingView = ((LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.view_loading_video, null); // Your own view, read class comments
        webChromeClient = new VideoEnabledWebChromeClient(nonVideoLayout, videoLayout, loadingView, webView) // See all available constructors...
        {
            // Subscribe to standard events, such as onProgressChanged()...
            @Override
            public void onProgressChanged(WebView view, int progress)
            {
                // Your code...
            }
        };
        webChromeClient.setOnToggledFullscreen(new VideoEnabledWebChromeClient.ToggledFullscreenCallback()
        {
            @Override
            public void toggledFullscreen(boolean fullscreen)
            {
                Log.d("awesome","Full Screen: "+fullscreen);
                // Your code to handle the full-screen change, for example showing and hiding the title bar. Example:
                if(fullscreen){
                    makeFullScreen =true;
                    Intent maximizeButton=new Intent(FloatingViewService.this,FloatingViewService.class);
                    maximizeButton.setAction(Utils.ACTION_MAXIMIZE);
                    startService(maximizeButton);
                    params.screenOrientation= ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    params.systemUiVisibility=View.SYSTEM_UI_FLAG_LOW_PROFILE;
                    mWindowManager.updateViewLayout(frameLayout,params);
                    controlLayout.setVisibility(GONE);
                    adViewContainer.setVisibility(GONE);


                    /*WindowManager.LayoutParams attrs = getWindow().getAttributes();
                    attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
                    attrs.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
                    getWindow().setAttributes(attrs);
                    if (android.os.Build.VERSION.SDK_INT >= 14)
                    {
                        //noinspection all
                        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
                    }*/
                }else{
                    makeFullScreen =false;
                    params.flags&= ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
                    params.flags&= ~WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
                    params.screenOrientation= SCREEN_ORIENTATION_PORTRAIT;
                    params.systemUiVisibility=View.SYSTEM_UI_FLAG_VISIBLE;
                    mWindowManager.updateViewLayout(frameLayout,params);
                    controlLayout.setVisibility(View.VISIBLE);
                    adViewContainer.setVisibility(View.VISIBLE);

                    /*WindowManager.LayoutParams attrs = getWindow().getAttributes();
                    attrs.flags &= ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
                    attrs.flags &= ~WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
                    getWindow().setAttributes(attrs);
                    if (android.os.Build.VERSION.SDK_INT >= 14)
                    {
                        //noinspection all
                        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
                    }*/
                }

            }
        });


        webView.setWebChromeClient(webChromeClient);
        // Call private class InsideWebViewClient
        webView.setWebViewClient(new InsideWebViewClient());

        // Navigate anywhere you want, but consider that this classes have only been tested on YouTube's mobile site
        webView.loadUrl("http://m.youtube.com");


        /*webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setPluginState(WebSettings.PluginState.ON);
        webView.getSettings().setUserAgentString("Android");
        webView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new MyWebViewClient());
        webView.loadUrl("https://m.youtube.com");*/
        /*webView.setFocusableInTouchMode(true);
        webView.requestFocus();
*/
        Log.d("awesome","Loaded url: "+webView.getUrl());

        /*adView=mFloatingView.findViewById(R.id.adView);
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
        adView.loadAd(adRequest);*/

        adViewContainer=mFloatingView.findViewById(R.id.adViewContainer);
        loadBannerAd();



        controlLayout=mFloatingView.findViewById(R.id.controlLayout);
        imgMinimize=mFloatingView.findViewById(R.id.imgMinimize);
        imgMinimize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*if(adView!=null){
                    adView.setVisibility(GONE);
                    adView.pause();
                }*/

                if(adView!=null){
                    adView.setVisibility(GONE);
                }

                loadInterstitialAd();

                minimized=true;
                makeFullScreen=false;
                params.screenOrientation=SCREEN_ORIENTATION_PORTRAIT;
                params.height=200;
                params.width=360;
                params.gravity=Gravity.BOTTOM|Gravity.END;
                params.flags=FLAG_NOT_FOCUSABLE|FLAG_NOT_TOUCHABLE;
                mWindowManager.updateViewLayout(frameLayout,params);
                controlLayout.setVisibility(GONE);
                adViewContainer.setVisibility(GONE);
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
                imgMinimize.callOnClick();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            Intent twitterIntent=new Intent(Intent.ACTION_VIEW);
                            twitterIntent.setData(Uri.parse("twitter://user?user_id=923794022837211137"));
                            twitterIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(twitterIntent);
                        }catch (Exception e){
                            Log.d("awesome","Exception in starting twitter application: "+e.toString());
                            Intent twitterIntent=new Intent(Intent.ACTION_VIEW);
                            twitterIntent.setData(Uri.parse("https://twitter.com/TubeMinimizer"));
                            twitterIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(twitterIntent);
                        }

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

        imgMenu=mFloatingView.findViewById(R.id.imgMenu);
        imgMenu.setOnClickListener(new View.OnClickListener() {
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

        //Send service started broadcast
        Intent serviceStartedIntent=new Intent(SERVICE_STARTED_INTENT);
        sendBroadcast(serviceStartedIntent);

    }

    private void loadBannerAd() {
        try{
            adView = new AdView(this, "872401172921626_872712976223779", AdSize.BANNER_HEIGHT_50);
            adViewContainer.addView(adView);
            adView.loadAd();

            adView.setAdListener(new AdListener() {
                @Override
                public void onError(Ad ad, AdError adError) {
                    Log.d("awesome","Error in loading floating view banner ad: "+adError.getErrorMessage());
                    if(adError.getErrorCode()==AdError.NO_FILL_ERROR_CODE){
                        Handler handler=new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                loadBannerAd();
                            }
                        },30000);
                    }
                }

                @Override
                public void onAdLoaded(Ad ad) {
                    Log.d("awesome","Floating view banner loaded: "+ad);
                }

                @Override
                public void onAdClicked(Ad ad) {
                    Log.d("awesome","Floating view banner ad clicked: "+ad);
                }

                @Override
                public void onLoggingImpression(Ad ad) {
                    Log.d("awesome","Floating view banner ad impression: "+ad);
                }
            });
        }catch (Exception ignored){}
    }

    private void loadInterstitialAd() {
        try{
            interstitialAd = new InterstitialAd(this, "872401172921626_872713092890434");
            interstitialAd.setAdListener(new InterstitialAdListener() {
                @Override
                public void onInterstitialDisplayed(Ad ad) {
                    Log.d("awesome","Floating View Interstitial ad displayed: "+ad);
                }

                @Override
                public void onInterstitialDismissed(Ad ad) {
                    Log.d("awesome","Floating view Interstitial ad dismissed: "+ad);
                }

                @Override
                public void onError(Ad ad, AdError adError) {
                    Log.d("awesome","Error in loading Interstitial ad: "+adError.getErrorMessage());
                    if(adError.getErrorCode()==AdError.NO_FILL_ERROR_CODE){
                        Handler handler=new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                loadInterstitialAd();
                            }
                        },30000);
                    }
                }

                @Override
                public void onAdLoaded(Ad ad) {
                    Log.d("awesome","Floating view interstitial ad loaded: "+ad);
                    interstitialAd.show();
                }

                @Override
                public void onAdClicked(Ad ad) {
                    Log.d("awesome","Floating view interstitial ad clicked: "+ad);
                }

                @Override
                public void onLoggingImpression(Ad ad) {
                    Log.d("awesome","Floating view interstitial logging impression: "+ad);
                }
            });
            interstitialAd.loadAd();
        }catch (Exception ignored){}

    }

    @Override
    public void onDestroy() {
        Log.d("FAS","Service destroyed");
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
        if (frameLayout != null) mWindowManager.removeView(frameLayout);
        try{
            this.mWakeLock.release();
        }catch (Exception e){
            Log.d("awesome","Exception in releasing wakeLock service: "+e.toString());
        }
        try{
            notificationManager.cancel(1);
        }catch (Exception e){
            Log.d("awesome","Exception in closing 1st notification: "+e);
        }

        try{
            notificationManager.cancel(2);
            stopSelf();
        }catch (Exception e){
            Log.d("awesome","Exception in closing 2nd notification: "+e.toString());
        }
    }


    private class MyWebViewClient extends WebViewClient{
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return false;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d("awesome","requested url: "+url);
            /*if (url.startsWith("rtsp")) {
                String html="<!DOCTYPE html>\n" +
                        "<html>\n" +
                        "<body>\n" +
                        "<video width=\"100%\" height=\"100%\" controls>\n"+
                        "<source src=\""+url+" type=\"video/3gpp\">\n"+
                        "</video>";
                Log.d("awesome","html: "+html);
                webView.loadData(html,"text/html","UTF-8");
                return true;
            }*/
            return super.shouldOverrideUrlLoading(view, url);
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

    private class InsideWebViewClient extends WebViewClient {
        @Override
        // Force links to be opened inside WebView and not in Default Browser
        // Thanks http://stackoverflow.com/a/33681975/1815624
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d("awesome","Loading url: "+url);
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onLoadResource(WebView view, String url) {
            try{
                if(webView.getScrollY()!=180){
                    if(webView.getUrl().contains("watch?v")&&minimized){
                        webView.scrollTo(0,180);
                    }
                }
            }catch (Exception e){
                Log.d("awesome","Exception in loading resource: "+e.toString());
            }
            super.onLoadResource(view, url);
        }

    }
}