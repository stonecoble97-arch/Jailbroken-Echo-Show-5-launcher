package com.echoshow.launcher;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Fetches live weather: Open-Meteo first, wttr.in as a fallback, with cached location. */
public final class WeatherClient {
    private WeatherClient() { }

    private static final String LOC_PREFS = "echo_weather";
    private static final String LAT = "latitude";
    private static final String LON = "longitude";
    private static final long MIN_INTERVAL = 10L * 60L * 1000L;
    private static long sLastAttempt = 0;

    public static void refresh(final Context c) {
        long now = System.currentTimeMillis();
        if (now - sLastAttempt < MIN_INTERVAL) return;
        sLastAttempt = now;
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    double[] ll = location(c);
                    String url = "https://api.open-meteo.com/v1/forecast"
                            + "?latitude=" + ll[0]
                            + "&longitude=" + ll[1]
                            + "&current=temperature_2m,weather_code,is_day"
                            + "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max"
                            + "&temperature_unit=fahrenheit&timezone=auto&forecast_days=6";
                    WeatherData d = WeatherData.fromOpenMeteo(fetch(url));
                    d.fetchedAt = System.currentTimeMillis();
                    WeatherData.save(c, d);
                    return;
                } catch (Exception ignored) { }

                try {
                    double[] ll = location(c);
                    String url = "https://wttr.in/" + ll[0] + "," + ll[1] + "?format=j1";
                    WeatherData d = WeatherData.fromWttr(fetch(url));
                    d.fetchedAt = System.currentTimeMillis();
                    WeatherData.save(c, d);
                } catch (Exception ignored) { }
            }
        }, "weather-fetch").start();
    }

    private static double[] location(Context c) {
        SharedPreferences p = c.getSharedPreferences(LOC_PREFS, Context.MODE_PRIVATE);
        if (p.contains(LAT) && p.contains(LON)) {
            return new double[]{p.getFloat(LAT, 0), p.getFloat(LON, 0)};
        }
        try {
            JSONObject o = new JSONObject(fetch("https://ipapi.co/json/"));
            double lat = o.getDouble("latitude");
            double lon = o.getDouble("longitude");
            p.edit().putFloat(LAT, (float) lat).putFloat(LON, (float) lon).commit();
            return new double[]{lat, lon};
        } catch (Exception e) {
            return new double[]{39.7295, -104.8319};
        }
    }

    private static String fetch(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(6000);
        conn.setReadTimeout(9000);
        conn.setRequestProperty("User-Agent", "EchoShowLauncher/1.0");
        try {
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new Exception("HTTP " + conn.getResponseCode());
            }
            InputStream in = conn.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            in.close();
            return out.toString("UTF-8");
        } finally {
            conn.disconnect();
        }
    }
}
