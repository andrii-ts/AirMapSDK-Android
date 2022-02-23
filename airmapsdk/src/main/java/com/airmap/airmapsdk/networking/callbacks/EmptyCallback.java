package com.airmap.airmapsdk.networking.callbacks;

import android.os.Handler;
import android.os.Looper;

import com.airmap.airmapsdk.AirMapException;
import com.airmap.airmapsdk.models.AirMapBaseModel;
import com.airmap.airmapsdk.util.Utils;

import okhttp3.Call;
import okhttp3.Response;

public class EmptyCallback extends GenericBaseOkHttpCallback {


    public EmptyCallback(AirMapCallback listener) {
        super(listener, null);
    }

    @Override
    public void onResponse(Call call, Response response) {
        if (listener == null) {
            return; //Don't need to do anything if no listener was provided
        }

        if(response.isSuccessful()){
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    listener.onSuccess(null);
                }
            });
        } else {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    failed(new Exception());
                }
            });
        }
    }
}
