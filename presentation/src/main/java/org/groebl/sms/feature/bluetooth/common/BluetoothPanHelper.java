package org.groebl.sms.feature.bluetooth.common;

import android.bluetooth.BluetoothProfile;
import java.lang.reflect.Method;

public class BluetoothPanHelper {
    private Object mBluetoothPan;

    public BluetoothPanHelper(BluetoothProfile profile) {
        mBluetoothPan = profile;
    }

    public void setBluetoothTethering(boolean setEnable) {
        try {
            Method method = Class.forName("android.bluetooth.BluetoothPan").getDeclaredMethod("setBluetoothTethering", boolean.class);
            method.invoke(mBluetoothPan, setEnable);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}