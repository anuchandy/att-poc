package com.azure.data.appconfiguration;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.http.GET;

public class HttpBinAPIClient {
    //
    public interface HttpBinAPI {
        @GET("anything")
        Call<HttpBinJSON> getAnything();
    }
    //
    private final String baseUrl = "https://httpbin.org";
    private HttpBinAPI api;

    private HttpBinAPIClient() {
        this.api = RetrofitAPIClient.createAPIService(baseUrl, HttpBinAPI.class);
    }

    public static HttpBinAPIClient create() {
        return new HttpBinAPIClient();
    }

    public HttpBinJSON getAnything() {
        try {
            return this.api.getAnything().execute().body();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
