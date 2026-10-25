package com.ardaozkul.sibercoban;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothManager {

    private static final String TAG = "BluetoothManager";
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static BluetoothManager instance;
    private BluetoothSocket socket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private Thread readThread;
    private boolean isConnected = false;
    private java.util.List<ConnectionCallback> listeners = new java.util.ArrayList<>();

    public interface ConnectionCallback {
        void onConnected();

        void onConnectionFailed(String error);

        void onDataReceived(String data);
    }

    public static synchronized BluetoothManager getInstance() {
        if (instance == null) {
            instance = new BluetoothManager();
        }
        return instance;
    }

    private BluetoothManager() {
    }

    public void addListener(ConnectionCallback listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(ConnectionCallback listener) {
        listeners.remove(listener);
    }

    public void connect(Context context, BluetoothDevice device, ConnectionCallback initCallback) {
        // İlk bağlantı isteği yapanı listeye ekle
        if (initCallback != null) {
            addListener(initCallback);
        }

        new Thread(() -> {
            try {
                // Eski bağlantıyı kapat
                disconnect();

                // Yeni socket oluştur
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID);
                socket.connect();

                outputStream = socket.getOutputStream();
                inputStream = socket.getInputStream();
                isConnected = true;

                // Okuma thread'i başlat
                startReadThread();

                notifyConnected();

            } catch (IOException e) {
                Log.e(TAG, "Bağlantı hatası: " + e.getMessage());
                isConnected = false;

                notifyConnectionFailed(e.getMessage());
            }
        }).start();
    }

    private void notifyConnected() {
        new Handler(Looper.getMainLooper()).post(() -> {
            for (ConnectionCallback listener : listeners) {
                listener.onConnected();
            }
        });
    }

    private void notifyConnectionFailed(String error) {
        new Handler(Looper.getMainLooper()).post(() -> {
            for (ConnectionCallback listener : listeners) {
                listener.onConnectionFailed(error);
            }
        });
    }

    private void startReadThread() {
        readThread = new Thread(() -> {
            byte[] buffer = new byte[1024];
            int bytes;

            while (isConnected) {
                try {
                    bytes = inputStream.read(buffer);
                    if (bytes > 0) {
                        String data = new String(buffer, 0, bytes);
                        Log.d(TAG, "Alınan veri: " + data);

                        new Handler(Looper.getMainLooper()).post(() -> {
                            for (ConnectionCallback listener : listeners) {
                                listener.onDataReceived(data);
                            }
                        });
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Okuma hatası: " + e.getMessage());
                    break;
                }
            }
        });
        readThread.start();
    }

    public void sendData(String data) {
        if (!isConnected || outputStream == null) {
            Log.e(TAG, "Bağlantı yok!");
            return;
        }

        new Thread(() -> {
            try {
                outputStream.write(data.getBytes());
                outputStream.flush();
                Log.d(TAG, "Veri gönderildi: " + data);
            } catch (IOException e) {
                Log.e(TAG, "Gönderme hatası: " + e.getMessage());
                isConnected = false;
            }
        }).start();
    }

    public void disconnect() {
        isConnected = false;

        try {
            if (readThread != null) {
                readThread.interrupt();
                readThread = null;
            }

            if (inputStream != null) {
                inputStream.close();
                inputStream = null;
            }

            if (outputStream != null) {
                outputStream.close();
                outputStream = null;
            }

            if (socket != null) {
                socket.close();
                socket = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Kapatma hatası: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return isConnected;
    }
}
