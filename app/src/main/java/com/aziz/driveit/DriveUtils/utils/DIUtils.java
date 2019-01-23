package com.aziz.driveit.DriveUtils.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class DIUtils {
    private static Gson gson;

    private static void buildGson() {
        gson = new GsonBuilder().create();
    }

    public static Gson getGson() {
        if (gson == null) {
            buildGson();
        }
        return gson;
    }
}
