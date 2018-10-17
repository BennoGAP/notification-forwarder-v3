package org.groebl.sms.feature.bluetooth.common;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class BluetoothDatabase {

    private static final String DATABASE = "ForwardMessages.db";
    private static final String TABLE = "messages";


    public static void init(Context context) {

        try (SQLiteDatabase db = context.openOrCreateDatabase(DATABASE, Context.MODE_PRIVATE, null)) {
            final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE + " (app TEXT(70) NOT NULL, hash TEXT(32) NOT NULL, time DATE DEFAULT (datetime('now', 'localtime')) );";
            db.execSQL(CREATE_TABLE);
        } catch (SQLiteException e) {
            //catch
        }

    }

    public static void deleteAll(Context context) {

        try (SQLiteDatabase db = context.openOrCreateDatabase(DATABASE, Context.MODE_PRIVATE, null)) {
            db.delete(TABLE, null, null);
        } catch (SQLiteException e) {
            //catch
        }

    }

    public static void deleteBluetoothDbData(Context context, Boolean afterTime) {

        try (SQLiteDatabase db = context.openOrCreateDatabase(DATABASE, Context.MODE_PRIVATE, null)) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(System.currentTimeMillis());
            c.add(Calendar.HOUR_OF_DAY, afterTime ? -6 : 0);

            db.execSQL("DELETE from " + TABLE + " WHERE time <= " + "'" + dateFormat.format(c.getTime()) + "';");
        } catch (SQLiteException e) {
            //catch
        }
    }

    public static boolean searchBluetoothNotificationHash(Context context, String app, String hash) {

        new Thread(() -> deleteBluetoothDbData(context, true)).start();

        //Ignore messages from notification-test-app
        if(app.equalsIgnoreCase("doc.saulmm.notification")) { return false; }

        try (SQLiteDatabase db = context.openOrCreateDatabase(DATABASE, Context.MODE_PRIVATE, null)) {
            //Check if hash is in Database
            try (Cursor c = db.rawQuery("SELECT * FROM " + TABLE + " WHERE app = ? AND hash = ?", new String[]{app, hash})) {

                if (c.getCount() > 0) {
                    //In Database, return true
                    return true;
                } else {
                    //Not in Database, so write it in DB and return false
                    ContentValues values = new ContentValues();
                    values.put("app", app);
                    values.put("hash", hash);
                    db.insertOrThrow(TABLE, null, values);

                    return false;
                }
            } catch (SQLiteException e) {
                return false;
            }

        } catch (SQLiteException e) {
            return false;
        }

    }

}