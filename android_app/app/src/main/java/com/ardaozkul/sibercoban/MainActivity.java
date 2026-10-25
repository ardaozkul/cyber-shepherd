package com.ardaozkul.sibercoban;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Set;

/**
 * ╔═══════════════════════════════════════════════════════════════════════╗
 * ║ SİBER ÇOBAN v2 - ANDROID KONTROL UYGULAMASI ║
 * ║ ARDA ÖZKUL - 2026 ║
 * ║ CYBER SHEPHERD v2 - SMART LIVESTOCK TRACKING ║
 * ╚═══════════════════════════════════════════════════════════════════════╝
 * 
 * Ana Activity:
 * - Harita planlama arayüzüne geçiş
 * - Bluetooth cihaz seçimi
 * - Veri gönderme ve komut kontrolü
 */

public class MainActivity extends androidx.appcompat.app.AppCompatActivity
        implements WiFiConnectionManager.ConnectionListener, BluetoothManager.ConnectionCallback {

    // UI bileşenleri
    private Button btnOpenSimulation;
    private Button btnWiFiSettings;
    private Button btnSendData;
    private Button btnStartGrazing;
    private Button btnStartReturn;

    // WiFi Bağlantı Durumu
    private TextView txtConnectionStatus;
    private TextView txtConnectedDevices;
    private TextView txtConnectionWarning;

    // 4. Bölüm: Gerçek Zamanlı Takip
    private androidx.recyclerview.widget.RecyclerView rvAnimalList;
    private android.widget.TextView txtAnimalStats;
    private android.widget.TextView txtNoAnimals;
    private android.widget.TextView txtMapInfo;
    private AnimalListAdapter animalAdapter;
    private java.util.HashMap<String, AnimalData> animalDataMap = new java.util.HashMap<>();

    private WebView webViewMap;

    // Bluetooth (opsiyonel - WiFi ile değiştirilecek)
    private BluetoothAdapter bluetoothAdapter;
    private boolean isBluetoothConnected = false;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI Başlat
        btnOpenSimulation = findViewById(R.id.btnOpenSimulation);
        btnWiFiSettings = findViewById(R.id.btnWiFiSettings);
        btnSendData = findViewById(R.id.btnSendData);
        btnStartGrazing = findViewById(R.id.btnStartGrazing);
        btnStartReturn = findViewById(R.id.btnStartReturn);

        // WiFi bağlantı durumu
        txtConnectionStatus = findViewById(R.id.txtConnectionStatus);
        txtConnectedDevices = findViewById(R.id.txtConnectedDevices);
        txtConnectionWarning = findViewById(R.id.txtConnectionWarning);

        // 4. Bölüm: Gerçek Zamanlı Takip UI
        rvAnimalList = findViewById(R.id.rvAnimalList);
        txtAnimalStats = findViewById(R.id.txtAnimalStats);
        txtNoAnimals = findViewById(R.id.txtNoAnimals);
        txtMapInfo = findViewById(R.id.txtMapInfo);
        setupAnimalTracking();

        // WebView Map Setup (Native Map yerine)
        webViewMap = findViewById(R.id.webViewMap);
        setupWebViewMap();

        // Bluetooth adapter (opsiyonel - WiFi'ye geçilecek)
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bu cihaz Bluetooth desteklemiyor!", Toast.LENGTH_LONG).show();
        }

        // İzinleri kontrol et
        checkPermissions();

        // Buton event'leri
        setupButtonListeners();

        // WiFi durumunu güncelle
        updateConnectionStatus();

        // WiFi Listener başlat
        startWiFiListener();
    }

    private void startWiFiListener() {
        WiFiConnectionManager.getInstance(this).setListener(this);

        // Server başlat
        WiFiConnectionManager.getInstance(this).startServer();
    }

    private void setupButtonListeners() {
        // 1. Harita Planlamaya Git
        btnOpenSimulation.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SimulationActivity.class);
            startActivity(intent);
        });

        // 2. WiFi Hotspot Ayarları
        btnWiFiSettings.setOnClickListener(v -> {
            try {
                startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
                Toast.makeText(this, "İpucu: Hotspot'u açın ve Siber Çoban cihazlarının bağlanmasını bekleyin",
                        Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "Ayarlar açılamadı", Toast.LENGTH_SHORT).show();
            }
        });

        // 3. Veri gönder
        btnSendData.setOnClickListener(v -> {
            if (!isAnyConnectionActive()) {
                Toast.makeText(this, "Önce Bluetooth veya WiFi ile bağlanın!",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            sendDataToESP32();
        });

        // 3. Yayılma başlat
        btnStartGrazing.setOnClickListener(v -> {
            if (!isAnyConnectionActive()) {
                Toast.makeText(this, "Önce bağlantı kurun!", Toast.LENGTH_SHORT).show();
                return;
            }
            sendCommand("START");
        });

        // 3. Geri dönüş başlat
        btnStartReturn.setOnClickListener(v -> {
            if (!isAnyConnectionActive()) {
                Toast.makeText(this, "Önce bağlantı kurun!", Toast.LENGTH_SHORT).show();
                return;
            }
            sendCommand("RETURN");
        });
    }

    private boolean isAnyConnectionActive() {
        boolean bt = BluetoothManager.getInstance().isConnected();
        boolean wifi = WiFiConnectionManager.getInstance(this).getConnectedDeviceCount() > 0;
        return bt || wifi;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void checkPermissions() {
        ArrayList<String> permissionsNeeded = new ArrayList<>();

        // Bluetooth izinleri
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH);
            }
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_ADMIN);
            }
        }

        // Konum izni
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]),
                    REQUEST_BLUETOOTH_PERMISSIONS);
        }
    }

    private void showBluetoothDevices() {
        Intent intent = new Intent(MainActivity.this, BluetoothActivity.class);
        startActivityForResult(intent, 100);
    }

    private void sendDataToESP32() {
        // Kaydedilmiş dosyaları listele
        java.util.List<String> files = PreferencesManager.getInstance(this).getSavedFiles();

        if (files.isEmpty()) {
            Toast.makeText(this, "Kayıtlı harita verisi bulunamadı! Lütfen önce haritadan planlama yapın.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Bağlı cihaz sayısını kontrol et
        java.util.List<String> connectedDevices = new java.util.ArrayList<>();

        int wifiDeviceCount = WiFiConnectionManager.getInstance(this).getConnectedDeviceCount();
        for (int i = 0; i < wifiDeviceCount; i++) {
            connectedDevices.add("WiFi Cihaz #" + (i + 1));
        }

        if (BluetoothManager.getInstance().isConnected()) {
            connectedDevices.add("Bluetooth Cihaz");
        }

        if (connectedDevices.isEmpty()) {
            Toast.makeText(this, "Hiç bağlı cihaz yok! Lütfen önce cihazları bağlayın.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Cihaz seçim dialogu
        String[] deviceArray = connectedDevices.toArray(new String[0]);
        boolean[] selectedDevices = new boolean[deviceArray.length];
        java.util.Arrays.fill(selectedDevices, false);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Cihaz Seçimi")
                .setMultiChoiceItems(deviceArray, selectedDevices, (dialog, which, isChecked) -> {
                    selectedDevices[which] = isChecked;
                })
                .setPositiveButton("Tümünü Seç", (dialog, which) -> {
                    java.util.Arrays.fill(selectedDevices, true);
                    showFileSelectionForDevices(files, selectedDevices, connectedDevices);
                })
                .setNegativeButton("Hiçbirini Seçme", (dialog, which) -> {
                    Toast.makeText(this, "İptal edildi", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("Devam", (dialog, which) -> {
                    boolean anySelected = false;
                    for (boolean selected : selectedDevices) {
                        if (selected) {
                            anySelected = true;
                            break;
                        }
                    }

                    if (!anySelected) {
                        Toast.makeText(this, "En az bir cihaz seçmelisiniz!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    showFileSelectionForDevices(files, selectedDevices, connectedDevices);
                })
                .show();

    }

    private void showFileSelectionForDevices(java.util.List<String> files, boolean[] selectedDevices,
            java.util.List<String> connectedDevices) {
        // Dosya listesini görüntüle (AlertDialog)
        String[] fileArray = files.toArray(new String[0]);
        String[] displayArray = new String[files.size()];

        for (int i = 0; i < files.size(); i++) {
            String f = files.get(i);
            try {
                String datePart = f.substring(9, 17);
                String timePart = f.substring(18, 24);
                String formattedDate = datePart.substring(6, 8) + "." + datePart.substring(4, 6) + "."
                        + datePart.substring(0, 4) +
                        " " + timePart.substring(0, 2) + ":" + timePart.substring(2, 4);
                String notePart = "";
                if (f.length() > 29) {
                    notePart = " - " + f.substring(25, f.length() - 5).replace("_", " ");
                }
                displayArray[i] = formattedDate + notePart;
            } catch (Exception e) {
                displayArray[i] = f;
            }
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Kayıtlı Harita Seçin")
                .setItems(displayArray, (dialog, which) -> {
                    String selectedFilename = fileArray[which];
                    String jsonData = PreferencesManager.getInstance(this).getMapData(selectedFilename);

                    if (jsonData != null) {
                        String dataCmd = "DATA:" + jsonData + "\n";
                        int sentCount = 0;

                        for (int i = 0; i < selectedDevices.length; i++) {
                            if (selectedDevices[i]) {
                                String deviceName = connectedDevices.get(i);

                                if (deviceName.startsWith("Bluetooth")) {
                                    if (BluetoothManager.getInstance().isConnected()) {
                                        BluetoothManager.getInstance().sendData(dataCmd);
                                        sentCount++;
                                    }
                                } else if (deviceName.startsWith("WiFi")) {
                                    WiFiConnectionManager.getInstance(this).sendCommandToAll(dataCmd);
                                    sentCount++;
                                }
                            }
                        }

                        if (sentCount > 0) {
                            Toast.makeText(this, sentCount + " cihaza veri gönderildi: " + displayArray[which],
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Veri gönderilemedi!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Dosya okunamadı!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    private void sendCommand(String command) {
        String cmdStr = "CMD:" + command + "\n";
        boolean sent = false;

        if (BluetoothManager.getInstance().isConnected()) {
            BluetoothManager.getInstance().sendData(cmdStr);
            sent = true;
        }

        if (WiFiConnectionManager.getInstance(this).getConnectedDeviceCount() > 0) {
            WiFiConnectionManager.getInstance(this).sendCommandToAll(cmdStr);
            sent = true;
        }

        if (sent)
            Toast.makeText(this, "Komut gönderildi: " + command, Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(this, "Hata: Bağlı cihaz yok!", Toast.LENGTH_SHORT).show();
    }

    private void updateUI() {
        isBluetoothConnected = BluetoothManager.getInstance().isConnected();
        boolean isWifiConnected = WiFiConnectionManager.getInstance(this).getConnectedDeviceCount() > 0;

        boolean anyConnection = isBluetoothConnected || isWifiConnected;

        btnSendData.setEnabled(anyConnection);
        btnStartGrazing.setEnabled(anyConnection);
        btnStartReturn.setEnabled(anyConnection);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                showBluetoothDevices();
            } else {
                Toast.makeText(this, "Bluetooth açılmadı!", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 100) {
            // Bluetooth bağlantı sonucu
            updateUI();
        }
    }

    // --- WiFi Connection Management ---

    /**
     * WiFi/Hotspot bağlantı durumunu güncelle
     */
    private void updateConnectionStatus() {
        // Şimdilik Bluetooth'un bağlı olup olmadığını kontrol et
        // Gelecekte WiFi WebSocket ile değiştirilecek
        int connectedDeviceCount = 0;

        // Bluetooth bağlı mı kontrol et
        boolean isConnected = BluetoothManager.getInstance().isConnected();

        if (isConnected) {
            connectedDeviceCount = 1; // Şimdilik tek cihaz
        }

        // WiFi cihazlarını ekle
        int wifiCount = WiFiConnectionManager.getInstance(this).getConnectedDeviceCount();
        if (wifiCount > 0) {
            connectedDeviceCount += wifiCount;
        }

        // Bağlı ESP32 sayısına göre hayvan verilerini say
        // connectedDeviceCount = animalDataMap.size(); // BU SATIRI KALDIRIYORUZ,
        // bağlantı sayısını ezmesin

        if (connectedDeviceCount == 0) {
            // Hiç bağlı cihaz yok
            txtConnectionStatus.setText("Durum: Henüz Siber Çoban cihazı bağlanmadı");
            txtConnectedDevices.setVisibility(android.view.View.GONE);
            txtConnectionWarning.setVisibility(android.view.View.VISIBLE);

            // Map ve RecyclerView gizle
            if (webViewMap != null) {
                webViewMap.setVisibility(android.view.View.GONE);
            }
            txtNoAnimals.setVisibility(android.view.View.VISIBLE);
            rvAnimalList.setVisibility(android.view.View.GONE);
        } else {
            // Bağlı cihazlar var
            txtConnectionStatus.setText("Durum: ✓ Aktif");
            txtConnectedDevices.setText("✓ " + connectedDeviceCount + " Siber Çoban Bağlı");
            txtConnectedDevices.setVisibility(android.view.View.VISIBLE);
            txtConnectionWarning.setVisibility(android.view.View.GONE);

            // Map ve RecyclerView göster
            if (webViewMap != null) {
                webViewMap.setVisibility(android.view.View.VISIBLE);
            }
        }
    }

    // --- Animal Tracking Logic (YENİ) ---

    private void setupAnimalTracking() {
        // RecyclerView setup
        animalAdapter = new AnimalListAdapter();
        rvAnimalList.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        rvAnimalList.setAdapter(animalAdapter);

        // Hayvan tıklandığında haritada odaklan
        animalAdapter.setOnAnimalClickListener(animal -> {
            focusOnAnimal(animal.getDeviceId(), animal.getLatitude(), animal.getLongitude());
            Toast.makeText(this,
                    "Fokus: " + animal.getDeviceId() + " (" + animal.getStatus() + ")",
                    Toast.LENGTH_SHORT).show();
        });

        updateAnimalStatsUI();
    }

    /**
     * Haritada belirli bir hayvana odaklan (Native Google Maps)
     */
    private void focusOnAnimal(String deviceId, double lat, double lng) {
        if (webViewMap != null) {
            webViewMap.evaluateJavascript("focusOnAnimal('" + deviceId + "', " + lat + ", " + lng + ")", null);
        }
    }

    /**
     * Hayvan istatistiklerini güncelle (Toplam, Sınır içi, Uyarı)
     */
    private void updateAnimalStatsUI() {
        int total = animalDataMap.size();
        int inside = 0;
        int outside = 0;

        for (AnimalData animal : animalDataMap.values()) {
            if (AnimalData.STATUS_INSIDE.equals(animal.getStatus())) {
                inside++;
            } else if (AnimalData.STATUS_OUTSIDE.equals(animal.getStatus()) ||
                    AnimalData.STATUS_WARNING.equals(animal.getStatus())) {
                outside++;
            }
        }

        String stats = String.format("Toplam: %d | Sınır İçi: %d | Uyarı: %d",
                total, inside, outside);
        txtAnimalStats.setText(stats);

        // Eğer hiç hayvan yoksa "henüz veri yok" mesajını göster
        if (total == 0) {
            txtNoAnimals.setVisibility(android.view.View.VISIBLE);
            rvAnimalList.setVisibility(android.view.View.GONE);
        } else {
            txtNoAnimals.setVisibility(android.view.View.GONE);
            rvAnimalList.setVisibility(android.view.View.VISIBLE);
        }
    }

    private void setupWebViewMap() {
        WebSettings webSettings = webViewMap.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);

        // JavaScript Interface ekle
        webViewMap.addJavascriptInterface(new WebAppInterface(this), "Android");

        webViewMap.setWebChromeClient(new WebChromeClient());
        webViewMap.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // Harita yüklendiğinde kayıtlı veriyi yükle
                String lastFile = PreferencesManager.getInstance(MainActivity.this).getLastSavedFile();
                if (lastFile != null) {
                    String jsonData = PreferencesManager.getInstance(MainActivity.this).getMapData(lastFile);
                    if (jsonData != null) {
                        try {
                            // JSON içindeki tek tırnakları kaçış karakteriyle düzelt
                            String safeJson = jsonData.replace("'", "\\'");
                            // JS fonksiyonunu çağır
                            view.evaluateJavascript("loadScenario('" + safeJson + "')", null);

                            if (txtMapInfo != null)
                                txtMapInfo.setText("Harita verisi yüklendi: " + lastFile);
                        } catch (Exception e) {
                            if (txtMapInfo != null)
                                txtMapInfo.setText("Veri yükleme hatası!");
                        }
                    }
                }
            }
        });

        // Lightweight live map için özel HTML dosyasını yükle (simülasyon UI'sız)
        webViewMap.loadUrl("file:///android_asset/live_map.html");
    }

    /**
     * Sınır kontrolü (Native Ray-Casting Algorithm)
     */
    private void checkBoundaryAndUpdate(String deviceId, double lat, double lng, String barnId) {
        String status = "UNKNOWN";

        // Sınır kontrolü WebView'deki JavaScript tarafından yapılacak
        // Burada sadece WebView'e veriyi gönderiyoruz
        webViewMap.evaluateJavascript("checkBoundary(" + lat + ", " + lng + ", null)", value -> {
            // JavaScript'ten dönen değeri al ("true" veya "false")
            String jsResult = value.replace("\"", "");
            boolean isInside = Boolean.parseBoolean(jsResult);
            String calculatedStatus = isInside ? "INSIDE" : "OUTSIDE";

            AnimalData animal = animalDataMap.get(deviceId);
            if (animal != null) {
                animal.setStatus(calculatedStatus);

                // Eğer sınır dışındaysa uyarı göster
                if ("OUTSIDE".equals(calculatedStatus)) {
                    showBoundaryWarning(deviceId);
                    sendBuzzerCommand(deviceId);
                }

                // UI'ı güncelle
                animalAdapter.addOrUpdateAnimal(animal);
                updateAnimalStatsUI();

                // Haritada marker'ı güncelle (Rengi JS tarafında hallediliyor ama yine de
                // çağıralım)
                updateMapMarker(deviceId, lat, lng, animal.getStatus(), animal.getType());
            }
        });
    }

    /**
     * Sınır uyarısı göster
     */
    private void showBoundaryWarning(String deviceId) {
        Toast.makeText(this,
                "⚠️ UYARI: " + deviceId + " sınır dışında!",
                Toast.LENGTH_LONG).show();
    }

    /**
     * Haritadaki marker'ı güncelle (Native)
     */
    private void updateMapMarker(String deviceId, double lat, double lng, String status, String type) {
        if (webViewMap != null) {
            webViewMap.evaluateJavascript(
                    "updateAnimalLocation('" + deviceId + "', " + lat + ", " + lng + ", '" + status + "', '" + type
                            + "')",
                    null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
        BluetoothManager.getInstance().addListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        BluetoothManager.getInstance().removeListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BluetoothManager.getInstance().removeListener(this);
    }

    @Override
    public void onConnected() {
        isBluetoothConnected = true;
        runOnUiThread(() -> {
            Toast.makeText(this, "Bluetooth Bağlandı!", Toast.LENGTH_SHORT).show();
            updateUI(); // Buton durumlarını ve metinlerini güncelle
        });
    }

    @Override
    public void onConnectionFailed(String error) {
        isBluetoothConnected = false;
        runOnUiThread(() -> {
            Toast.makeText(this, "Bağlantı Hatası: " + error, Toast.LENGTH_LONG).show();
            updateUI();
        });
    }

    @Override
    public void onDataReceived(String data) {
        runOnUiThread(() -> {
            // Parse GPS data: GPS:36.123,34.123,DEVICE_ID,BARN_ID,STATUS
            try {
                String cleanData = data.trim();

                // Komut yanıtlarını filtrele
                if (cleanData.startsWith("OK") || cleanData.startsWith("ERROR"))
                    return;

                if (cleanData.startsWith("GPS:"))
                    cleanData = cleanData.substring(4);

                String[] parts = cleanData.split(",");
                if (parts.length >= 2) {
                    double lat = Double.parseDouble(parts[0]);
                    double lng = Double.parseDouble(parts[1]);
                    String deviceId = parts.length > 2 ? parts[2] : "ESP32_BT";
                    String barnId = parts.length > 3 ? parts[3] : "BARN_UNKNOWN";
                    String espStatus = parts.length > 4 ? parts[4] : "OK";
                    String animalType = parts.length > 5 ? parts[5] : "sheep"; // Default sheep

                    updateGPSData(deviceId, lat, lng, barnId, espStatus, animalType);
                }
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "BT GPS parse error: " + e.getMessage());
            }
        });
    }

    // --- WiFi ConnectionListener Implementation ---

    @Override
    public void onDeviceConnected(String deviceId) {
        runOnUiThread(() -> {
            Toast.makeText(MainActivity.this, "WiFi Cihazı Bağlandı: " + deviceId, Toast.LENGTH_SHORT).show();
            updateConnectionStatus();
            updateUI();
        });
    }

    @Override
    public void onGPSDataReceived(String deviceId, double lat, double lng, String barnId) {
        runOnUiThread(() -> {
            // WiFi üzerinden gelen veri formatı: lat, lng, barnId, status, type (Eğer
            // protokol destekliyorsa)
            // Şimdilik varsayılan type ile
            updateGPSData(deviceId, lat, lng, barnId, "OK", "sheep");
        });
    }

    /**
     * Merkezi GPS Veri İşleme (Bluetooth ve WiFi ortak)
     */
    private void updateGPSData(String deviceId, double lat, double lng, String barnId, String espStatus, String type) {
        // Hayvan verisini güncelle veya ekle
        AnimalData animal = animalDataMap.get(deviceId);
        if (animal == null) {
            animal = new AnimalData(deviceId, barnId, lat, lng);
            animal.setType(type);
            animalDataMap.put(deviceId, animal);
        } else {
            animal.updateLocation(lat, lng);
            animal.setBarnId(barnId);
            animal.setType(type);
        }

        // Sınır kontrolü (Native)
        checkBoundaryAndUpdate(deviceId, lat, lng, barnId);

        // UI'ı güncelle
        animalAdapter.addOrUpdateAnimal(animal);
        updateAnimalStatsUI();

        // Haritada marker'ı güncelle
        updateMapMarker(deviceId, lat, lng, animal.getStatus(), type);

        // Bağlantı durumunu güncelle (yeni cihaz geldiyse)
        updateConnectionStatus();
    }

    /**
     * ESP32'ye buzzer komutu gönder
     */
    private void sendBuzzerCommand(String deviceId) {
        if (BluetoothManager.getInstance().isConnected()) {
            BluetoothManager.getInstance().sendData("BUZZER:" + deviceId + ":ON\n");
            android.util.Log.d("MainActivity", "Buzzer command sent to: " + deviceId);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (!allGranted) {
                Toast.makeText(this, "İzinler verilmedi! Uygulama düzgün çalışmayabilir.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    public class WebAppInterface {
        android.content.Context mContext;

        WebAppInterface(android.content.Context c) {
            mContext = c;
        }

        @android.webkit.JavascriptInterface
        public void showToast(String toast) {
            android.widget.Toast.makeText(mContext, toast, android.widget.Toast.LENGTH_SHORT).show();
        }

        @android.webkit.JavascriptInterface
        public void saveMapData(String json, String fileName) {
            if (fileName == null || fileName.isEmpty()) {
                fileName = "map_data_" + System.currentTimeMillis() + ".json";
            }
            if (!fileName.endsWith(".json")) {
                fileName += ".json";
            }

            PreferencesManager.getInstance(mContext).saveMapData(json, fileName);

            final String finalFileName = fileName;
            runOnUiThread(() -> {
                android.widget.Toast
                        .makeText(mContext, "Harita kaydedildi: " + finalFileName, android.widget.Toast.LENGTH_LONG)
                        .show();
            });
        }
    }
}
