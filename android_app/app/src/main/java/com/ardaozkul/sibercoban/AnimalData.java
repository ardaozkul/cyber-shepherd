package com.ardaozkul.sibercoban;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Hayvan verilerini tutan model sınıfı
 * Her ESP32 cihazı için konum, durum ve ahır bilgilerini saklar
 */
public class AnimalData {
    private String deviceId; // ESP32 cihaz ID (örn: ESP32_001)
    private String barnId; // Hangi ahıra ait (örn: BARN_1)
    private double latitude; // Son bilinen enlem
    private double longitude; // Son bilinen boylam
    private String status; // INSIDE, OUTSIDE, WARNING
    private String type; // cow, sheep, goat
    private long lastUpdate; // Son güncelleme zamanı (timestamp)

    // Durum sabitleri
    public static final String STATUS_INSIDE = "INSIDE";
    public static final String STATUS_OUTSIDE = "OUTSIDE";
    public static final String STATUS_WARNING = "WARNING";
    public static final String STATUS_UNKNOWN = "UNKNOWN";

    public AnimalData(String deviceId, String barnId, double latitude, double longitude) {
        this.deviceId = deviceId;
        this.barnId = barnId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = STATUS_UNKNOWN;
        this.type = "sheep"; // Varsayılan
        this.lastUpdate = System.currentTimeMillis();
    }

    // Getters
    public String getDeviceId() {
        return deviceId;
    }

    public String getBarnId() {
        return barnId;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getStatus() {
        return status;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    // Setters
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public void setBarnId(String barnId) {
        this.barnId = barnId;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
        this.lastUpdate = System.currentTimeMillis();
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
        this.lastUpdate = System.currentTimeMillis();
    }

    public void setStatus(String status) {
        this.status = status;
        this.lastUpdate = System.currentTimeMillis();
    }

    public void updateLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.lastUpdate = System.currentTimeMillis();
    }

    public void updateLocation(double latitude, double longitude, String status) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = status;
        this.lastUpdate = System.currentTimeMillis();
    }

    /**
     * Son güncelleme zamanını okunabilir formatta döndürür
     */
    public String getFormattedLastUpdate() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(lastUpdate));
    }

    /**
     * Son güncelleme ne kadar önce yapıldı (saniye)
     */
    public long getSecondsSinceUpdate() {
        return (System.currentTimeMillis() - lastUpdate) / 1000;
    }

    /**
     * Hayvan verisi eskimiş mi? (60 saniyeden eski)
     */
    public boolean isStale() {
        return getSecondsSinceUpdate() > 60;
    }

    @Override
    public String toString() {
        return "AnimalData{" +
                "deviceId='" + deviceId + '\'' +
                ", barnId='" + barnId + '\'' +
                ", lat=" + latitude +
                ", lng=" + longitude +
                ", status='" + status + '\'' +
                ", lastUpdate=" + getFormattedLastUpdate() +
                '}';
    }
}
