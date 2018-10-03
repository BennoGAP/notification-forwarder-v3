package org.groebl.sms.feature.bluetooth.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.telephony.PhoneNumberUtils;

import java.util.HashSet;
import java.util.Set;

public class BluetoothWABlocked {

    public static String getWABlockPref(Boolean isGroup) {
        return isGroup ? "bluetoothWhatsAppBlockedGroup" : "bluetoothWhatsAppBlockedContact";
    }

    public static Set<String> getWABlockedConversations(SharedPreferences prefs, Boolean isGroup) {
        return prefs.getStringSet(getWABlockPref(isGroup), new HashSet<>());
    }

    public static void setWAUnblock(Context context, String address, Boolean isGroup) {
        Set<String> idStrings = PreferenceManager.getDefaultSharedPreferences(context).getStringSet(getWABlockPref(isGroup), new HashSet<>());
        idStrings.remove(address);
        PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet(getWABlockPref(isGroup), idStrings).apply();
    }

    public static void setWABlock(Context context, String address, Boolean isGroup) {
        Set<String> idStrings = PreferenceManager.getDefaultSharedPreferences(context).getStringSet(getWABlockPref(isGroup), new HashSet<>());
        idStrings.add(address);
        PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet(getWABlockPref(isGroup), idStrings).apply();
    }

    public static boolean isWABlocked(SharedPreferences prefs, String name, Boolean isGroup) {
        if (name.equals(""))    { return false; }
        if (!isGroup)           { name = PhoneNumberUtils.stripSeparators(name); }

        for (String s : getWABlockedConversations(prefs, isGroup)) {
            if (s.equalsIgnoreCase(name)) {
                return true;
            }
        }

        return false;
    }

}
