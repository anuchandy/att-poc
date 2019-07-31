package com.azure.core.implementation;

import com.azure.core.implementation.serializer.SerializerAdapter;
import com.azure.core.implementation.serializer.SerializerEncoding;
import com.azure.core.implementation.serializer.jackson.JacksonAdapter;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;

public class RetrofitAPIClient {
    private static Retrofit retrofit = null;

    private static Retrofit getClient(String baseUri, SerializerAdapter serializerAdapter) {
        //
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.level(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();
        //
        retrofit = new Retrofit.Builder()
                .baseUrl(baseUri)
                .addConverterFactory(serializerAdapter.retrofitConverterFactory(SerializerEncoding.JSON))
                .client(client)
                .build();
        //
        return retrofit;
    }

    public static <T> T createAPIService(String baseUri, SerializerAdapter serializerAdapter, Class<T> service) {
        return getClient(baseUri, serializerAdapter).create(service);
    }


    public static <T> T createAPIService(String baseUri, Class<T> service) {
        return getClient(baseUri, JacksonAdapter.createDefaultSerializerAdapter()).create(service);
    }
}
