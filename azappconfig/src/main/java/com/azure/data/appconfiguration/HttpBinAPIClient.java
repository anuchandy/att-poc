package com.azure.data.appconfiguration;

import com.azure.core.implementation.RetrofitAPIClient;

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
    private static final String DEFAULT_BASE_URL = "https://httpbin.org";
    private HttpBinAPI api;

    private HttpBinAPIClient(String baseUrl) {
        this.api = RetrofitAPIClient.createAPIService(baseUrl, HttpBinAPI.class);
    }

    public static HttpBinAPIClient create() {
        return create(DEFAULT_BASE_URL);
    }

    public static HttpBinAPIClient create(String baseUrl) {
        return new HttpBinAPIClient(baseUrl);
    }

    public HttpBinJSON getAnything() {
        try {
            return this.api.getAnything().execute().body();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
