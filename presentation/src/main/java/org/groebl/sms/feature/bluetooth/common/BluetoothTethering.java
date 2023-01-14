package org.groebl.sms.feature.bluetooth.common;


import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import timber.log.Timber;


public class BluetoothTethering {

    private Constructor bluetoothPanConstructor;
    private Context context;
    private ConnectivityManager connectivityManager;
    private Method getTetheredIfaces;

    public BluetoothTethering(Context context, ConnectivityManager connectivityManager) {
        this.context = context;
        this.connectivityManager = connectivityManager;
        try {
            Class classBluetoothPan = Class.forName("android.bluetooth.BluetoothPan");
            this.bluetoothPanConstructor = classBluetoothPan.getDeclaredConstructor(Context.class, BluetoothProfile.ServiceListener.class);
            this.bluetoothPanConstructor.setAccessible(true);
        } catch (Exception e) {
            Timber.e(e);
        }

        try {
            this.getTetheredIfaces = this.connectivityManager.getClass().getDeclaredMethod("getTetheredIfaces");
        } catch (Exception e) {
            Timber.e(e);
        }


    }

    public void startTethering() {
        if (this.isTetheringEnabled() || !BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return;
        }

        try {
            this.bluetoothPanConstructor.newInstance(this.context, new BluetoothPanServiceListener(true));
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    public void stopTethering() {
        try {
            this.bluetoothPanConstructor.newInstance(this.context, new BluetoothPanServiceListener(false));
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    public boolean isTetheringEnabled() {
        try {
            String[] tetherInt = (String[]) getTetheredIfaces.invoke(this.connectivityManager);
            for (String inf : tetherInt) {
                if (inf.contains("bt-pan")) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            Timber.e(e);
        }
        return false;
    }

}

class BluetoothPanServiceListener implements BluetoothProfile.ServiceListener {

    private boolean enable;

    BluetoothPanServiceListener(boolean enable){
        this.enable = enable;
    }

    @Override
    public void onServiceConnected(int profile, BluetoothProfile proxy) {
        try {
            proxy.getClass().getMethod("setBluetoothTethering",Boolean.TYPE).invoke(proxy, Boolean.valueOf(this.enable));
        } catch(SecurityException ignored) {
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public void onServiceDisconnected(int profile) {

    }
}