package com.aziz.drive_it.DriveUtils;

public interface DICallBack<T> {
    void success(T file);

    void failure(String error);
}
