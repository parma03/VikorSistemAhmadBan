package com.example.vikorsistemahmadban.activity.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.vikorsistemahmadban.LogoutActivity;
import com.example.vikorsistemahmadban.R;
import com.example.vikorsistemahmadban.databinding.ActivityMainAdminBinding;

public class MainAdminActivity extends AppCompatActivity {
    private ActivityMainAdminBinding binding;
    private boolean isDropdownVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainAdminBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupClickListeners();
    }

    private void setupClickListeners() {
        // Profile container click listener untuk toggle dropdown
        binding.profileContainer.setOnClickListener(v -> toggleDropdownMenu());

        // Menu cards click listeners
        binding.cardDataUser.setOnClickListener(v -> {
            Intent intent = new Intent(MainAdminActivity.this, DataUserActivity.class);
            startActivity(intent);
        });

        binding.cardDataBan.setOnClickListener(v -> {
            Intent intent = new Intent(MainAdminActivity.this, DataBanActivity.class);
            startActivity(intent);
        });

        binding.cardDataKriteria.setOnClickListener(v -> {
            // Intent ke DataKriteriaActivity
            // Intent intent = new Intent(MainAdminActivity.this, DataKriteriaActivity.class);
            // startActivity(intent);
        });

        binding.cardDataSubKriteria.setOnClickListener(v -> {
            // Intent ke DataSubKriteriaActivity
            // Intent intent = new Intent(MainAdminActivity.this, DataSubKriteriaActivity.class);
            // startActivity(intent);
        });

        // Dropdown menu items click listeners
        binding.menuProfile.setOnClickListener(v -> {
            hideDropdownMenu();
            // Intent ke ProfileSettingActivity
            // Intent intent = new Intent(MainAdminActivity.this, ProfileSettingActivity.class);
            // startActivity(intent);
            // MEMEK
        });

        binding.menuLogout.setOnClickListener(v -> {
            hideDropdownMenu();
            // Panggil LogoutHelper untuk menampilkan konfirmasi dan logout
            LogoutActivity.showLogoutConfirmation(MainAdminActivity.this);
        });

        // Click listener untuk area lain (untuk menutup dropdown)
        binding.main.setOnClickListener(v -> {
            if (isDropdownVisible) {
                hideDropdownMenu();
            }
        });
    }

    private void toggleDropdownMenu() {
        if (isDropdownVisible) {
            hideDropdownMenu();
        } else {
            showDropdownMenu();
        }
    }

    private void showDropdownMenu() {
        binding.dropdownMenu.setVisibility(View.VISIBLE);

        // Animasi slide down
        Animation slideDown = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left);
        slideDown.setDuration(200);
        binding.dropdownMenu.startAnimation(slideDown);

        // Rotasi icon dropdown
        binding.dropdownIcon.animate()
                .rotation(180f)
                .setDuration(200)
                .start();

        isDropdownVisible = true;
    }

    private void hideDropdownMenu() {
        // Animasi slide up
        Animation slideUp = AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right);
        slideUp.setDuration(200);
        slideUp.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                binding.dropdownMenu.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        binding.dropdownMenu.startAnimation(slideUp);

        // Reset rotasi icon dropdown
        binding.dropdownIcon.animate()
                .rotation(0f)
                .setDuration(200)
                .start();

        isDropdownVisible = false;
    }

    @Override
    public void onBackPressed() {
        if (isDropdownVisible) {
            hideDropdownMenu();
        } else {
            super.onBackPressed();
        }
    }
}