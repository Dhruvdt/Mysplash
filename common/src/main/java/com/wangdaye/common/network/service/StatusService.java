package com.wangdaye.common.network.service;

import com.wangdaye.base.unsplash.Total;
import com.wangdaye.common.network.SchedulerTransformer;
import com.wangdaye.common.network.UrlCollection;
import com.wangdaye.common.network.api.StatusApi;
import com.wangdaye.common.network.interceptor.AuthInterceptor;
import com.wangdaye.common.network.observer.BaseObserver;
import com.wangdaye.common.network.observer.ObserverContainer;

import io.reactivex.disposables.CompositeDisposable;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Status service.
 * */

public class StatusService {

    private StatusApi api;
    private CompositeDisposable compositeDisposable;

    public StatusService(OkHttpClient client,
                         GsonConverterFactory gsonConverterFactory,
                         RxJava2CallAdapterFactory rxJava2CallAdapterFactory,
                         CompositeDisposable disposable) {
        api = new Retrofit.Builder()
                .baseUrl(UrlCollection.UNSPLASH_API_BASE_URL)
                .client(
                        client.newBuilder()
                                .addInterceptor(new AuthInterceptor())
                                .build()
                ).addConverterFactory(gsonConverterFactory)
                .addCallAdapterFactory(rxJava2CallAdapterFactory)
                .build()
                .create((StatusApi.class));
        compositeDisposable = disposable;
    }

    public void requestTotal(BaseObserver<Total> observer) {
        api.getTotal()
                .compose(SchedulerTransformer.create())
                .subscribe(new ObserverContainer<>(compositeDisposable, observer));
    }

    public void cancel() {
        compositeDisposable.clear();
    }
}
