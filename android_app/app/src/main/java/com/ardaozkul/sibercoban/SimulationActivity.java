package com.ardaozkul.sibercoban;

import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Harita Planlama ve Simülasyon Activity
 * Ana sayfadan ayrı olarak ahır/otlak planlaması için kullanılır
 */
public class SimulationActivity extends AppCompatActivity {

    private WebView webView;
    private Button btnBackToMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simulation);

        // UI bileşenlerini başlat
        webView = findViewById(R.id.webViewSimulation);
        btnBackToMain = findViewById(R.id.btnBackToMain);

        // WebView'i yapılandır
        setupWebView();

        // Geri dönüş butonu
        btnBackToMain.setOnClickListener(v -> {
            finish(); // Activity'yi kapat, MainActivity'ye dön
        });
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);

        // JS Interface ve Chrome Client
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new WebAppInterface(), "Android");

        // HTML dosyasını yükle
        webView.loadUrl("file:///android_asset/sibercobanv2.html");
    }

    // JavaScript Interface - Harita verilerini kaydetme
    public class WebAppInterface {
        @android.webkit.JavascriptInterface
        public void saveMapData(String jsonData, String filename) {
            PreferencesManager.getInstance(SimulationActivity.this).saveMapData(jsonData, filename);
            runOnUiThread(() -> {
                Toast.makeText(SimulationActivity.this, "✓ Kaydedildi: " + filename, Toast.LENGTH_LONG).show();
            });
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webView != null) {
            webView.destroy();
        }
    }
}
