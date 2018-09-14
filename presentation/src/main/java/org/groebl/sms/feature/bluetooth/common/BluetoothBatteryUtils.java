package org.groebl.sms.feature.bluetooth.common;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import java.util.Arrays;
import java.util.List;


public class BluetoothBatteryUtils {

    public static List<Intent> POWERMANAGER_INTENTS = Arrays.asList(

            new Intent().setComponent(new ComponentName("com.samsung.android.sm", "com.samsung.android.sm.ui.battery.BatteryActivity")),                            //Samsung - <= API 23
            //new Intent().setComponent(new ComponentName("com.samsung.android.sm_cn", "com.samsung.android.sm.ui.battery.BatteryActivity")),                         //Samsung -
            new Intent().setComponent(new ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"))                          //Samsung - => API 24
            //new Intent().setComponent(new ComponentName("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity")),
            //new Intent().setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity")),     //Huawei, Honor - => API 26
            //new Intent().setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")),                  //Huawei, Honor - <= API 25
            //new Intent().setComponent(new ComponentName("com.coloros.oppoguardelf", "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity")),
            //new Intent().setComponent(new ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
            //new Intent().setComponent(new ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.entry.FunctionActivity")).setData(android.net.Uri.parse("mobilemanager://function/entry/AutoStart"))
    );

    public static void startPowerSaverIntent(Context context) {
        Intent start_intent = new Intent(Settings.ACTION_SETTINGS);

        for (Intent intent : POWERMANAGER_INTENTS) {
            if (isCallable(context, intent)) {
                start_intent = intent;
                break;
            }
        }

        context.startActivity(start_intent);

       // if (start_intent.equals(null)) {
       //   start_intent = new Intent(Settings.ACTION_SETTINGS);
            /*
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                try {
                    context.startActivity(intent);
                } catch (ActivityNotFoundException ex) {
                    Toast.makeText(context, "Nothing found", Toast.LENGTH_LONG)
                            .show();
                }
                //startActivity(intent);
            } else {
                Toast.makeText(context, "Nothing found", Toast.LENGTH_LONG)
                        .show();
            }
            */
       // }
    }

    private static boolean isCallable(Context context, Intent intent) {
        List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

//------------------------------------------------

    @TargetApi(Build.VERSION_CODES.M) //
    public static boolean isBatteryOptimizationDisabled(Context context) {
        String packageName = context.getPackageName();
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return pm.isIgnoringBatteryOptimizations(packageName);
    }

    @TargetApi(Build.VERSION_CODES.M) //
    public static Intent getBatteryOptimizationIntent() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        return intent;
    }

    public static boolean isBatteryOptimizationSettingsAvailable() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }
}