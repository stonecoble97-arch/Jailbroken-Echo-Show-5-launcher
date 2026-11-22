package com.echoshow.launcher;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Normalised weather data shared by the home and weather screens. */
public class WeatherData {
    public int temp = 72;
    public String condition = "Partly cloudy";
    public int code = 2;
    public boolean isDay = true;
    public int high = 75;
    public int low = 54;
    public int rain = 40;
    public String[] days = {"Fri", "Sat", "Sun", "Mon", "Tue"};
    public int[] dayCodes = {2, 0, 2, 3, 3};
    public int[] dayHighs = {73, 81, 82, 75, 61};
    public int[] dayLows = {50, 50, 54, 57, 52};
    public int[] dayRains = {0, 0, 0, 50, 60};
    public long fetchedAt = 0;

    private static final String PREFS = "echo_weather";
    private static final String KEY = "json";

    static WeatherData fromOpenMeteo(String json) throws Exception {
        WeatherData d = new WeatherData();
        JSONObject o = new JSONObject(json);
        JSONObject cur = o.getJSONObject("current");
        d.temp = (int) Math.round(cur.getDouble("temperature_2m"));
        d.code = cur.getInt("weather_code");
        d.isDay = cur.getInt("is_day") == 1;
        d.condition = condition(d.code);

        JSONObject daily = o.getJSONObject("daily");
        JSONArray codes = daily.getJSONArray("weather_code");
        JSONArray tmax = daily.getJSONArray("temperature_2m_max");
        JSONArray tmin = daily.getJSONArray("temperature_2m_min");
        JSONArray rains = daily.getJSONArray("precipitation_probability_max");
        JSONArray times = daily.getJSONArray("time");
        if (codes.length() > 0) {
            d.high = (int) Math.round(tmax.getDouble(0));
            d.low = (int) Math.round(tmin.getDouble(0));
            d.rain = rains.isNull(0) ? 0 : rains.getInt(0);
        }
        int n = Math.max(0, Math.min(5, codes.length() - 1));
        d.days = new String[n];
        d.dayCodes = new int[n];
        d.dayHighs = new int[n];
        d.dayLows = new int[n];
        d.dayRains = new int[n];
        for (int i = 0; i < n; i++) {
            int idx = i + 1;
            d.days[i] = dayLabel(times.getString(idx));
            d.dayCodes[i] = codes.getInt(idx);
            d.dayHighs[i] = (int) Math.round(tmax.getDouble(idx));
            d.dayLows[i] = (int) Math.round(tmin.getDouble(idx));
            d.dayRains[i] = rains.isNull(idx) ? 0 : rains.getInt(idx);
        }
        return d;
    }

    static WeatherData fromWttr(String json) throws Exception {
        WeatherData d = new WeatherData();
        JSONObject o = new JSONObject(json);
        JSONObject cur = o.getJSONArray("current_condition").getJSONObject(0);
        d.temp = Integer.parseInt(cur.getString("temp_F"));
        d.code = Integer.parseInt(cur.getString("weatherCode"));
        d.isDay = true;
        d.condition = condition(d.code);

        JSONArray days = o.getJSONArray("weather");
        if (days.length() > 0) {
            JSONObject today = days.getJSONObject(0);
            d.high = Integer.parseInt(today.getString("maxtempF"));
            d.low = Integer.parseInt(today.getString("mintempF"));
            d.rain = maxRain(today.getJSONArray("hourly"));
        }
        int n = Math.max(0, Math.min(5, days.length()));
        d.days = new String[n];
        d.dayCodes = new int[n];
        d.dayHighs = new int[n];
        d.dayLows = new int[n];
        d.dayRains = new int[n];
        for (int i = 0; i < n; i++) {
            JSONObject day = days.getJSONObject(i);
            d.days[i] = dayLabel(day.getString("date"));
            JSONArray hours = day.getJSONArray("hourly");
            int mid = Math.min(4, hours.length() - 1);
            d.dayCodes[i] = Integer.parseInt(hours.getJSONObject(Math.max(0, mid))
                    .getString("weatherCode"));
            d.dayHighs[i] = Integer.parseInt(day.getString("maxtempF"));
            d.dayLows[i] = Integer.parseInt(day.getString("mintempF"));
            d.dayRains[i] = maxRain(hours);
        }
        return d;
    }

    private static int maxRain(JSONArray hours) {
        int max = 0;
        for (int i = 0; i < hours.length(); i++) {
            try {
                int v = Integer.parseInt(hours.getJSONObject(i).getString("chanceofrain"));
                if (v > max) max = v;
            } catch (Exception ignored) { }
        }
        return max;
    }

    private static String dayLabel(String iso) {
        try {
            Date dt = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(iso);
            return new SimpleDateFormat("EEE", Locale.US).format(dt);
        } catch (Exception e) {
            return iso;
        }
    }

    public static String condition(int code) {
        if (code == 0) return "Clear sky";
        if (code == 1) return "Mainly clear";
        if (code == 2) return "Partly cloudy";
        if (code == 3) return "Overcast";
        if (code == 45 || code == 48) return "Fog";
        if (code >= 51 && code <= 57) return "Drizzle";
        if (code >= 61 && code <= 67) return "Rain";
        if (code >= 71 && code <= 77) return "Snow";
        if (code >= 80 && code <= 82) return "Rain showers";
        if (code >= 85 && code <= 86) return "Snow showers";
        if (code >= 95) return "Thunderstorm";
        return "Cloudy";
    }

    static void save(Context c, WeatherData d) throws Exception {
        JSONObject o = new JSONObject();
        o.put("temp", d.temp);
        o.put("condition", d.condition);
        o.put("code", d.code);
        o.put("isDay", d.isDay);
        o.put("high", d.high);
        o.put("low", d.low);
        o.put("rain", d.rain);
        o.put("days", new JSONArray(d.days));
        o.put("dayCodes", new JSONArray(d.dayCodes));
        o.put("dayHighs", new JSONArray(d.dayHighs));
        o.put("dayLows", new JSONArray(d.dayLows));
        o.put("dayRains", new JSONArray(d.dayRains));
        o.put("fetchedAt", d.fetchedAt);
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY, o.toString()).commit();
    }

    public static WeatherData load(Context c) {
        String raw = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY, null);
        if (raw == null) return new WeatherData();
        try {
            JSONObject o = new JSONObject(raw);
            WeatherData d = new WeatherData();
            d.temp = o.getInt("temp");
            d.condition = o.getString("condition");
            d.code = o.getInt("code");
            d.isDay = o.getBoolean("isDay");
            d.high = o.getInt("high");
            d.low = o.getInt("low");
            d.rain = o.getInt("rain");
            d.fetchedAt = o.getLong("fetchedAt");
            JSONArray days = o.getJSONArray("days");
            d.days = new String[days.length()];
            for (int i = 0; i < days.length(); i++) d.days[i] = days.getString(i);
            d.dayCodes = intArray(o.getJSONArray("dayCodes"));
            d.dayHighs = intArray(o.getJSONArray("dayHighs"));
            d.dayLows = intArray(o.getJSONArray("dayLows"));
            d.dayRains = intArray(o.getJSONArray("dayRains"));
            return d;
        } catch (Exception e) {
            return new WeatherData();
        }
    }

    private static int[] intArray(JSONArray a) {
        int[] out = new int[a.length()];
        for (int i = 0; i < a.length(); i++) out[i] = a.optInt(i, 0);
        return out;
    }
}
