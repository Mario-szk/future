package com.jnhyxx.html5.net;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.android.volley.NetworkError;
import com.android.volley.ParseError;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.jnhyxx.html5.App;
import com.jnhyxx.html5.R;
import com.jnhyxx.html5.activity.BaseActivity;
import com.jnhyxx.html5.domain.local.LocalUser;
import com.jnhyxx.html5.utils.ToastUtil;
import com.johnz.kutils.net.ApiCallback;
import com.johnz.kutils.net.NullResponseError;

public abstract class Callback<T> extends ApiCallback<T> {

    @Override
    public void onSuccess(T t) {
        Log.d("VolleyHttp", getUrl() + " onSuccess: " + t.toString());

        if (t instanceof Resp) {
            if (((Resp) t).isTokenExpired()) {
                sendTokenExpiredBroadcast(((Resp) t).getMsg());
            } else {
                onReceive(t);
            }
        } else if (t instanceof String) {
            if (((String) t).indexOf("code") != -1) {
                try {
                    Resp resp = new Gson().fromJson((String) t, Resp.class);
                    if (resp.isTokenExpired()) {
                        sendTokenExpiredBroadcast(resp.getMsg());
                    }
                } catch (JsonSyntaxException e) {
                    onReceive(t);
                }
            } else {
                onReceive(t);
            }
        }
    }

    private void sendTokenExpiredBroadcast(String msg) {
        Intent intent = new Intent(BaseActivity.ACTION_TOKEN_EXPIRED);
        intent.putExtra(BaseActivity.EX_TOKEN_EXPIRED_MESSAGE, msg);
        LocalBroadcastManager.getInstance(App.getAppContext()).sendBroadcast(intent);
        //如果是未登录状态，将缓存清理掉.
        LocalUser.getUser().logout();
    }

    @Override
    public void onFailure(VolleyError volleyError) {
        Log.d("VolleyHttp", getUrl() + " " + volleyError.toString());
        int toastResId = R.string.api_error_network;
        if (volleyError instanceof NullResponseError) {
            toastResId = R.string.api_error_null;
        } else if (volleyError instanceof TimeoutError) {
            toastResId = R.string.api_error_timeout;
        } else if (volleyError instanceof ParseError) {
            toastResId = R.string.api_error_parse;
        } else if (volleyError instanceof NetworkError) {
            toastResId = R.string.api_error_network;
        } else if (volleyError instanceof ServerError) {
            toastResId = R.string.api_error_server;
        }
        ToastUtil.show(toastResId);
    }

    public abstract void onReceive(T t);
}
