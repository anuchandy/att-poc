package com.azure.data.azappconfigactivity;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.azure.data.appconfiguration.HttpBinClient;
import com.azure.data.appconfiguration.HttpBinJSON;

import java.util.concurrent.Callable;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button button;
    private TextView textView;
    private HttpBinClient client;
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //
        button = findViewById(R.id.button);
        textView = findViewById(R.id.textView);
        //
        button.setOnClickListener(this);
        //
        client = HttpBinClient.create();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }

    @Override
    public void onClick(View view) {
        DisposableObserver<HttpBinJSON> disposable = Observable.defer(new Callable<ObservableSource<HttpBinJSON>>() {
            @Override
            public ObservableSource<HttpBinJSON> call() throws Exception {
                return Observable.fromCallable(new Callable<HttpBinJSON>() {
                    @Override
                    public HttpBinJSON call() throws Exception {
                        return client.getAnything();
                    }
                });
            }
        })
        .subscribeOn(Schedulers.newThread())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeWith(new DisposableObserver<HttpBinJSON>() {
            @Override
            public void onComplete() {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(HttpBinJSON result) {
                textView.setText(result.url());
            }
        });
        //
        disposables.add(disposable);
    }


}
