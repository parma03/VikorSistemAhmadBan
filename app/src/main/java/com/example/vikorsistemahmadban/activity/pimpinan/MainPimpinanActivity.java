package com.example.vikorsistemahmadban.activity.pimpinan;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.vikorsistemahmadban.LogoutActivity;
import com.example.vikorsistemahmadban.R;
import com.example.vikorsistemahmadban.activity.admin.MainAdminActivity;

public class MainPimpinanActivity extends AppCompatActivity {
    private Button btnLogout;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_pimpinan);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize button
        btnLogout = findViewById(R.id.btnLogout);

        // Set click listener untuk tombol logout
        btnLogout.setOnClickListener(v -> {
            // Panggil LogoutHelper untuk menampilkan konfirmasi dan logout
            LogoutActivity.showLogoutConfirmation(MainPimpinanActivity.this);
        });

    }
}