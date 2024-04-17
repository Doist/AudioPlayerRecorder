package com.heavyplayer.audioplayerrecorder.service.manager;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.heavyplayer.audioplayerrecorder.BuildConfig;

import java.util.List;

public class ServiceManager implements ServiceConnection {
    private IBinder mBinder;

    private Activity mActivity;
    private Class<?> mServiceClass;

    private StateListener mStateListener;

    public <T extends Service> ServiceManager(Activity activity, Class<T> serviceClass) {
        mActivity = activity;
        mServiceClass = serviceClass;

        startService();
    }

    final public void onStart() {
        onActivateService();
    }

    final public void onStop() {
        onDeactivateService(!mActivity.isChangingConfigurations());
    }

    protected void onActivateService() {
        bindService();

        ActivityManager activityManager = (ActivityManager) mActivity.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = activityManager.getRunningAppProcesses();
        if (runningAppProcesses != null) {
            int importance = runningAppProcesses.get(0).importance;
            if (importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                // Only start the service if we are actually in the foreground.
                // https://issuetracker.google.com/issues/110237673
                startService();
            }
        }
    }

    protected void onDeactivateService(boolean stopService) {
        if (stopService) {
            stopService();
        }

        unbindService();
    }

    protected void startService() {
        mActivity.startService(new Intent(mActivity, mServiceClass));
    }

    protected void stopService() {
        if (mStateListener != null) {
            mStateListener.onServiceStop();
        }

        mActivity.stopService(new Intent(mActivity, mServiceClass));
    }

    protected void bindService() {
        mActivity.bindService(
                new Intent(mActivity, mServiceClass),
                this,
                Context.BIND_AUTO_CREATE);
    }

    protected void unbindService() {
        if (mBinder != null) {
            if (mStateListener != null) {
                mStateListener.onServiceUnbind(mBinder);
            }

            mBinder = null;
        }

        mActivity.unbindService(this);
    }

    public IBinder getBinder() {
        return mBinder;
    }

    @Override
    final public void onServiceConnected(ComponentName name, IBinder service) {
        mBinder = service;

        if (mStateListener != null) {
            mStateListener.onServiceBind(mBinder);
        }

        if (BuildConfig.DEBUG) {
            Log.i(mServiceClass.getSimpleName(), "Local service connected");
        }
    }

    @Override
    final public void onServiceDisconnected(ComponentName name) {
        mBinder = null;
        if (mStateListener != null) {
            mStateListener.onServiceUnbind(null);
        }

        if (BuildConfig.DEBUG) {
            Log.i(mServiceClass.getSimpleName(), "Local service disconnected");
        }
    }

    public void setServiceStateListener(StateListener listener) {
        mStateListener = listener;
    }

    public interface StateListener {
        void onServiceBind(IBinder binder);

        /**
         * @param binder may be null if the service disconnects.
         */
        void onServiceUnbind(IBinder binder);

        void onServiceStop();
    }
}
