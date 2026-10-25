package com.ardaozkul.sibercoban;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {

    private static final String PREF_NAME = "SiberCobanPrefs";
    private static final String KEY_LAST_FILE = "last_file_name";
    private static final String FILE_PREFIX = "map_data_";
    private static final String FILE_EXT = ".json";

    private static PreferencesManager instance;
    private Context context;
    private SharedPreferences prefs;

    public static synchronized PreferencesManager getInstance(Context context) {
        if (instance == null) {
            instance = new PreferencesManager(context.getApplicationContext());
        }
        return instance;
    }

    private PreferencesManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveMapData(String jsonData, String filename) {
        // Filename JS tarafından zaten oluşturuldu, direkt kullanıyoruz.
        // Ama prefix kontrolü yapalım
        if (!filename.startsWith(FILE_PREFIX) && !filename.startsWith("sibercoban_")) {
            filename = FILE_PREFIX + filename;
        }

        try {
            java.io.FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
            fos.write(jsonData.getBytes());
            fos.close();

            // Son kaydedilen dosyayı hatırla
            prefs.edit().putString(KEY_LAST_FILE, filename).apply();

        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    public String getMapData() {
        // En son kaydedilen dosyayı döndür
        String lastFile = prefs.getString(KEY_LAST_FILE, null);
        if (lastFile != null) {
            return getMapData(lastFile);
        }
        return null; // Dosya yoksa null
    }

    public String getMapData(String filename) {
        try {
            java.io.FileInputStream fis = context.openFileInput(filename);
            java.io.InputStreamReader isr = new java.io.InputStreamReader(fis);
            java.io.BufferedReader bufferedReader = new java.io.BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (java.io.IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public java.util.List<String> getSavedFiles() {
        java.util.List<String> files = new java.util.ArrayList<>();
        java.io.File dir = context.getFilesDir();

        // Search for both map_data_* and sibercoban_* files to support both old format
        // and ESP32 exports
        java.io.File[] fileList = dir.listFiles((d,
                name) -> (name.startsWith(FILE_PREFIX) || name.startsWith("sibercoban_")) && name.endsWith(FILE_EXT));

        if (fileList != null) {
            // Tarihe göre ters sırala (en yeni en üstte)
            java.util.Arrays.sort(fileList, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

            for (java.io.File file : fileList) {
                files.add(file.getName());
            }
        }
        return files;
    }

    public void clearMapData() {
        // Tüm map_data dosyalarını sil
        java.io.File dir = context.getFilesDir();
        java.io.File[] fileList = dir.listFiles((d, name) -> name.startsWith(FILE_PREFIX) && name.endsWith(FILE_EXT));

        if (fileList != null) {
            for (java.io.File file : fileList) {
                file.delete();
            }
        }
        prefs.edit().remove(KEY_LAST_FILE).apply();
    }

    public String getLastSavedFile() {
        return prefs.getString(KEY_LAST_FILE, null);
    }
}
