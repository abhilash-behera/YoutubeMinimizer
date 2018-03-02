package com.youtubeminimizer.youtubebackgroundplayer.retrofit;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Created by Abhilash on 09-10-2017
 */

public class VersionResponse implements Serializable {
    @SerializedName("version_android")
    @Expose
    private String version_android;

    public String getVersion_android() {
        return version_android;
    }
}
