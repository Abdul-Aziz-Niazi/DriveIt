package com.aziz.driveit.DriveUtils;

public interface DICallBack<T> {
    void success(T file);

    void failure(String error);
}
