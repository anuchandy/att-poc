package com.azure.data.appconfiguration;

import com.azure.core.implementation.RetrofitAPIClient;

import java.io.IOException;

public class HttpBinClient {
    //
    private static final String DEFAULT_BASE_URL = "https://httpbin.org";
    private HttpBinService api;

    private HttpBinClient(String baseUrl) {
        this.api = RetrofitAPIClient.createAPIService(baseUrl, HttpBinService.class);
    }

    public static HttpBinClient create() {
        return create(DEFAULT_BASE_URL);
    }

    public static HttpBinClient create(String baseUrl) {
        return new HttpBinClient(baseUrl);
    }

    public HttpBinJSON getAnything() {
        try {
            return this.api.getAnything().execute().body();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}