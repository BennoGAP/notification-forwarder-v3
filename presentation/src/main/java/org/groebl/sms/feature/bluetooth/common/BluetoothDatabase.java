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
        SQLiteDatabase db = context.openOrCreateDatabase(DATABASE, Context.MODE_PRIVATE, null);

        try {
            final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE + " (app STRING(70) NOT NULL, hash STRING(32) NOT NULL, time DATE DEFAULT (datetime('now', 'localtime')) );";
            db.execSQL(CREATE_TABLE);
        } catch (SQLiteException e) {
            //catch
        }
        finally {
            db.close();
        }

    }

    public static void deleteAll(Context context) {
        SQLiteDatabase db = context.openOrCreateDatabase(DATABASE, Context.MODE_PRIVATE, null);

        try {
            db.delete(TABLE, null, null);
        }
        catch (SQLiteException e) {
            //catch
        }
        finally {
            db.close();
        }

    }

    public static void deleteAfterTime(Context context) {
        SQLiteDatabase db = context.openOrCreateDatabase(DATABASE, Context.MODE_PRIVATE, null);

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(System.currentTimeMillis());
            c.add(Calendar.HOUR_OF_DAY, -12);
            db.execSQL("DELETE from " + TABLE + " WHERE time <= " + "'" + dateFormat.format(c.getTime()) + "';");
        }
        catch (SQLiteException e) {
            //catch
        }
        finally {
            db.close();
        }
    }

    public static boolean searchHash(Context context, String app, String hash) {
        SQLiteDatabase db = context.openOrCreateDatabase(DATABASE, Context.MODE_PRIVATE, null);

        new Thread(() -> {
            deleteAfterTime(context);
        }).start();

        try {
            //Check if hash is in Database
            Cursor c  = db.rawQuery("SELECT * FROM " + TABLE + " WHERE app = ? AND hash = ?", new String[]{app, hash});

            if (c.getCount() > 0) {
                //In Database, return true
                db.close();
                return true;
            } else {
                //Not in Database, so write it in DB and return false
                ContentValues values = new ContentValues();
                values.put("app", app);
                values.put("hash", hash);

                try {
                    db.insertOrThrow(TABLE, null, values);
                } catch (Exception e) {
                    return false;
                } finally {
                    db.close();
                    return false;
                }

            }

        } catch (SQLiteException e) {
            return false;
        }

    }

}