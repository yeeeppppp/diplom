package com.example.muslimcompanion;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

final class ReminderScheduler {
    private static final String PREFS = "muslim_companion";
    private static final String KEY_REMINDERS = "prayer_reminders";
    private static final String KEY_RAMADAN = "ramadan_reminders";
    private static final int RAMADAN_SUHOOR_ID = 9001;
    private static final int RAMADAN_IFTAR_ID = 9002;

    private ReminderScheduler() {
    }

    static void refresh(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        double latitude = Double.longBitsToDouble(prefs.getLong("latitude", Double.doubleToLongBits(55.7558)));
        double longitude = Double.longBitsToDouble(prefs.getLong("longitude", Double.doubleToLongBits(37.6173)));
        boolean prayerReminders = prefs.getBoolean(KEY_REMINDERS, false);
        boolean ramadanReminders = prefs.getBoolean(KEY_RAMADAN, false);

        Calendar today = Calendar.getInstance();
        LinkedHashMap<String, Calendar> times = PrayerTimes.forDate(today, latitude, longitude);

        for (Map.Entry<String, Calendar> entry : times.entrySet()) {
            if (PrayerTimes.SUNRISE.equals(entry.getKey())) {
                continue;
            }
            int id = Math.abs(entry.getKey().hashCode());
            if (prayerReminders) {
                Calendar fireAt = nextOccurrence(entry.getValue(), latitude, longitude, entry.getKey());
                schedule(context, id, fireAt, entry.getKey(), "Время молитвы " + entry.getKey());
            } else {
                cancel(context, id);
            }
        }

        if (ramadanReminders) {
            Calendar suhoor = (Calendar) times.get(PrayerTimes.FAJR).clone();
            suhoor.add(Calendar.MINUTE, -20);
            Calendar iftar = times.get(PrayerTimes.MAGHRIB);
            if (suhoor.before(Calendar.getInstance())) {
                suhoor.add(Calendar.DAY_OF_YEAR, 1);
            }
            if (iftar.before(Calendar.getInstance())) {
                iftar = (Calendar) iftar.clone();
                iftar.add(Calendar.DAY_OF_YEAR, 1);
            }
            schedule(context, RAMADAN_SUHOOR_ID, suhoor, "Сухур", "Скоро Фаджр. Завершите сухур.");
            schedule(context, RAMADAN_IFTAR_ID, iftar, "Ифтар", "Наступил Магриб. Время ифтара.");
        } else {
            cancel(context, RAMADAN_SUHOOR_ID);
            cancel(context, RAMADAN_IFTAR_ID);
        }
    }

    private static Calendar nextOccurrence(Calendar candidate, double lat, double lon, String prayerName) {
        Calendar now = Calendar.getInstance();
        if (candidate.after(now)) {
            return candidate;
        }
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        return PrayerTimes.forDate(tomorrow, lat, lon).get(prayerName);
    }

    private static void schedule(Context context, int id, Calendar fireAt, String title, String message) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        PendingIntent intent = pendingIntent(context, id, title, message);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, fireAt.getTimeInMillis(), intent);
            return;
        }
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt.getTimeInMillis(), intent);
    }

    private static void cancel(Context context, int id) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent(context, id, "", ""));
        }
    }

    private static PendingIntent pendingIntent(Context context, int id, String title, String message) {
        Intent intent = new Intent(context, PrayerAlarmReceiver.class);
        intent.putExtra("title", title);
        intent.putExtra("message", message);
        return PendingIntent.getBroadcast(
                context,
                id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
