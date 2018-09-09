package org.groebl.sms.feature.bluetooth.common;

import android.bluetooth.BluetoothProfile;
import java.lang.reflect.InvocationTargetException;

public class BluetoothPanHelper {
    private BluetoothProfile mBluetoothPan;
    private int profileid;

    public BluetoothPanHelper(BluetoothProfile profile, int profileid) {
        this.mBluetoothPan = profile;
        this.profileid = profileid;
    }

    public BluetoothProfile getProxy() {
        return this.mBluetoothPan;
    }

    public int getProfile() {
        return this.profileid;
    }

    public boolean isTetheringOn() {
        boolean z = false;
        try {
            z = ((Boolean) Class.forName("android.bluetooth.BluetoothPan").getDeclaredMethod("isTetheringOn", new Class[0]).invoke(this.mBluetoothPan, new Object[0])).booleanValue();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e2) {
            e2.printStackTrace();
        } catch (NoSuchMethodException e3) {
            e3.printStackTrace();
        } catch (IllegalAccessException e4) {
            e4.printStackTrace();
        }
        return z;
    }

    public void setBluetoothTethering(boolean enable) {
        try {
            Class.forName("android.bluetooth.BluetoothPan").getDeclaredMethod("setBluetoothTethering", new Class[]{Boolean.TYPE}).invoke(this.mBluetoothPan, new Object[]{Boolean.valueOf(enable)});
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e2) {
            e2.printStackTrace();
        } catch (NoSuchMethodException e3) {
            e3.printStackTrace();
        } catch (IllegalAccessException e4) {
            e4.printStackTrace();
        }
    }
}
