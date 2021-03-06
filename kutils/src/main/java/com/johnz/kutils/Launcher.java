package com.johnz.kutils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class Launcher {

    public static final String EX_PAYLOAD = "payload";
    public static final String EX_PAYLOAD_1 = "payload1";
    public static final String EX_PAYLOAD_2 = "payload2";

    private static Launcher sInstance;

    private Context mContext;
    private Intent mIntent;

    private Launcher() {
        mIntent = new Intent();
    }

    public static Launcher with(Context context, Class<?> clazz) {
        sInstance = new Launcher();
        sInstance.mContext = new WeakReference<Context>(context).get();
        sInstance.mIntent.setClass(sInstance.mContext, clazz);
        return sInstance;
    }

    public Launcher putExtra(String key, ArrayList<? extends Parcelable> value) {
        mIntent.putParcelableArrayListExtra(key, value);
        return this;
    }

    public Launcher putExtra(String key, Parcelable parcelable) {
        mIntent.putExtra(key, parcelable);
        return this;
    }

    public Launcher putExtra(String key, int value) {
        mIntent.putExtra(key, value);
        return this;
    }

    public Launcher putExtra(String key, double value) {
        mIntent.putExtra(key, value);
        return this;
    }

    public Launcher putExtra(String key, String value) {
        mIntent.putExtra(key, value);
        return this;
    }

    public Launcher putExtra(String key, boolean value) {
        mIntent.putExtra(key, value);
        return this;
    }

    /**
     * @param key
     * @param data
     * @return
     * @deprecated 为了提高速度 使用 putExtra(String key, Parcelable parcelable)
     */
    public Launcher putExtra(String key, Serializable data) {
        mIntent.putExtra(key, data);
        return this;
    }

    public Launcher putExtra(String key, Bundle bundle) {
        mIntent.putExtra(key, bundle);
        return this;
    }

    public Launcher setFlags(int flag) {
        mIntent.setFlags(flag);
        return this;
    }

    public Launcher addFlags(int flag) {
        mIntent.addFlags(flag);
        return this;
    }

    public void execute() {
        if (mContext != null) {
            mContext.startActivity(mIntent);
            mContext = null;
            mIntent = null;
        }
    }

    public void executeForResult(int requestCode) {
        if (mContext != null) {
            if (mContext instanceof Activity) {
                Activity activity = (Activity) mContext;
                activity.startActivityForResult(mIntent, requestCode);
                mContext = null;
                mIntent = null;
            }
        }
    }


}
