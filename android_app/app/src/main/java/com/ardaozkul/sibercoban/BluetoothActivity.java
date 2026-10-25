package com.ardaozkul.sibercoban;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Set;

public class BluetoothActivity extends AppCompatActivity {

    private ListView listViewDevices;
    private TextView tvStatus;
    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<BluetoothDevice> deviceList;
    private ArrayAdapter<String> deviceNamesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        listViewDevices = findViewById(R.id.listViewDevices);
        tvStatus = findViewById(R.id.tvStatus);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        deviceList = new ArrayList<>();

        // Eşleşmiş cihazları listele
        listPairedDevices();

        // Cihaz seçimi
        listViewDevices.setOnItemClickListener((parent, view, position, id) -> {
            BluetoothDevice device = deviceList.get(position);
            connectToDevice(device);
        });
    }

    private void listPairedDevices() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Bluetooth izni gerekli!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        ArrayList<String> deviceNames = new ArrayList<>();

        deviceList.clear();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                if (deviceName != null && deviceName.startsWith("SiberCoban_")) {
                    deviceList.add(device);
                    deviceNames.add(deviceName + "\n" + device.getAddress());
                }
            }
        }

        if (deviceList.isEmpty()) {
            deviceNames.add("ESP32 cihazı bulunamadı!");
            tvStatus.setText("⚠ ESP32 ile eşleşme yapın");
        } else {
            tvStatus.setText("📱 Cihaz seçin:");
        }

        deviceNamesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceNames);
        listViewDevices.setAdapter(deviceNamesAdapter);
    }

    private void connectToDevice(BluetoothDevice device) {
        tvStatus.setText("🔄 Bağlanıyor...");

        BluetoothManager.getInstance().connect(this, device, new BluetoothManager.ConnectionCallback() {
            @Override
            public void onConnected() {
                runOnUiThread(() -> {
                    tvStatus.setText("✓ Bağlandı!");
                    Toast.makeText(BluetoothActivity.this, "ESP32'ye bağlandı!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            }

            @Override
            public void onConnectionFailed(String error) {
                runOnUiThread(() -> {
                    tvStatus.setText("✗ Bağlantı hatası: " + error);
                    Toast.makeText(BluetoothActivity.this, "Bağlantı başarısız!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onDataReceived(String data) {
                // Data alma callback
            }
        });
    }
}
