package com.example.muslimcompanion;

import java.util.Calendar;

final class RamadanHelper {
    private RamadanHelper() {
    }

    static HijriDate hijriFromGregorian(Calendar calendar) {
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH) + 1;
        int year = calendar.get(Calendar.YEAR);

        int[] adjusted = adjustGregorian(day, month, year);
        day = adjusted[0];
        month = adjusted[1];
        year = adjusted[2];

        int a = (14 - month) / 12;
        int y = year + 4800 - a;
        int m = month + 12 * a - 3;
        long julian = day + (153L * m + 2) / 5 + 365L * y + y / 4 - y / 100 + y / 400 - 32045;

        long islamic = julian - 1948440 + 10632;
        long n = (islamic - 1) / 10631;
        islamic = islamic - 10631 * n + 354;
        long j = ((10985 - islamic) / 5316) * ((50 * islamic) / 17719)
                + (islamic / 5670) * ((43 * islamic) / 15238);
        islamic = islamic - ((30 - j) / 15) * ((17719 * j) / 50)
                - (j / 16) * ((15238 * j) / 43) + 29;
        int hijriMonth = (int) ((24 * islamic) / 709);
        int hijriDay = (int) (islamic - (709L * hijriMonth) / 24);
        int hijriYear = (int) (30 * n + j - 30);
        return new HijriDate(hijriDay, hijriMonth, hijriYear);
    }

    static String monthName(int month) {
        String[] names = {
                "Мухаррам", "Сафар", "Раби аль-авваль", "Раби ас-сани",
                "Джумада аль-уля", "Джумада ас-сани", "Раджаб", "Шаабан",
                "Рамадан", "Шавваль", "Зуль-каада", "Зуль-хиджа"
        };
        return names[Math.max(1, Math.min(12, month)) - 1];
    }

    private static int[] adjustGregorian(int day, int month, int year) {
        return new int[]{day, month, year};
    }

    static final class HijriDate {
        final int day;
        final int month;
        final int year;

        HijriDate(int day, int month, int year) {
            this.day = day;
            this.month = month;
            this.year = year;
        }

        boolean isRamadan() {
            return month == 9;
        }
    }
}
