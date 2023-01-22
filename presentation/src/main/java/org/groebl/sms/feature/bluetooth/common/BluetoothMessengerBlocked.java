package org.groebl.sms.feature.bluetooth.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.telephony.PhoneNumberUtils;

import java.util.HashSet;
import java.util.Set;

public class BluetoothMessengerBlocked {

    public static String getBlockPref(Boolean isGroup, String MessengerType) {
        return isGroup ? "bluetooth" + MessengerType + "BlockedGroup" : "bluetooth" + MessengerType + "BlockedContact";
    }

    public static Set<String> getBlockedConversations(SharedPreferences prefs, Boolean isGroup, String MessengerType) {
        return prefs.getStringSet(getBlockPref(isGroup, MessengerType), new HashSet<>());
    }

    public static void setMessengerUnblock(Context context, String address, Boolean isGroup, String MessengerType) {
        Set<String> idStrings = PreferenceManager.getDefaultSharedPreferences(context).getStringSet(getBlockPref(isGroup, MessengerType), new HashSet<>());
        idStrings.remove(address);
        PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet(getBlockPref(isGroup, MessengerType), idStrings).apply();
    }

    public static void setMessengerBlock(Context context, String address, Boolean isGroup, String MessengerType) {
        Set<String> idStrings = PreferenceManager.getDefaultSharedPreferences(context).getStringSet(getBlockPref(isGroup, MessengerType), new HashSet<>());
        idStrings.add(address);
        PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet(getBlockPref(isGroup, MessengerType), idStrings).apply();
    }

    public static boolean isMessengerBlocked(Context context, String name, Boolean isGroup, String MessengerType) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (name.equals(""))                                { return false; }
        if (!isGroup && MessengerType.equals("WhatsApp"))   { name = PhoneNumberUtils.stripSeparators(name); }

        for (String s : getBlockedConversations(prefs, isGroup, MessengerType)) {
            if (s.equalsIgnoreCase(name)) {
                return true;
            }
        }

        return false;
    }

}
