package com.aziz.drive_it.DriveUtils;

public interface DICallBack<T> {
    void success(T DIObject);

    void failure(String error);
}
