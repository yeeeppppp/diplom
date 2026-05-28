package com.example.muslimcompanion;

import android.Manifest;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private static final String PREFS = "muslim_companion";
    private static final int GREEN = Color.rgb(14, 111, 92);
    private static final int INK = Color.rgb(31, 43, 41);
    private static final int MUTED = Color.rgb(91, 101, 98);
    private static final int SURFACE = Color.rgb(246, 247, 242);
    private static final int PANEL = Color.WHITE;

    private SharedPreferences prefs;
    private LinearLayout content;
    private Button prayerTab;
    private Button dhikrTab;
    private Button ramadanTab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        requestNotificationPermission();
        buildShell();
        showPrayerScreen();
    }

    private void buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(SURFACE);
        root.setPadding(dp(16), dp(18), dp(16), dp(12));

        TextView title = label("Мой намаз", 28, Typeface.BOLD, INK);
        root.addView(title);
        root.addView(label("Молитвы, зикр и Рамадан без регистрации", 14, Typeface.NORMAL, MUTED));
        root.addView(space(14));

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        prayerTab = tab("Молитвы");
        dhikrTab = tab("Зикр");
        ramadanTab = tab("Рамадан");
        tabs.addView(prayerTab);
        tabs.addView(dhikrTab);
        tabs.addView(ramadanTab);
        root.addView(tabs);

        ScrollView scrollView = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp(14), 0, dp(16));
        scrollView.addView(content);
        root.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private void showPrayerScreen() {
        selectTab(prayerTab);
        content.removeAllViews();

        double latitude = readDouble("latitude", 55.7558);
        double longitude = readDouble("longitude", 37.6173);
        LinkedHashMap<String, Calendar> times = PrayerTimes.forDate(Calendar.getInstance(), latitude, longitude);
        Map.Entry<String, Calendar> next = PrayerTimes.nextPrayer(times, latitude, longitude);

        content.addView(card("Следующая молитва", next.getKey() + " в " + PrayerTimes.format(next.getValue())));
        content.addView(sectionTitle("Расписание на сегодня"));
        for (Map.Entry<String, Calendar> entry : times.entrySet()) {
            content.addView(row(entry.getKey(), PrayerTimes.format(entry.getValue())));
        }

        Switch reminders = new Switch(this);
        reminders.setText("Напоминать о молитвах");
        reminders.setTextSize(16);
        reminders.setTextColor(INK);
        reminders.setChecked(prefs.getBoolean("prayer_reminders", false));
        reminders.setOnCheckedChangeListener((buttonView, checked) -> {
            prefs.edit().putBoolean("prayer_reminders", checked).apply();
            ReminderScheduler.refresh(this);
        });
        content.addView(wrap(reminders));

        content.addView(sectionTitle("Местоположение"));
        EditText latInput = input(String.format(Locale.US, "%.4f", latitude), "Широта");
        EditText lonInput = input(String.format(Locale.US, "%.4f", longitude), "Долгота");
        content.addView(latInput);
        content.addView(lonInput);
        Button save = action("Сохранить координаты");
        save.setOnClickListener(v -> {
            saveDouble("latitude", parse(latInput.getText().toString(), latitude));
            saveDouble("longitude", parse(lonInput.getText().toString(), longitude));
            ReminderScheduler.refresh(this);
            showPrayerScreen();
        });
        content.addView(save);
        content.addView(note("По умолчанию установлена Москва. Введите координаты своего города для более точного расписания."));
    }

    private void showDhikrScreen() {
        selectTab(dhikrTab);
        content.removeAllViews();
        content.addView(card("Счетчики зикра", "Нажимайте на счетчик после каждого повторения. Значения сохраняются на устройстве."));
        content.addView(counter("subhanallah", "Субханаллах", "Пречист Аллах"));
        content.addView(counter("alhamdulillah", "Альхамдулиллях", "Хвала Аллаху"));
        content.addView(counter("allahuakbar", "Аллаху акбар", "Аллах Велик"));
        content.addView(counter("astaghfirullah", "Астагфируллах", "Прошу прощения у Аллаха"));

        Button reset = secondary("Сбросить все счетчики");
        reset.setOnClickListener(v -> {
            prefs.edit()
                    .putInt("dhikr_subhanallah", 0)
                    .putInt("dhikr_alhamdulillah", 0)
                    .putInt("dhikr_allahuakbar", 0)
                    .putInt("dhikr_astaghfirullah", 0)
                    .apply();
            showDhikrScreen();
        });
        content.addView(reset);
    }

    private void showRamadanScreen() {
        selectTab(ramadanTab);
        content.removeAllViews();

        double latitude = readDouble("latitude", 55.7558);
        double longitude = readDouble("longitude", 37.6173);
        LinkedHashMap<String, Calendar> times = PrayerTimes.forDate(Calendar.getInstance(), latitude, longitude);
        RamadanHelper.HijriDate hijri = RamadanHelper.hijriFromGregorian(Calendar.getInstance());
        String hijriLine = hijri.day + " " + RamadanHelper.monthName(hijri.month) + " " + hijri.year + " г. х.";

        if (hijri.isRamadan()) {
            content.addView(card("Сегодня " + hijri.day + "-й день Рамадана", hijriLine));
        } else {
            content.addView(card("Рамадан", "Сегодня по приблизительному хиджри-календарю: " + hijriLine));
        }

        content.addView(sectionTitle("Пост сегодня"));
        content.addView(row("Сухур завершить до", PrayerTimes.format(times.get(PrayerTimes.FAJR))));
        content.addView(row("Ифтар", PrayerTimes.format(times.get(PrayerTimes.MAGHRIB))));

        Switch ramadan = new Switch(this);
        ramadan.setText("Напоминать о сухуре и ифтаре");
        ramadan.setTextSize(16);
        ramadan.setTextColor(INK);
        ramadan.setChecked(prefs.getBoolean("ramadan_reminders", false));
        ramadan.setOnCheckedChangeListener((CompoundButton buttonView, boolean checked) -> {
            prefs.edit().putBoolean("ramadan_reminders", checked).apply();
            ReminderScheduler.refresh(this);
        });
        content.addView(wrap(ramadan));
        content.addView(note("Даты хиджри являются расчетными. Для религиозных решений сверяйтесь с местной мечетью."));
    }

    private LinearLayout counter(String key, String title, String subtitle) {
        LinearLayout box = panel();
        TextView name = label(title, 20, Typeface.BOLD, INK);
        TextView sub = label(subtitle, 13, Typeface.NORMAL, MUTED);
        TextView count = label(String.valueOf(prefs.getInt("dhikr_" + key, 0)), 44, Typeface.BOLD, GREEN);
        count.setGravity(Gravity.CENTER);

        Button plus = action("+1");
        plus.setTextSize(28);
        plus.setOnClickListener(v -> {
            int next = prefs.getInt("dhikr_" + key, 0) + 1;
            prefs.edit().putInt("dhikr_" + key, next).apply();
            count.setText(String.valueOf(next));
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        });
        Button reset = secondary("Сбросить");
        reset.setOnClickListener(v -> {
            prefs.edit().putInt("dhikr_" + key, 0).apply();
            count.setText("0");
        });

        box.addView(name);
        box.addView(sub);
        box.addView(count);
        box.addView(plus);
        box.addView(reset);
        return box;
    }

    private TextView row(String left, String right) {
        TextView view = label(left + "    " + right, 18, Typeface.NORMAL, INK);
        view.setPadding(dp(14), dp(11), dp(14), dp(11));
        view.setBackgroundColor(PANEL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(1));
        view.setLayoutParams(params);
        return view;
    }

    private LinearLayout card(String title, String body) {
        LinearLayout box = panel();
        box.setBackgroundColor(GREEN);
        box.addView(label(title, 22, Typeface.BOLD, Color.WHITE));
        TextView text = label(body, 15, Typeface.NORMAL, Color.WHITE);
        text.setPadding(0, dp(6), 0, 0);
        box.addView(text);
        return box;
    }

    private LinearLayout panel() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(14), dp(16), dp(14));
        box.setBackgroundColor(PANEL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(12));
        box.setLayoutParams(params);
        return box;
    }

    private View wrap(View child) {
        LinearLayout box = panel();
        box.addView(child);
        return box;
    }

    private TextView sectionTitle(String text) {
        TextView view = label(text, 18, Typeface.BOLD, INK);
        view.setPadding(0, dp(12), 0, dp(8));
        return view;
    }

    private TextView note(String text) {
        TextView view = label(text, 13, Typeface.NORMAL, MUTED);
        view.setPadding(0, dp(10), 0, dp(4));
        return view;
    }

    private Button tab(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setTextColor(INK);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setOnClickListener(v -> {
            if (v == prayerTab) {
                showPrayerScreen();
            } else if (v == dhikrTab) {
                showDhikrScreen();
            } else {
                showRamadanScreen();
            }
        });
        button.setLayoutParams(new LinearLayout.LayoutParams(0, dp(48), 1));
        return button;
    }

    private void selectTab(Button selected) {
        Button[] tabs = {prayerTab, dhikrTab, ramadanTab};
        for (Button tab : tabs) {
            if (tab == null) {
                continue;
            }
            tab.setTextColor(tab == selected ? Color.WHITE : INK);
            tab.setTypeface(Typeface.DEFAULT, tab == selected ? Typeface.BOLD : Typeface.NORMAL);
            tab.setBackgroundColor(tab == selected ? GREEN : Color.TRANSPARENT);
        }
    }

    private Button action(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setTextSize(16);
        button.setBackgroundColor(GREEN);
        button.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(52)));
        return button;
    }

    private Button secondary(String text) {
        Button button = action(text);
        button.setTextColor(GREEN);
        button.setBackgroundColor(Color.rgb(230, 239, 235));
        return button;
    }

    private EditText input(String value, String hint) {
        EditText editText = new EditText(this);
        editText.setText(value);
        editText.setHint(hint);
        editText.setTextSize(16);
        editText.setSingleLine(true);
        editText.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        editText.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(56));
        params.setMargins(0, 0, 0, dp(8));
        editText.setLayoutParams(params);
        return editText;
    }

    private TextView label(String text, int sp, int style, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sp);
        view.setTypeface(Typeface.DEFAULT, style);
        view.setTextColor(color);
        view.setLineSpacing(0, 1.08f);
        return view;
    }

    private Space space(int dp) {
        Space space = new Space(this);
        space.setLayoutParams(new LinearLayout.LayoutParams(1, dp(dp)));
        return space;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private double readDouble(String key, double fallback) {
        return Double.longBitsToDouble(prefs.getLong(key, Double.doubleToLongBits(fallback)));
    }

    private void saveDouble(String key, double value) {
        prefs.edit().putLong(key, Double.doubleToLongBits(value)).apply();
    }

    private double parse(String value, double fallback) {
        try {
            return Double.parseDouble(value.trim().replace(',', '.'));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 10);
        }
    }
}
