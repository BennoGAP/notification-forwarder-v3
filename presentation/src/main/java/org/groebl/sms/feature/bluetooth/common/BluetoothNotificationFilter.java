package org.groebl.sms.feature.bluetooth.common;

import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.service.notification.StatusBarNotification;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;

import org.groebl.sms.BuildConfig;
import org.groebl.sms.feature.bluetooth.common.BluetoothHelper;
import com.vdurmont.emoji.EmojiParser;

public class BluetoothNotificationFilter {

    private static boolean isPhoneNumber(String name) {
        if (TextUtils.isEmpty(name)) { return false; }

        char c = name.charAt(0);
        return !name.contains("@") && !name.matches(".*[a-zA-Z]+.*") && (c == '+' || c == '(' || Character.isDigit(c));
    }

    private static String removeDirectionChars(String text) {
        return text.replaceAll("[\u202A|\u202B|\u202C|\u200B]", "");
    }


    public static class BT_Filter {

        private String sender = "";
        private String content = "";
        private Integer errorCode = 777;
        private Long sendTime = 0L;


        public String getSender() {
            return this.sender;
        }

        public String getContent() {
            return this.content;
        }

        public Integer getErrorCode() {
            return this.errorCode;
        }

        public Long getSendTime() { return this.sendTime; }

        public Boolean allData() {
            return (!this.sender.equalsIgnoreCase("") && !this.content.equalsIgnoreCase(""));
        }

        public void BluetoothFilter(StatusBarNotification sbn, Context mContext) {

            String set_sender = "";
            String set_content = "";
            String ticker = "";
            String title = "";
            String text = "";
            SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            Bundle extras = sbn.getNotification().extras;
            String pack = sbn.getPackageName();

            if (sbn.getNotification().tickerText != null) {
                ticker = removeDirectionChars(sbn.getNotification().tickerText.toString());
            }

            String titleExtra = extras.containsKey(Notification.EXTRA_TITLE_BIG)
                    ? Notification.EXTRA_TITLE_BIG : Notification.EXTRA_TITLE;
            if (extras.get(titleExtra) != null) {
                title = removeDirectionChars(extras.get(titleExtra).toString());
            }

            if (extras.get(Notification.EXTRA_TEXT) != null) {
                text = removeDirectionChars(extras.get(Notification.EXTRA_TEXT).toString());
            }


            switch(pack) {
                case "org.telegram.messenger":
                    if (ticker.equals("")) {
                        CharSequence[] textline_telegram = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);

                        if (textline_telegram != null) {
                            ticker = textline_telegram[0].toString();
                        } else {
                            return;
                        }
                    }

                    set_sender = "Telegram";
                    set_content = ticker;
                    break;

                case "ch.threema.app":
                    if (ticker.equals("")) {
                        return;
                    }

                    set_sender = "Threema";
                    set_content = ticker;
                    break;

                case "com.skype.raider":
                    if (ticker.equals("")) {
                        return;
                    }

                    if (extras.get(Notification.EXTRA_BIG_TEXT) != null) {
                        ticker = title + ": " + removeDirectionChars(extras.get(Notification.EXTRA_BIG_TEXT).toString());
                    }

                    set_sender = "Skype";
                    set_content = ticker;
                    break;

                case "com.android.email":
                case "com.boxer.email":
                    if (text.equals("")) {
                        return;
                    }

                    if (extras.get(Notification.EXTRA_BIG_TEXT) != null) {
                        String text_long_email = removeDirectionChars(extras.get(Notification.EXTRA_BIG_TEXT).toString());

                        if (!text_long_email.equals(text) && !title.equals("")) {
                            set_sender = "E-Mail";
                            set_content = title + ": " + text_long_email;
                        }
                    }
                    break;

                case "com.google.android.gm":
                    if (title.matches("^[0-9]*\\u00A0.*$")) {
                        return;
                    }

                    if (extras.get(Notification.EXTRA_BIG_TEXT) != null) {
                        text = removeDirectionChars(extras.get(Notification.EXTRA_BIG_TEXT).toString());
                    }

                    set_sender = "E-Mail";
                    if (!title.equals("") && !text.equals("")) {
                        set_content = title + ": " + text;
                    }
                    break;

                case "com.fsck.k9":
                    if (extras.get(Notification.EXTRA_BIG_TEXT) != null) {
                        ticker = title + ": " + removeDirectionChars(extras.get(Notification.EXTRA_BIG_TEXT).toString());
                    }

                    set_sender = "E-Mail";
                    set_content = ticker;
                    break;

                case "com.microsoft.office.outlook":
                    //Newest Msg = Last Item in Line; contains: Sender Subject Text
                    CharSequence[] textline_outlook = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
                    if (textline_outlook != null) {
                        text = textline_outlook[textline_outlook.length - 1].toString();
                    }

                    set_sender = "E-Mail";
                    set_content = text;
                    break;

                case "de.web.mobile.android.mail":
                case "de.gmx.mobile.android.mail":
                case "com.lenovo.email":
                    if (title.equals("")) {
                        return;
                    }

                    set_sender = "E-Mail";

                    CharSequence[] textline_gmx = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
                    if (textline_gmx != null) {
                        set_content = textline_gmx[0].toString();
                    } else if (!title.equals("") && !text.equals("")) {
                        set_content = title + " - " + text;
                    }

                    break;

                case "com.ebay.mobile":
                    set_sender = "eBay";
                    if (!title.equals("") && !text.equals("")) {
                        set_content = title + ": " + text;
                    } else {
                        set_content = ticker;
                    }
                    break;

                case "com.google.android.apps.fireball":
                    set_sender = "Allo";
                    CharSequence[] textline_allo = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);

                    if (textline_allo != null) {
                        set_content = textline_allo[0].toString().replaceFirst("\\s\\s", ": ");
                    } else if (!title.equals(ticker)) {
                        set_content = text.replaceFirst("\\s\\s", " in " + title + ": ");
                    } else {
                        set_content = title + ": " + text;
                    }
                    break;

                case "com.tippingcanoe.mydealz":
                    CharSequence[] textline_mydealz = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
                    if (textline_mydealz != null) {
                        text = textline_mydealz[0].toString();
                    }

                    set_sender = "mydealz";
                    set_content = text;
                    break;

                case "com.whatsapp":
                    if (sbn.getTag() != null) { return; }

                    CharSequence[] textline_whatsapp = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);

                    if (mPrefs.getBoolean("bluetoothWhatsAppToContact", true)) {

                        String WA_grp = "";
                        String WA_name = "";
                        String WA_msg = "";
                        String phoneNumber = "";

                        //Yeah, here happens magic and stuff  ¯\_(ツ)_/¯
                        if (textline_whatsapp == null && !ticker.equals("") && !text.equals("")) {
                            if (ticker.endsWith(" @ " + title) && text.contains(": ")) {
                                WA_grp = title;
                                WA_name = text.substring(0, text.indexOf(": "));
                                WA_msg = text.substring(text.indexOf(": ") + 2, text.length());
                            } else {
                                WA_grp = "";
                                WA_name = title;
                                WA_msg = text;
                            }
                        } else if (title.equals("WhatsApp")) {
                            //Nummer = ticker between 202a und 202c
                            text = removeDirectionChars(textline_whatsapp[textline_whatsapp.length - 1].toString());

                            if (ticker.contains(" @ ") && text.contains(" @ ") && text.contains(": ")) {
                                WA_name = text.substring(0, text.indexOf(" @ "));
                                WA_grp = text.substring(text.indexOf(" @ ") + 3, text.indexOf(": "));
                                WA_msg = text.substring(text.indexOf(": ") + 2, text.length());
                            } else {
                                WA_grp = "";
                                WA_name = text.substring(0, text.indexOf(": "));
                                WA_msg = text.substring(text.indexOf(": ") + 2, text.length());
                            }
                        } else if (textline_whatsapp != null) {
                            text = removeDirectionChars(textline_whatsapp[textline_whatsapp.length - 1].toString());
                            if (ticker.endsWith(" @ " + title)) {
                                WA_grp = title;
                                WA_name = text.substring(0, text.indexOf(": "));
                                WA_msg = text.substring(text.indexOf(": ") + 2, text.length());
                            } else {
                                WA_grp = "";
                                WA_name = title;
                                WA_msg = text;
                            }

                        }

                        //Check if Message is from blocked group
                        if (BluetoothWABlocked.isWABlocked(mPrefs, WA_grp, true)) { return; }

                        //Check if the Name is just a Number or a Name we can search for in the Phonebook
                        if (isPhoneNumber(WA_name)) {
                            set_sender = WA_name;
                            this.errorCode = 778;
                        } else {
                            try {
                                phoneNumber = BluetoothHelper.INSTANCE.findWhatsAppNumberFromName(mContext, WA_name);
                                /*
                                Cursor c = mContext.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                        new String[]{"data1"},
                                        "display_name = ? AND account_type = ?",
                                        new String[]{WA_name, "com.whatsapp"},
                                        null);

                                if (c != null && c.moveToFirst()) {
                                    phoneNumber = c.getString(0);
                                }
                                if (c != null && !c.isClosed()) {
                                    c.close();
                                }
                                */

                                //Check if everything went fine, otherwise back to the roots (╯°□°）╯︵ ┻━┻
                                if (phoneNumber.equals("")) {
                                    set_sender = "WhatsApp";
                                    if (textline_whatsapp == null) {
                                        set_content = title + ": " + text;
                                    } else {
                                        set_content = (title.equals("WhatsApp") ? "" : title + ": ") + textline_whatsapp[textline_whatsapp.length - 1].toString();
                                    }
                                } else {
                                    //Check if Message is from blocked contact
                                    if (BluetoothWABlocked.isWABlocked(mPrefs, phoneNumber, false)) { return; }

                                    set_sender = phoneNumber;
                                    this.errorCode = 778;
                                }
                            } catch (Exception e) {
                                set_sender = "WhatsApp";
                                if (textline_whatsapp == null) {
                                    set_content = title + ": " + text;
                                } else {
                                    set_content = (title.equals("WhatsApp") ? "" : title + ": ") + textline_whatsapp[textline_whatsapp.length - 1].toString();
                                }
                            }
                        }

                        //Check if necessary (see above) // Private Msg or Group-Chat Msg
                        if (set_content.equals("")) {
                            set_content = (WA_grp.equals("") ? WA_msg : EmojiParser.removeAllEmojis(WA_grp) + ": " + WA_msg);
                        }

                        //Set WhatsApp Prefix to Msg
                        if(!set_sender.equals("WhatsApp") && !mPrefs.getBoolean("bluetoothWhatsAppHidePrefix", true)) {
                            set_content = "WhatsApp: " + set_content;
                        }

                    } else {
                        set_sender = "WhatsApp";
                        if (textline_whatsapp == null) {
                            set_content = title + ": " + text;
                        } else {
                            set_content = (title.equals("WhatsApp") ? "" : title + ": ") + textline_whatsapp[textline_whatsapp.length - 1].toString();
                        }
                    }

                    break;

                default:
                    if (!pack.equalsIgnoreCase(BuildConfig.APPLICATION_ID) && !pack.equalsIgnoreCase("android")) {
                        PackageManager pm = mContext.getPackageManager();
                        ApplicationInfo ai;

                        try {
                            ai = pm.getApplicationInfo(pack, 0);
                            set_sender = pm.getApplicationLabel(ai).toString();
                        } catch (PackageManager.NameNotFoundException e) {
                            set_sender = "";
                        }

                        set_content = (ticker.equals("") ? title + ": " + text : ticker);
                    }
            }

            if(this.errorCode.equals(777)) {
                if(mPrefs.getBoolean("bluetoothAppnameToNumber", false)) {
                    set_sender =  "+499876" + PhoneNumberUtils.stripSeparators(PhoneNumberUtils.convertKeypadLettersToDigits(set_sender.toString()));
                }
                else if (!mPrefs.getBoolean("bluetoothAppnameAsText", false)) {
                    set_content = set_sender + ": " + set_content;
                    set_sender = "+49987654321";
                }
            }

            this.sender = set_sender.substring(0, Math.min(set_sender.length(), 49));
            this.content = set_content.substring(0, Math.min(set_content.length(), 999));
            this.sendTime = System.currentTimeMillis();
        }
    }
}
