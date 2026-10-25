package com.ardaozkul.sibercoban;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * WiFi Hotspot ve TCP Sunucusu Yöneticisi
 * WebSocket yerine Raw TCP kullanılarak ESP32 tarafında kütüphane bağımlılığı
 * kaldırıldı.
 */
public class WiFiConnectionManager {

    private static WiFiConnectionManager instance;
    private Context context;
    private ServerSocket serverSocket;
    private Thread serverThread;
    private static final int PORT = 8080;
    private boolean isRunning = false;

    // Bağlı istemciler (ESP32'ler)
    private List<ClientHandler> clients = new ArrayList<>();

    // Bağlantı Dinleyicisi
    private ConnectionListener listener;

    public interface ConnectionListener {
        void onDeviceConnected(String deviceId);

        void onGPSDataReceived(String deviceId, double lat, double lng, String barnId);
    }

    private WiFiConnectionManager(Context context) {
        this.context = context;
        startServer();
    }

    public static synchronized WiFiConnectionManager getInstance(Context context) {
        if (instance == null) {
            instance = new WiFiConnectionManager(context);
        }
        return instance;
    }

    public void setListener(ConnectionListener listener) {
        this.listener = listener;
    }

    public void startServer() {
        if (isRunning)
            return;

        isRunning = true;
        serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                Log.d("WiFiManager", "TCP Server başlatıldı: Port " + PORT);

                while (isRunning) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        Log.d("WiFiManager", "Yeni istemci: " + clientSocket.getInetAddress());

                        ClientHandler client = new ClientHandler(clientSocket);
                        synchronized (clients) {
                            clients.add(client);
                        }
                        new Thread(client).start();

                    } catch (IOException e) {
                        if (isRunning)
                            e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        serverThread.start();
    }

    public void stopServer() {
        isRunning = false;
        try {
            if (serverSocket != null)
                serverSocket.close();
            synchronized (clients) {
                for (ClientHandler client : clients) {
                    client.close();
                }
                clients.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendCommandToAll(String command) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.send(command);
            }
        }
    }

    public int getConnectedDeviceCount() {
        synchronized (clients) {
            return clients.size();
        }
    }

    /**
     * İstemci Bağlantı Yöneticisi (Her ESP32 için bir thread)
     */
    private class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String deviceIp;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            this.deviceIp = socket.getInetAddress().toString();
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String message;
                while ((message = in.readLine()) != null) {
                    Log.d("WiFiManager", "Mesaj alındı: " + message);
                    processMessage(message);
                }
            } catch (IOException e) {
                Log.e("WiFiManager", "İstemci hatası/koptu: " + e.getMessage());
            } finally {
                close();
                synchronized (clients) {
                    clients.remove(this);
                }
            }
        }

        public void send(String msg) {
            if (out != null) {
                new Thread(() -> {
                    try {
                        out.println(msg);
                        out.flush();
                    } catch (Exception e) {
                    }
                }).start();
            }
        }

        public void close() {
            try {
                if (socket != null && !socket.isClosed())
                    socket.close();
            } catch (IOException e) {
            }
        }

        private void processMessage(String message) {
            // Mesaj formatı: GPS:LAT,LNG,ID,BARN or just ID update
            if (listener != null) {
                if (message.contains("GPS:")) {
                    try {
                        String cleanMsg = message.replace("GPS:", "").trim();
                        String[] parts = cleanMsg.split(",");
                        // Format: LAT,LNG,ID,BARN
                        if (parts.length >= 3) {
                            double lat = Double.parseDouble(parts[0]);
                            double lng = Double.parseDouble(parts[1]);
                            String id = parts[2];
                            String barn = parts.length > 3 ? parts[3] : "BARN_1";

                            listener.onGPSDataReceived(id, lat, lng, barn);
                            listener.onDeviceConnected(id);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (message.startsWith("HELLO:")) {
                    // Cihaz ilk bağlantıda ID gönderirse
                    String id = message.split(":")[1];
                    listener.onDeviceConnected(id);
                }
            }
        }
    }
}
