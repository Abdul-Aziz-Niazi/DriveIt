package com.aziz.drive_it.DriveUtils;

import com.aziz.drive_it.DriveUtils.model.DIFile;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.ArrayList;
import java.util.Map;

interface WebService {

    @GET
    Call<ResponseBody> get(@Url String endpoint);

    @GET
    Call<ResponseBody> get(@Url String endpoint, @HeaderMap Map<String, String> headerMap);

    @GET
    Call<DIFile> getFile(@Url String endpoint, @HeaderMap Map<String, String> headerMap);

    @GET
    Call<ResponseBody> get(@Url String endpoint,
                           @HeaderMap Map<String, String> headers,
                           @QueryMap Map<String, String> query);

    @POST
    Call<ResponseBody> post(@Url String endpoint);

    @POST
    Call<Void> post(@Url String endpoint, @HeaderMap Map<String, String> headerMap, @Body RequestBody body);

    @POST
    Call<ResponseBody> post(@Url String endpoint, @HeaderMap Map<String, String> headerMap);

    @POST
    Call<DIFile> post(@Url String endpoint, @HeaderMap Map<String, String> headerMap, @Body DIFile body);

    @POST
    Call<Void> postResumable(@Url String endpoint, @HeaderMap Map<String, String> headerMap, @Body DIFile body);

    @Multipart
    @POST
    Call<ResponseBody> post(@Url String endpoint,
                            @HeaderMap Map<String, String> headerMap,
                            @PartMap Map<String, RequestBody> bodyMap);


    @PATCH
    Call<DIFile> patch(@Url String endpoint,
                       @HeaderMap Map<String, String> headerMap,
                       @Body RequestBody file);

    @Multipart
    @POST
    Call<ResponseBody> post(@Url String endpoint,
                            @HeaderMap Map<String, String> headerMap,
                            @PartMap Map<String, RequestBody> bodyMap,
                            @Part ArrayList<MultipartBody.Part> files);

    @DELETE
    Call<ResponseBody> delete(@Url String endpoint, @HeaderMap Map<String, String> headerMap);

}
