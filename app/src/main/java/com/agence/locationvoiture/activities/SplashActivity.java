package com.agence.locationvoiture.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.agence.locationvoiture.R;
import com.agence.locationvoiture.utils.SessionManager;

/**
 * Écran de démarrage affiché 2 secondes.
 * Redirige vers :
 *  - ClientDashboardActivity si l'utilisateur est client
 *  - MainActivity si admin ou employé
 *  - LoginActivity si pas de session
 */
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION_MS = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            SessionManager session = new SessionManager(this);
            Intent intent;
            if (session.isLoggedIn()) {
                // Router selon le rôle
                if (session.isClient()) {
                    intent = new Intent(this, ClientDashboardActivity.class);
                } else {
                    intent = new Intent(this, MainActivity.class);
                }
            } else {
                intent = new Intent(this, LoginActivity.class);
            }
            startActivity(intent);
            finish();
        }, SPLASH_DURATION_MS);
    }
}
