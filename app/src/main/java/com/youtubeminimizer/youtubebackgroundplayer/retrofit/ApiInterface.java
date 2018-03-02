package com.youtubeminimizer.youtubebackgroundplayer.retrofit;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;

/**
 * Created by Abhilash on 17-09-2017
 */

public interface ApiInterface {
    @Headers("Content-type:application/json")
    @GET("/version.php")
    Call<VersionResponse> getVersion();
}
