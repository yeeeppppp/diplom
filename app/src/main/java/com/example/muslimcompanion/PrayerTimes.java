package com.example.muslimcompanion;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

final class PrayerTimes {
    static final String FAJR = "Фаджр";
    static final String SUNRISE = "Восход";
    static final String DHUHR = "Зухр";
    static final String ASR = "Аср";
    static final String MAGHRIB = "Магриб";
    static final String ISHA = "Иша";

    private static final double FAJR_ANGLE = 18.0;
    private static final double ISHA_ANGLE = 17.0;
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm", new Locale("ru"));

    private PrayerTimes() {
    }

    static LinkedHashMap<String, Calendar> forDate(Calendar date, double latitude, double longitude) {
        TimeZone zone = date.getTimeZone();
        TIME_FORMAT.setTimeZone(zone);
        double timezone = zone.getOffset(date.getTimeInMillis()) / 3600000.0;
        int dayOfYear = date.get(Calendar.DAY_OF_YEAR);

        double declination = sunDeclination(dayOfYear);
        double equation = equationOfTime(dayOfYear);
        double noon = 12.0 + timezone - longitude / 15.0 - equation / 60.0;

        LinkedHashMap<String, Calendar> times = new LinkedHashMap<>();
        putTime(times, FAJR, date, noon - hourAngle(latitude, declination, 90 + FAJR_ANGLE) / 15.0);
        putTime(times, SUNRISE, date, noon - hourAngle(latitude, declination, 90.833) / 15.0);
        putTime(times, DHUHR, date, noon + 2.0 / 60.0);
        putTime(times, ASR, date, asrTime(noon, latitude, declination));
        putTime(times, MAGHRIB, date, noon + hourAngle(latitude, declination, 90.833) / 15.0);
        putTime(times, ISHA, date, noon + hourAngle(latitude, declination, 90 + ISHA_ANGLE) / 15.0);
        return times;
    }

    static String format(Calendar calendar) {
        synchronized (TIME_FORMAT) {
            return TIME_FORMAT.format(calendar.getTime());
        }
    }

    static Map.Entry<String, Calendar> nextPrayer(LinkedHashMap<String, Calendar> today, double lat, double lon) {
        Calendar now = Calendar.getInstance();
        for (Map.Entry<String, Calendar> entry : today.entrySet()) {
            if (!SUNRISE.equals(entry.getKey()) && entry.getValue().after(now)) {
                return entry;
            }
        }

        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        return forDate(tomorrow, lat, lon).entrySet().iterator().next();
    }

    static Calendar todayAt(Calendar source, int hour, int minute) {
        Calendar copy = (Calendar) source.clone();
        copy.set(Calendar.HOUR_OF_DAY, hour);
        copy.set(Calendar.MINUTE, minute);
        copy.set(Calendar.SECOND, 0);
        copy.set(Calendar.MILLISECOND, 0);
        return copy;
    }

    private static void putTime(LinkedHashMap<String, Calendar> map, String name, Calendar base, double decimalHour) {
        double normalized = normalizeHour(decimalHour);
        int hour = (int) Math.floor(normalized);
        int minute = (int) Math.round((normalized - hour) * 60.0);
        if (minute == 60) {
            hour++;
            minute = 0;
        }
        map.put(name, todayAt(base, hour % 24, minute));
    }

    private static double asrTime(double noon, double latitude, double declination) {
        double angle = Math.toDegrees(Math.atan(1.0 / (1.0 + Math.tan(Math.toRadians(Math.abs(latitude - declination))))));
        return noon + hourAngle(latitude, declination, 90 - angle) / 15.0;
    }

    private static double sunDeclination(int dayOfYear) {
        double gamma = 2.0 * Math.PI / 365.0 * (dayOfYear - 1);
        return Math.toDegrees(
                0.006918
                        - 0.399912 * Math.cos(gamma)
                        + 0.070257 * Math.sin(gamma)
                        - 0.006758 * Math.cos(2 * gamma)
                        + 0.000907 * Math.sin(2 * gamma)
                        - 0.002697 * Math.cos(3 * gamma)
                        + 0.00148 * Math.sin(3 * gamma));
    }

    private static double equationOfTime(int dayOfYear) {
        double gamma = 2.0 * Math.PI / 365.0 * (dayOfYear - 1);
        return 229.18 * (0.000075
                + 0.001868 * Math.cos(gamma)
                - 0.032077 * Math.sin(gamma)
                - 0.014615 * Math.cos(2 * gamma)
                - 0.040849 * Math.sin(2 * gamma));
    }

    private static double hourAngle(double latitude, double declination, double zenith) {
        double latRad = Math.toRadians(latitude);
        double decRad = Math.toRadians(declination);
        double cos = (Math.cos(Math.toRadians(zenith)) - Math.sin(latRad) * Math.sin(decRad))
                / (Math.cos(latRad) * Math.cos(decRad));
        cos = Math.max(-1.0, Math.min(1.0, cos));
        return Math.toDegrees(Math.acos(cos));
    }

    private static double normalizeHour(double hour) {
        double result = hour % 24.0;
        return result < 0 ? result + 24.0 : result;
    }
}
