package com.aziz.driveit.DriveUtils;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.ArrayList;
import java.util.Map;

public interface WebService {

    @GET
    Call<ResponseBody> get(@Url String endpoint);

    @GET
    Call<ResponseBody> get(@Url String endpoint, @HeaderMap Map<String, String> headerMap);

    @GET
    Call<ResponseBody> get(@Url String endpoint,
                           @HeaderMap Map<String, String> headers,
                           @QueryMap Map<String, String> query);

    @POST
    Call<ResponseBody> post(@Url String endpoint);

    @POST
    Call<ResponseBody> post(@Url String endpoint, @HeaderMap Map<String, String> headerMap);

    @Multipart
    @POST
    Call<ResponseBody> post(@Url String endpoint,
                            @HeaderMap Map<String, String> headerMap,
                            @PartMap Map<String, RequestBody> bodyMap);

    @Multipart
    @POST
    Call<ResponseBody> post(@Url String endpoint,
                            @HeaderMap Map<String, String> headerMap,
                            @PartMap Map<String, RequestBody> bodyMap,
                            @Part MultipartBody.Part file);

    @Multipart
    @POST
    Call<ResponseBody> post(@Url String endpoint,
                            @HeaderMap Map<String, String> headerMap,
                            @PartMap Map<String, RequestBody> bodyMap,
                            @Part ArrayList<MultipartBody.Part> files);

    @DELETE
    Call<ResponseBody> delete(@Url String endpoint, @HeaderMap Map<String, String> headerMap);

}
