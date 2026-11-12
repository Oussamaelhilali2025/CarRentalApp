package com.agence.locationvoiture.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.agence.locationvoiture.R;
import com.agence.locationvoiture.dao.VoitureDAO;
import com.agence.locationvoiture.models.Voiture;

import java.util.List;
import java.util.Random;

/**
 * Écran de géolocalisation des voitures.
 * Utilise OpenStreetMap (Leaflet.js) via WebView — pas de clé API requise.
 *
 * En production :
 *  - Chaque voiture aurait ses coordonnées GPS réelles stockées en base
 *  - Un module GPS embarqué ou un tracking temps-réel alimenterait ces données
 *  - Ici, les positions sont simulées autour d'un centre (Marrakech par défaut)
 */
public class GeolocationActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_CODE = 101;
    private WebView webView;
    private VoitureDAO voitureDAO;

    // Centre de la carte (Marrakech par défaut)
    private static final double DEFAULT_LAT  = 31.6295;
    private static final double DEFAULT_LNG  = -7.9811;
    private static final double RADIUS_DELTA = 0.05; // ~5 km

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geolocation);

        voitureDAO = new VoitureDAO(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("📍 Localisation des Voitures");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        webView = findViewById(R.id.web_map);
        setupWebView();

        // Demander la permission de localisation si pas accordée
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_CODE);
        } else {
            loadMap();
        }
    }

    @SuppressWarnings("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new WebAppInterface(), "Android");
    }

    private void loadMap() {
        List<Voiture> voitures = voitureDAO.getAllVoitures();
        String markersJs = buildMarkersJs(voitures);

        String html = "<!DOCTYPE html><html><head>" +
                "<meta charset='utf-8'/>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>" +
                "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>" +
                "<style>html,body,#map{width:100%;height:100%;margin:0;padding:0;}" +
                ".popup-content{font-family:sans-serif;font-size:14px;}" +
                ".dispo{color:#2E7D32;font-weight:bold;}" +
                ".indispo{color:#C62828;font-weight:bold;}</style>" +
                "</head><body>" +
                "<div id='map'></div>" +
                "<script>" +
                "var map = L.map('map').setView([" + DEFAULT_LAT + "," + DEFAULT_LNG + "], 13);" +
                "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {" +
                "  attribution: '© OpenStreetMap contributors'," +
                "  maxZoom: 18" +
                "}).addTo(map);" +
                // Icônes personnalisées
                "var iconDispo = L.divIcon({className:'', html:'<div style=\"font-size:28px;\">🚗</div>', iconSize:[30,30]});" +
                "var iconIndispo = L.divIcon({className:'', html:'<div style=\"font-size:28px;\">🔴</div>', iconSize:[30,30]});" +
                markersJs +
                // Localisation utilisateur
                "if (navigator.geolocation) {" +
                "  navigator.geolocation.getCurrentPosition(function(pos){" +
                "    var userIcon = L.divIcon({className:'', html:'<div style=\"font-size:28px;\">📍</div>', iconSize:[30,30]});" +
                "    L.marker([pos.coords.latitude, pos.coords.longitude], {icon: userIcon})" +
                "     .addTo(map).bindPopup('<b>Ma position</b>').openPopup();" +
                "  });" +
                "}" +
                "</script></body></html>";

        webView.loadDataWithBaseURL("https://openstreetmap.org", html, "text/html", "UTF-8", null);
    }

    /** Génère le JS Leaflet pour placer chaque voiture sur la carte */
    private String buildMarkersJs(List<Voiture> voitures) {
        StringBuilder sb = new StringBuilder();
        Random rnd = new Random(42); // seed fixe pour positions reproductibles

        for (Voiture v : voitures) {
            // Position simulée autour du centre
            double lat = DEFAULT_LAT + (rnd.nextDouble() - 0.5) * RADIUS_DELTA * 2;
            double lng = DEFAULT_LNG + (rnd.nextDouble() - 0.5) * RADIUS_DELTA * 2;

            String dispoClass = v.isDisponible() ? "dispo" : "indispo";
            String dispoText  = v.isDisponible() ? "✅ Disponible" : "🔴 Indisponible";

            String popup = "<div class='popup-content'>" +
                    "<b>" + escapeJs(v.getNomComplet()) + "</b><br/>" +
                    "<span class='" + dispoClass + "'>" + dispoText + "</span><br/>" +
                    String.format("%.0f MAD/jour", v.getPrixJour()) +
                    "</div>";

            String icon = v.isDisponible() ? "iconDispo" : "iconIndispo";
            sb.append(String.format(
                    "L.marker([%f,%f], {icon: %s}).addTo(map).bindPopup('%s');%n",
                    lat, lng, icon, popup.replace("'", "\\'")));
        }
        return sb.toString();
    }

    private String escapeJs(String s) {
        return s == null ? "" : s.replace("'", "\\'").replace("\"", "\\\"");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == LOCATION_PERMISSION_CODE) {
            loadMap();
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Localisation activée", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        webView.destroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }

    /** Interface JS→Java pour future communication bidirectionnelle */
    private class WebAppInterface {
        @JavascriptInterface
        public void onVoitureSelectionnee(int voitureId) {
            runOnUiThread(() ->
                    Toast.makeText(GeolocationActivity.this,
                            "Voiture #" + voitureId + " sélectionnée", Toast.LENGTH_SHORT).show());
        }
    }
}
